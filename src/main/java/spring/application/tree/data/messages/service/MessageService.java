package spring.application.tree.data.messages.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.messages.models.AbstractMessageModel;
import spring.application.tree.data.messages.repository.MessageDataAccessObject;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageService {
    private final MessageDataAccessObject messageDataAccessObject;

    public List<AbstractMessageModel> getMessages(int chatId) throws InvalidAttributesException {
        return messageDataAccessObject.getMessages(chatId);
    }

    public int addMessage(AbstractMessageModel abstractMessageModel) throws InvalidAttributesException {
        return messageDataAccessObject.addMessage(abstractMessageModel);
    }

    public void updateMessage(int messageId, String message) throws InvalidAttributesException {
        messageDataAccessObject.updateMessage(messageId, message);
    }

    public void deleteMessage(int messageId, int chatId) throws InvalidAttributesException {
        messageDataAccessObject.deleteMessage(messageId, chatId);
    }
}
