package spring.application.tree.data.utility.mailing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.utility.mailing.models.AbstractMailMessageModel;
import spring.application.tree.data.utility.mailing.models.MailType;
import spring.application.tree.data.utility.properties.CustomPropertyDataLoader;
import spring.application.tree.data.utility.properties.CustomPropertySourceConverter;
import spring.application.tree.data.utility.tasks.ActionHistoryStorage;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {
    @Value("${spring.mail.username}")
    private String sender;
    public static Map<String, String> properties;
    private final JavaMailSender javaMailSender;

    @PostConstruct
    private void initializeProperties() {
        properties = CustomPropertySourceConverter.convertToKeyValueFormat(CustomPropertyDataLoader.getResourceContent("classpath:mail.properties"));
    }

    public synchronized Runnable sendMessage(AbstractMailMessageModel abstractMailMessageModel) throws ApplicationException {
        if (abstractMailMessageModel.getRecipient() == null || abstractMailMessageModel.getRecipient().isEmpty() ||
            abstractMailMessageModel.getMailType() == null  || abstractMailMessageModel.getActionType() == null) {
            throw new InvalidAttributesException(buildExceptionMessageForValidationOfMessageModel(abstractMailMessageModel),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        AbstractMailMessageModel processedMailMessage = processMailMessageModelForSending(abstractMailMessageModel);
        if (processedMailMessage.getMailType() == MailType.HTML) {
            return sendHtmlMailMessage(processedMailMessage);
        }
        return null;
    }

    private String buildExceptionMessageForValidationOfMessageModel(AbstractMailMessageModel abstractMailMessageModel) {
        StringBuilder exceptionMessage = new StringBuilder();
        if (abstractMailMessageModel.getRecipient() == null || abstractMailMessageModel.getRecipient().isEmpty()) {
            exceptionMessage.append(String.format("Invalid recipient email: %s", abstractMailMessageModel.getRecipient()));
        }
        if (abstractMailMessageModel.getMailType() == null) {
            exceptionMessage.append(String.format("Invalid mail type: %s", abstractMailMessageModel.getMailType()));
        }
        if (abstractMailMessageModel.getActionType() == null) {
            exceptionMessage.append(String.format("Invalid action type: %s", abstractMailMessageModel.getActionType()));
        }
        return exceptionMessage.toString();
    }

    @Deprecated
    private Runnable sendMailMessage(AbstractMailMessageModel abstractMailMessageModel) throws InvalidAttributesException {
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
        return task;
    }

    private Runnable sendHtmlMailMessage(AbstractMailMessageModel abstractMailMessageModel) throws InvalidAttributesException {
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
                try {
                     ActionHistoryStorage.removeConfirmationCode(abstractMailMessageModel.getRecipient());
                } catch (InvalidAttributesException ex) {
                    log.error(ex.getMessage(), ex);
                    log.error(String.format("Error occurs when trying to remove confirmation code on fail email sending, recipient: %s",
                                             abstractMailMessageModel.getRecipient()));
                }
                synchronized (MailUtility.userToCancellationTask) {
                    if (MailUtility.userToCancellationTask.containsKey(abstractMailMessageModel.getRecipient()) &&
                        MailUtility.userToCancellationTask.get(abstractMailMessageModel.getRecipient()).getKey() == abstractMailMessageModel.getActionType()) {
                        MailUtility.userToCancellationTask.get(abstractMailMessageModel.getRecipient()).getValue().cancel(true);
                        MailUtility.userToCancellationTask.remove(abstractMailMessageModel.getRecipient());
                    }
                }
            }
        };
        return task;
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
            text += "<p>Link expires in {duration} seconds</p>";
            text = text.replace("{link}", String.format("%s/api/utility/task/confirm/%s/%s/%s", serverURI, uniqueCode, recipient, action));
            text = text.replace("{duration}", properties.get("duration"));
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
