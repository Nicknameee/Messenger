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
import spring.application.tree.data.utility.mailing.models.ActionType;
import spring.application.tree.data.utility.mailing.models.MailType;
import spring.application.tree.data.utility.tasks.ActionHistoryStorage;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {
    @Value("${spring.mail.username}")
    private String sender;
    private final JavaMailSender javaMailSender;

    public synchronized Runnable sendMessage(AbstractMailMessageModel abstractMailMessageModel) throws ApplicationException {
        validateMessageModel(abstractMailMessageModel);
        if (abstractMailMessageModel.getMailType() == MailType.HTML) {
            return sendHtmlMailMessage(abstractMailMessageModel);
        } else if (abstractMailMessageModel.getMailType() == MailType.PLAIN) {
            return sendMailMessage(abstractMailMessageModel);
        }
        return null;
    }

    private void validateMessageModel(AbstractMailMessageModel abstractMailMessageModel) throws InvalidAttributesException {
        StringBuilder exceptionMessage = new StringBuilder();
        if (abstractMailMessageModel.getRecipient() == null || abstractMailMessageModel.getRecipient().isEmpty()) {
            exceptionMessage.append(String.format("Invalid recipient email: %s ", abstractMailMessageModel.getRecipient()));
        }
        if (abstractMailMessageModel.getMailType() == null) {
            exceptionMessage.append(String.format("Invalid mail type: %s ", abstractMailMessageModel.getMailType()));
        }
        if (abstractMailMessageModel.getActionType() == null) {
            exceptionMessage.append(String.format("Invalid action type: %s ", abstractMailMessageModel.getActionType()));
        }
        if (ActionType.isSimpleAction(abstractMailMessageModel.getActionType())) {
            if (abstractMailMessageModel.getSubject() == null || abstractMailMessageModel.getSubject().isEmpty()) {
                exceptionMessage.append(String.format("Invalid subject: %s ", abstractMailMessageModel.getSubject()));
            }
            if (abstractMailMessageModel.getText() == null || abstractMailMessageModel.getText().isEmpty()) {
                exceptionMessage.append(String.format("Invalid text: %s", abstractMailMessageModel.getText()));
            }
        }
        if (!exceptionMessage.toString().isEmpty()) {
            throw new InvalidAttributesException(exceptionMessage.toString(),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @Deprecated
    private Runnable sendMailMessage(AbstractMailMessageModel abstractMailMessageModel) {
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

    private Runnable sendHtmlMailMessage(AbstractMailMessageModel abstractMailMessageModel) {
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
                if (ActionType.isConfirmationAction(abstractMailMessageModel.getActionType())) {
                    try {
                        ActionHistoryStorage.removeConfirmationCode(abstractMailMessageModel.getRecipient());
                    } catch (InvalidAttributesException ex) {
                        log.error(ex.getMessage(), ex);
                        log.error(String.format("Error occurs when trying to remove confirmation code on fail email sending, recipient: %s",
                                abstractMailMessageModel.getRecipient()));
                    }
                }
            }
        };
        return task;
    }
}
