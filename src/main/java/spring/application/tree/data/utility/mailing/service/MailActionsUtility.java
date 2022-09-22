package spring.application.tree.data.utility.mailing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.scheduling.service.ScheduleService;
import spring.application.tree.data.users.service.UserService;
import spring.application.tree.data.utility.converting.DateConvertingUtility;
import spring.application.tree.data.utility.mailing.models.AbstractMailMessageModel;
import spring.application.tree.data.utility.mailing.models.ActionType;
import spring.application.tree.data.utility.mailing.models.MailType;
import spring.application.tree.data.utility.properties.CustomPropertyDataLoader;
import spring.application.tree.data.utility.properties.CustomPropertySourceConverter;
import spring.application.tree.data.utility.tasks.ActionHistoryStorage;
import spring.application.tree.data.utility.tasks.TaskFactory;

import javax.annotation.PostConstruct;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailActionsUtility {
    private final ScheduleService scheduleService;
    private final MailService mailService;
    private final TaskFactory taskFactory;
    /**
     * Key - property name, value - property value
     */
    private static Map<String, String> properties;

    @PostConstruct
    private void initializeProperties() {
        properties = CustomPropertySourceConverter.convertToKeyValueFormat(CustomPropertyDataLoader.getResourceContent("classpath:mail.properties"));
    }

    public void sendMessage(AbstractMailMessageModel abstractMailMessageModel) throws ApplicationException {
        if (ActionType.confirmationActions.contains(abstractMailMessageModel.getActionType())) {
            sendConfirmationEmailMessage(abstractMailMessageModel);
        } else if (ActionType.simpleActions.contains(abstractMailMessageModel.getActionType())) {
            mailService.sendMessage(abstractMailMessageModel).run();
        }
    }

    private void sendConfirmationEmailMessage(AbstractMailMessageModel abstractMailMessageModel) throws ApplicationException {
        AbstractMailMessageModel processedMailMessage = processConfirmationMailMessageModelForSending(abstractMailMessageModel);
        Runnable mailTask = mailService.sendMessage(processedMailMessage);
        Runnable confirmationTask = () -> {
            mailTask.run();
            taskFactory.callRollbackTaskForSignUpConfirmation(abstractMailMessageModel.getRecipient(), abstractMailMessageModel.getActionType(), scheduleService, properties);
        };
        ScheduledFuture<?> task = scheduleService.scheduleOnceFireTask(confirmationTask, 0, TimeUnit.SECONDS);
        ActionHistoryStorage.putConfirmationTask(abstractMailMessageModel.getRecipient(), task, Integer.parseInt(properties.get("duration")), ChronoUnit.SECONDS);
    }

    private String generateUniqueCode() {
        return UUID.randomUUID().toString();
    }

    private AbstractMailMessageModel processConfirmationMailMessageModelForSending(AbstractMailMessageModel abstractMailMessageModel) throws InvalidAttributesException {
        String subject;
        String text;
        String uniqueCode = generateUniqueCode();
        String serverURI = String.format("%s://%s:%s", properties.get("protocol"), properties.get("host"), properties.get("port"));
        String recipient = abstractMailMessageModel.getRecipient();
        String action = abstractMailMessageModel.getActionType().getDescription();
        Date expireDate = new DateTime().plusSeconds(Integer.parseInt(properties.get("duration"))).toDate();
        subject = "Confirmation message";
        if (abstractMailMessageModel.getMailType() == MailType.HTML) {
            text = String.format("<p>You are trying %s</p>", abstractMailMessageModel.getActionType().getProcessDescription());
            text += "<a style=\"font-weight: bold; font-color: black; text-decoration: none;\" href=\"{link}\">Click here for verifying action</a>";
            text += "<p>Link expires at {expire}</p>";
            text = text.replace("{link}", String.format("%s/api/utility/task/confirm/%s/%s/%s", serverURI, uniqueCode, recipient, action));
            text = text.replace("{expire}", DateConvertingUtility.convertDate(TimeZone.getTimeZone(abstractMailMessageModel.getClientTimezone()), expireDate).toString());
        } else {
            text = String.format("You are trying %s\n", abstractMailMessageModel.getActionType().getProcessDescription());
            text += String.format("%s/api/utility/task/confirm/%s/%s/%s\n", serverURI, uniqueCode, recipient, action);
            text += String.format("Link expires at %s", DateConvertingUtility.convertDate(TimeZone.getTimeZone(abstractMailMessageModel.getClientTimezone()), expireDate));
        }
        ActionHistoryStorage.putConfirmationCode(abstractMailMessageModel.getRecipient(), uniqueCode, abstractMailMessageModel.getActionType());
        AbstractMailMessageModel processedAbstractMailMessageModel = new AbstractMailMessageModel();
        processedAbstractMailMessageModel.setRecipient(abstractMailMessageModel.getRecipient());
        processedAbstractMailMessageModel.setSubject(subject);
        processedAbstractMailMessageModel.setText(text);
        processedAbstractMailMessageModel.setMailType(abstractMailMessageModel.getMailType());
        processedAbstractMailMessageModel.setActionType(abstractMailMessageModel.getActionType());
        return processedAbstractMailMessageModel;
    }
}
