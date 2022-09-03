package spring.application.tree.data.utility.mailing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.scheduling.service.ScheduleService;
import spring.application.tree.data.utility.mailing.models.AbstractMailMessageModel;
import spring.application.tree.data.utility.mailing.models.MailType;
import spring.application.tree.data.utility.properties.CustomPropertyDataLoader;
import spring.application.tree.data.utility.properties.CustomPropertySourceConverter;
import spring.application.tree.data.utility.tasks.ActionHistoryStorage;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {
    @Value("${spring.mail.username}")
    private String sender;
    private Map<String, String> properties;
    private final JavaMailSender javaMailSender;
    private final ScheduleService scheduleService;

    @PostConstruct
    private void initializeProperties() {
        properties = CustomPropertySourceConverter.convertToKeyValueFormat(CustomPropertyDataLoader.getResourceContent("classpath:mail.properties"));
    }

    public synchronized void sendMessage(AbstractMailMessageModel abstractMailMessageModel) throws ApplicationException {
        AbstractMailMessageModel processedMailMessage = processMailMessageModelForSending(abstractMailMessageModel);
        if (processedMailMessage.getMailType() == MailType.HTML) {
            sendHtmlMailMessage(processedMailMessage);
        }
    }

    @Deprecated
    private void sendMailMessage(AbstractMailMessageModel abstractMailMessageModel) throws InvalidAttributesException {
        Runnable task = () -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(abstractMailMessageModel.getRecipient());
            message.setSubject(abstractMailMessageModel.getSubject());
            message.setText(abstractMailMessageModel.getText());
            message.setSentDate(new Date());
            javaMailSender.send(message);
            log.info(String.format("Sending email to: %s", abstractMailMessageModel.getRecipient()));
        };
        ScheduledFuture<?> scheduledTask = scheduleService.scheduleOnceFireTask(task, 0, TimeUnit.SECONDS);
        ActionHistoryStorage.putConfirmationTask(abstractMailMessageModel.getRecipient(), scheduledTask);
    }

    private void sendHtmlMailMessage(AbstractMailMessageModel abstractMailMessageModel) throws InvalidAttributesException {
        AtomicBoolean taskSuccess = new AtomicBoolean(true);
        Runnable task = () -> {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            try {
                helper.setFrom(sender);
                helper.setTo(abstractMailMessageModel.getRecipient());
                helper.setSubject(abstractMailMessageModel.getSubject());
                helper.setText(abstractMailMessageModel.getText(), true);
                javaMailSender.send(mimeMessage);
                log.info(String.format("Sending email to: %s", abstractMailMessageModel.getRecipient()));
            } catch (MessagingException e) {
                log.error(e.getMessage(), e);
                taskSuccess.set(false);
            }
        };
        if (taskSuccess.get()) {
            ScheduledFuture<?> scheduledTask = scheduleService.scheduleOnceFireTask(task, 0, TimeUnit.SECONDS);
            ActionHistoryStorage.putConfirmationTask(abstractMailMessageModel.getRecipient(), scheduledTask);
        } else {
            ActionHistoryStorage.removeConfirmationCode(abstractMailMessageModel.getRecipient());
        }
    }

    private String generateUniqueCode() {
        return UUID.randomUUID().toString();
    }

    private AbstractMailMessageModel processMailMessageModelForSending(AbstractMailMessageModel abstractMailMessageModel) throws InvalidAttributesException {
        String subject;
        String text;
        String uniqueCode = generateUniqueCode();
        String serverURI = String.format("%s://%s:%s", properties.get("protocol"), properties.get("host"), properties.get("port"));
        String recipient = abstractMailMessageModel.getRecipient();
        String action = abstractMailMessageModel.getActionType().getDescription();
        if (abstractMailMessageModel.getMailType() == MailType.HTML) {
            subject = "Confirmation message";
            text = "<a style=\"font-weight: bold; font-color: black; text-decoration: none;\" href=\"{link}\">Click here for verifying action</a>";
            text = text.replace("{link}", String.format("%s/api/utility/task/confirm/%s/%s/%s", serverURI, uniqueCode, recipient, action));
        } else {
            subject = abstractMailMessageModel.getSubject();
            text = abstractMailMessageModel.getText();
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
