package spring.application.tree.data.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.application.tree.data.chats.attributes.ChatType;
import spring.application.tree.data.chats.service.ChatService;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.exceptions.DataNotFoundException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.exceptions.NotAllowedException;
import spring.application.tree.data.users.models.AbstractUserModel;
import spring.application.tree.data.users.repository.UserDataAccessObject;
import spring.application.tree.data.users.security.DataEncoderTool;
import spring.application.tree.data.utility.tasks.TaskUtility;

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

    public void saveUser(AbstractUserModel abstractUserModel, HttpServletRequest httpServletRequest) throws ApplicationException {
        if (!checkUserCredentialsAvailable(abstractUserModel.getEmail(), abstractUserModel.getUsername())) {
            throw new NotAllowedException(String.format("Credentials are taken, email: %s, username: %s", abstractUserModel.getEmail(), abstractUserModel.getUsername()),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        Runnable postponeSuccessTask = () -> {
            try {
                enableUser(abstractUserModel.getEmail());
            } catch (InvalidAttributesException e) {
                log.error(e.getMessage(), e);
                log.error(String.format("Could not enable user: %s", abstractUserModel.getEmail()));
            }
        };
        String origin = extractSourceURI(httpServletRequest);
        TaskUtility.putSuccessConfirmationTask(abstractUserModel.getEmail(), origin, postponeSuccessTask);
        userDataAccessObject.saveUser(abstractUserModel);
    }

    public void updateUser(AbstractUserModel updatedUser) throws ApplicationException {
        AbstractUserModel oldUser = userDataAccessObject.getUserById(updatedUser.getId());
        if (oldUser == null) {
            throw new DataNotFoundException(String.format("User with following ID was not found: %s", updatedUser.getId()),
                                            Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                            LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        oldUser.mergeChanges(updatedUser);
        userDataAccessObject.updateUser(oldUser);
    }

    public void restoreUserPassword(String email, String newPassword, HttpServletRequest httpServletRequest) throws ApplicationException {
        if (checkEmailAvailability(email)) {
            throw new DataNotFoundException(String.format("No account signed to email was found: %s", email),
                                            Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                            LocalDateTime.now(), HttpStatus.NOT_FOUND);
        }
        Runnable postponeSuccessTask = () -> {
            try {
                updateUserPassword(email, newPassword);
            } catch (ApplicationException e) {
                log.error(e.getMessage(), e);
                log.error(String.format("Could not update password for user: %s", email));
            }
        };
        String origin = extractSourceURI(httpServletRequest);
        TaskUtility.putSuccessConfirmationTask(email, origin, postponeSuccessTask);
    }

    public void restoreUserEmail(String email, String username, HttpServletRequest httpServletRequest) throws ApplicationException {
        if (!checkEmailAvailability(email) || checkUsernameAvailability(username)) {
            throw new NotAllowedException(String.format("Email: %s is taken or no signed to username account was found: %s", email, username),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.NOT_FOUND);
        }
        Runnable postponeSuccessTask = () -> {
            try {
                updateUserEmail(email, username);
            } catch (ApplicationException e) {
                log.error(e.getMessage(), e);
                log.error(String.format("Could not update email for user: %s", username));
            }
        };
        String origin = extractSourceURI(httpServletRequest);
        TaskUtility.putSuccessConfirmationTask(email, origin, postponeSuccessTask);
    }

    public void updateUserPassword(String email, String newPassword) throws ApplicationException {
        userDataAccessObject.updateUserPassword(email, DataEncoderTool.encodeData(newPassword));
    }

    public void updateUserEmail(String email, String username) throws ApplicationException {
        userDataAccessObject.updateUserEmail(email, username);
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
                userDataAccessObject.removeUserFromChat(userId, chatId);
                int chatMembersCount = userDataAccessObject.countChatMembers(chatId);
                ChatType chatType = chatService.getChatType(chatId);
                if (chatMembersCount > 0 && chatType == ChatType.GROUP) {
                    int memberId = userDataAccessObject.getRandomChatSimpleMemberId(chatId, userId);
                    chatService.passChatOwnerRightsToUserWithId(chatId, memberId);
                } else {
                    chatService.deleteChat(chatId, userId);
                }
            }
        }
    }

    public void addUserToChat(int userId, int chatId) throws InvalidAttributesException, NotAllowedException {
        Integer currentUserId = getIdOfCurrentlyAuthenticatedUser();
        if (currentUserId == null || !chatService.getChatIdsOwnedByUser(currentUserId).contains(chatId)) {
            throw new NotAllowedException(String.format("Member adding declined, only author of chat able to do that, chat ID: %s", chatId),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.FORBIDDEN);
        }
        ChatType chatType = chatService.getChatType(chatId);
        int chatMembersCount = countChatMembers(chatId);
        if (!chatType.isUserAddingAllowed(chatMembersCount)) {
            throw new NotAllowedException(String.format("Member adding declined, chat type restricts it, chat ID: %s, chat type: %s", chatId, chatType),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.FORBIDDEN);
        }
        userDataAccessObject.addUserToChat(userId, chatId);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void removeUserFromChat(int userId, int chatId) throws InvalidAttributesException, NotAllowedException {
        Integer currentUserId = getIdOfCurrentlyAuthenticatedUser();
        if (currentUserId == null || !chatService.getChatIdsOwnedByUser(currentUserId).contains(chatId)) {
            throw new NotAllowedException(String.format("Member removing declined, only author of chat able to do that, chat ID: %s", chatId),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.FORBIDDEN);
        }
        if (currentUserId == userId) {
            throw new NotAllowedException(String.format("Member removing declined, author of chat can not be removed, chat ID: %s", chatId),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.FORBIDDEN);
        }
        userDataAccessObject.removeUserFromChat(userId, chatId);
    }

    public void joinChat(int chatId, String password) throws InvalidAttributesException, NotAllowedException {
        Integer userId = getIdOfCurrentlyAuthenticatedUser();
        if (userId != null) {
            String chatPassword = chatService.getChatPassword(chatId);
            ChatType chatType = chatService.getChatType(chatId);
            int chatMembersCount = countChatMembers(chatId);
            boolean joiningAllowed = chatPassword == null || (chatPassword.equals(password)) || chatType.isUserAddingAllowed(chatMembersCount);
            if (!joiningAllowed) {
                throw new NotAllowedException(String.format("Joining declined, password does not match or chat type does not support that action, chat ID: %s, chat type: %s", chatId, chatType),
                                              Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                              LocalDateTime.now(), HttpStatus.FORBIDDEN);
            }
            userDataAccessObject.addUserToChat(userId, chatId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void exitChat(int chatId) throws InvalidAttributesException, NotAllowedException {
        Integer userId = getIdOfCurrentlyAuthenticatedUser();
        if (userId != null) {
            if (chatService.getChatIdsOwnedByUser(userId).contains(chatId)) {
                processChatsOnAuthorAccountDeletion(userId);
            } else {
                userDataAccessObject.removeUserFromChat(userId, chatId);
            }
        }
    }

    public void updateUserLoginTime(String username) throws InvalidAttributesException {
        userDataAccessObject.updateUserLoginTime(username);
    }

    public void updateUserLogoutTime(String username) throws InvalidAttributesException {
        userDataAccessObject.updateUserLogoutTime(username);
    }

    public void enableUser(String email) throws InvalidAttributesException {
        userDataAccessObject.enableUser(email);
    }

    public void disableUser(String email) throws InvalidAttributesException {
        userDataAccessObject.disableUser(email);
    }

    public void deleteActivationExpiredAccountByLogin(String login) throws InvalidAttributesException {
        userDataAccessObject.deleteActivationExpiredAccountByLogin(login);
    }

    public int countChatMembers(int chatId) throws InvalidAttributesException {
        return userDataAccessObject.countChatMembers(chatId);
    }

    private String extractSourceURI(HttpServletRequest httpServletRequest) {
        String origin = httpServletRequest.getHeader("Origin");
        if (origin == null) {
            String scheme = httpServletRequest.getScheme();
            String server = httpServletRequest.getServerName();
            String port = String.valueOf(httpServletRequest.getServerPort());
            origin = String.format("%s://%s:%s", scheme, server, port);
        }
        return origin;
    }
}
