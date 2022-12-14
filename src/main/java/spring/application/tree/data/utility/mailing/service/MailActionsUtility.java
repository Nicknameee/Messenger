package spring.application.tree.data.utility.mailing.service;

import com.fasterxml.uuid.Generators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.scheduling.service.ScheduleService;
import spring.application.tree.data.utility.converting.DateConvertingUtility;
import spring.application.tree.data.utility.mailing.models.AbstractMailMessageModel;
import spring.application.tree.data.utility.mailing.models.ActionType;
import spring.application.tree.data.utility.mailing.models.MailType;
import spring.application.tree.data.utility.properties.CustomPropertyDataLoader;
import spring.application.tree.data.utility.properties.CustomPropertySourceConverter;
import spring.application.tree.data.utility.tasks.ActionHistoryStorage;
import spring.application.tree.data.utility.tasks.TaskFactory;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    private final DateConvertingUtility dateConvertingUtility = new DateConvertingUtility();

    @PostConstruct
    private void initializeProperties() {
        properties = CustomPropertySourceConverter.convertToKeyValueFormat(CustomPropertyDataLoader.getResourceContent("classpath:mail.properties"));
    }

    public void sendMessage(AbstractMailMessageModel abstractMailMessageModel, HttpServletRequest httpServletRequest) throws ApplicationException, ParseException {
        if (ActionType.isConfirmationAction(abstractMailMessageModel.getActionType())) {
            sendConfirmationEmailMessage(abstractMailMessageModel, httpServletRequest);
        } else if (ActionType.isSimpleAction(abstractMailMessageModel.getActionType())) {
            mailService.sendMessage(abstractMailMessageModel).run();
        }
    }

    private void sendConfirmationEmailMessage(AbstractMailMessageModel abstractMailMessageModel, HttpServletRequest httpServletRequest) throws ApplicationException, ParseException {
        AbstractMailMessageModel processedMailMessage = processConfirmationMailMessageModelForSending(abstractMailMessageModel, httpServletRequest);
        Runnable mailTask = mailService.sendMessage(processedMailMessage);
        Runnable confirmationTask = () -> {
            mailTask.run();
            taskFactory.callRollbackTask(abstractMailMessageModel.getRecipient(), abstractMailMessageModel.getActionType(), scheduleService, properties);
        };
        ScheduledFuture<?> task = scheduleService.scheduleOnceFireTask(confirmationTask, 0, TimeUnit.SECONDS);
        ActionHistoryStorage.putConfirmationTask(abstractMailMessageModel.getRecipient(), task, Integer.parseInt(properties.get("duration")), ChronoUnit.SECONDS);
        log.debug("Sending confirmation email message '{}'", abstractMailMessageModel);
    }

    private String generateUniqueCode() {
        return Generators.timeBasedGenerator().generate().toString();
    }

    private AbstractMailMessageModel processConfirmationMailMessageModelForSending(AbstractMailMessageModel abstractMailMessageModel, HttpServletRequest httpServletRequest) throws InvalidAttributesException, ParseException {
        String subject = "Confirmation message";
        String text;
        String uniqueCode = generateUniqueCode();
        String recipient = abstractMailMessageModel.getRecipient();
        String action = abstractMailMessageModel.getActionType().getDescription();
        Date expireDate = new DateTime().plusSeconds(Integer.parseInt(properties.get("duration"))).toDate();
        ZonedDateTime convertedDate = dateConvertingUtility.convertDate(TimeZone.getTimeZone(abstractMailMessageModel.getClientTimezone()), expireDate);
        String scheme = httpServletRequest.getScheme();
        String server = httpServletRequest.getServerName();
        String port = String.valueOf(httpServletRequest.getServerPort());
        String serverURI = String.format("%s://%s:%s", scheme, server, port);
        if (abstractMailMessageModel.getMailType() == MailType.HTML) {
            text = String.format("<p style=\"font-size: 1.33em;\">You are trying <span style=\"font-weight: bold; font-size: 1.33em;\">%s</span></p>",
                    abstractMailMessageModel.getActionType().getProcessDescription().toUpperCase(Locale.ROOT));
            text += "<a style=\"font-weight: bold; font-color: black; text-decoration: underline; font-size: 1.33em;\" href=\"{link}\">Click here for verifying action</a>";
            text += "<p style=\"font-size: 1.33em;\">Link expires at <span style=\"font-weight:bold; font-size: 1.33em;\">{expire}</span></p>";
            text = text.replace("{link}", String.format("%s/api/utility/task/confirm/%s/%s/%s", serverURI, uniqueCode, recipient, action));
            text = text.replace("{expire}", dateConvertingUtility.format(convertedDate, "dd.MM.yyyy HH:mm:ssXXX"));
        } else {
            text = String.format("You are trying %s\n", abstractMailMessageModel.getActionType().getProcessDescription());
            text += String.format("%s/api/utility/task/confirm/%s/%s/%s\n", serverURI, uniqueCode, recipient, action);
            text += String.format("Link expires at %s", dateConvertingUtility.format(convertedDate, "dd.MM.yyyy HH:mm:ssXXX"));
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
