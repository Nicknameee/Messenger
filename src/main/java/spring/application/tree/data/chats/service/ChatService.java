package spring.application.tree.data.chats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.application.tree.data.chats.models.AbstractChatModel;
import spring.application.tree.data.chats.repository.ChatDataAccessObject;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.exceptions.NotAllowedException;
import spring.application.tree.data.users.service.UserService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {
    private final ChatDataAccessObject chatDataAccessObject;

    public List<AbstractChatModel> getChats(int memberId) throws InvalidAttributesException {
        return chatDataAccessObject.getChats(memberId);
    }

    public List<AbstractChatModel> getChatsForCurrentUser() throws InvalidAttributesException, NotAllowedException {
        Integer userId = UserService.getIdOfCurrentlyAuthenticatedUser();
        if (userId == null) {
            throw new NotAllowedException("Chats data forbidden, no authorization detected",
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.FORBIDDEN);
        }
        return chatDataAccessObject.getChats(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public int addChat(AbstractChatModel abstractChatModel) throws InvalidAttributesException {
        return chatDataAccessObject.addChat(abstractChatModel);
    }

    public void updateChat(AbstractChatModel abstractChatModel) throws InvalidAttributesException, NotAllowedException {
        Integer currentUserId = UserService.getIdOfCurrentlyAuthenticatedUser();
        if (currentUserId == null || abstractChatModel.getAuthorId() != currentUserId) {
            throw new NotAllowedException(String.format("User`s ID does not match chat`s author ID: %s", abstractChatModel.getAuthorId()),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        chatDataAccessObject.updateChat(abstractChatModel);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void deleteChat(int chatId, int authorId) throws InvalidAttributesException, NotAllowedException {
        Integer currentUserId = UserService.getIdOfCurrentlyAuthenticatedUser();
        if (currentUserId == null || authorId != currentUserId) {
            throw new NotAllowedException(String.format("User`s ID does not match chat`s author ID: %s", authorId),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        chatDataAccessObject.deleteChat(chatId);
    }

    public void passChatOwnerRightsToUserWithId(int chatId, int newAuthorId) throws NotAllowedException, InvalidAttributesException {
        Integer currentUserId = UserService.getIdOfCurrentlyAuthenticatedUser();
        if (currentUserId == null || newAuthorId == currentUserId) {
            throw new NotAllowedException(String.format("New owner ID matches current chat owner: %s", newAuthorId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        chatDataAccessObject.changeChatOwner(chatId, newAuthorId);
    }

    public List<Integer> getChatIdsOwnedByUser(int authorId) throws InvalidAttributesException {
        return chatDataAccessObject.getChatIdsOwnedByUser(authorId);
    }

    public boolean checkUserPresenceInChat(int userId, int chatId) throws InvalidAttributesException {
        return chatDataAccessObject.checkUserPresenceInChat(userId, chatId);
    }
}
