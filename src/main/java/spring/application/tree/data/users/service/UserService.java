package spring.application.tree.data.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.application.tree.data.chats.service.ChatService;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.exceptions.CredentialsException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.exceptions.NotAllowedException;
import spring.application.tree.data.users.models.AbstractUserModel;
import spring.application.tree.data.users.repository.UserDataAccessObject;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserDataAccessObject userDataAccessObject;
    private final ChatService chatService;

    public List<AbstractUserModel> getChatMembers(int chatId) throws InvalidAttributesException, NotAllowedException {
        Integer currentUserId = getIdOfCurrentlyAuthenticatedUser();
        if (currentUserId == null || !chatService.checkUserPresenceInChat(currentUserId, chatId)) {
            throw new NotAllowedException(String.format("Access to chat with ID: %s denied, you not participating it", chatId),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.FORBIDDEN);
        }
        return userDataAccessObject.getChatMembers(chatId);
    }

    public static AbstractUserModel getCurrentlyAuthenticatedUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null && securityContext.getAuthentication().isAuthenticated()) {
            if (securityContext.getAuthentication().getPrincipal() instanceof AbstractUserModel) {
                return (AbstractUserModel) securityContext.getAuthentication().getPrincipal();
            } else {
                return null;
            }
        }
        return null;
    }

    public static Integer getIdOfCurrentlyAuthenticatedUser() {
        AbstractUserModel abstractUserModel = getCurrentlyAuthenticatedUser();
        return abstractUserModel == null ? null : abstractUserModel.getId();
    }

    public AbstractUserModel getUserByLoginCredentials(String login) throws ApplicationException {
        return userDataAccessObject.getUserByLoginCredentials(login);
    }

    public boolean checkUsernameAvailability(String username) throws ApplicationException {
        return userDataAccessObject.checkUsernameAvailability(username);
    }

    public boolean checkEmailAvailability(String email) throws ApplicationException {
        return userDataAccessObject.checkEmailAvailability(email);
    }

    public void saveUser(AbstractUserModel abstractUserModel) throws ApplicationException {
        if (!checkUserCredentialsAvailable(abstractUserModel.getEmail(), abstractUserModel.getUsername())) {
            throw new CredentialsException(String.format("Credentials are taken, email: %s, username: %s", abstractUserModel.getEmail(), abstractUserModel.getUsername()),
                                           Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                           LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userDataAccessObject.saveUser(abstractUserModel);
    }

    private boolean checkUserCredentialsAvailable(String email, String username) throws InvalidAttributesException {
        return userDataAccessObject.checkUserCredentialsAvailable(email, username);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void deleteUser(HttpServletRequest httpRequest) throws ApplicationException {
        Integer id = getIdOfCurrentlyAuthenticatedUser();
        if (id == null) {
            throw new NotAllowedException("Account deletion not allowed, no authorization detected",
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.FORBIDDEN);
        }
        processChatsOnAuthorAccountDeletion(id);
        userDataAccessObject.deleteUserById(id);
        SecurityContextHolder.clearContext();
        httpRequest.getSession().invalidate();
    }

    private void processChatsOnAuthorAccountDeletion(int userId) throws InvalidAttributesException, NotAllowedException {
        int ownedChatsCount = userDataAccessObject.countCreatedChatsByUser(userId);
        if (ownedChatsCount > 0) {
            List<Integer> chatIds = chatService.getChatIdsOwnedByUser(userId);
            for (Integer chatId : chatIds) {
                exitFromChat(chatId);
                int chatMembersCount = userDataAccessObject.countChatMembers(chatId);
                if (chatMembersCount > 0) {
                    int memberId = userDataAccessObject.getRandomChatSimpleMemberId(chatId, userId);
                    chatService.passChatOwnerRightsToUserWithId(chatId, memberId);
                } else {
                    chatService.deleteChat(chatId, userId);
                }
            }
        }
    }

    public void addUserToChat(int userId, int chatId) throws InvalidAttributesException {
        userDataAccessObject.addUserToChat(userId, chatId);
    }

    public void removerUserFromChat(int userId, int chatId) throws InvalidAttributesException {
        userDataAccessObject.removeUserFromChat(userId, chatId);
    }

    public void joinChat(int chatId) throws InvalidAttributesException {
        Integer userId = getIdOfCurrentlyAuthenticatedUser();
        if (userId != null) {
            userDataAccessObject.addUserToChat(userId, chatId);
        }
    }
    public void exitFromChat(int chatId) throws InvalidAttributesException {
        Integer userId = getIdOfCurrentlyAuthenticatedUser();
        if (userId != null) {
            userDataAccessObject.removeUserFromChat(userId, chatId);
        }
    }

    public void updateUserLoginTime(String username) throws InvalidAttributesException {
        userDataAccessObject.updateUserLoginTime(username);
    }

    public void updateUserLogoutTime(String username) throws InvalidAttributesException {
        userDataAccessObject.updateUserLogoutTime(username);
    }
}
