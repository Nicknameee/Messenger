package spring.application.tree.data.messages.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import spring.application.tree.data.chats.service.ChatService;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.exceptions.NotAllowedException;
import spring.application.tree.data.messages.models.AbstractMessageModel;
import spring.application.tree.data.messages.repository.MessageDataAccessObject;
import spring.application.tree.data.users.service.UserService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageService {
    private final MessageDataAccessObject messageDataAccessObject;
    private final ChatService chatService;

    public List<AbstractMessageModel> getMessages(int chatId) throws InvalidAttributesException {
        return messageDataAccessObject.getMessages(chatId);
    }

    public int addMessage(AbstractMessageModel abstractMessageModel) throws InvalidAttributesException, NotAllowedException {
        int authorId = abstractMessageModel.getAuthorId();
        int chatId = abstractMessageModel.getChatId();
        if (!chatService.checkUserPresenceInChat(authorId, chatId)) {
            throw new NotAllowedException(String.format("User with ID: %s is not participating chat with ID: %s, message sending is forbidden", authorId, chatId),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        return messageDataAccessObject.addMessage(abstractMessageModel);
    }

    public void updateMessage(AbstractMessageModel abstractMessageModel) throws InvalidAttributesException, NotAllowedException {
        int authorId = abstractMessageModel.getAuthorId();
        Integer currentUserId = UserService.getIdOfCurrentlyAuthenticatedUser();
        if (currentUserId == null || currentUserId != authorId) {
            throw new NotAllowedException(String.format("User with ID: %s is not author of this message, editing is forbidden", authorId),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        messageDataAccessObject.updateMessage(abstractMessageModel.getId(), abstractMessageModel.getMessage());
    }

    public void deleteMessage(AbstractMessageModel abstractMessageModel) throws InvalidAttributesException, NotAllowedException {
        int authorId = abstractMessageModel.getAuthorId();
        int chatId = abstractMessageModel.getChatId();
        Integer currentUserId = UserService.getIdOfCurrentlyAuthenticatedUser();
        if (currentUserId == null || currentUserId != authorId) {
            throw new NotAllowedException(String.format("User with ID: %s is not author of this message, deletion is forbidden", authorId),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        messageDataAccessObject.deleteMessage(abstractMessageModel.getId(), chatId);
    }
}
