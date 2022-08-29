package spring.application.tree.data.chats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.application.tree.data.chats.models.AbstractChatModel;
import spring.application.tree.data.chats.repository.ChatDataAccessObject;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.users.service.UserService;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {
    private final ChatDataAccessObject chatDataAccessObject;

    public List<AbstractChatModel> getChats(int memberId) throws InvalidAttributesException {
        return chatDataAccessObject.getChats(memberId);
    }

    public List<AbstractChatModel> getChatsForCurrentUser() throws InvalidAttributesException {
        Integer userId = UserService.getIdOfCurrentlyAuthenticatedUser();
        return userId == null ? new ArrayList<>() : chatDataAccessObject.getChats(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public int addChat(AbstractChatModel abstractChatModel) throws InvalidAttributesException {
        return chatDataAccessObject.addChat(abstractChatModel);
    }

    public void updateChat(AbstractChatModel abstractChatModel) throws InvalidAttributesException {
        chatDataAccessObject.updateChat(abstractChatModel);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void deleteChat(int chatId) throws InvalidAttributesException {
        chatDataAccessObject.deleteChat(chatId);
    }
}
