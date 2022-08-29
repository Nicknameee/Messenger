package spring.application.tree.data.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.exceptions.CredentialsException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.users.models.AbstractUserModel;
import spring.application.tree.data.users.repository.UserDataAccessObject;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserDataAccessObject userDataAccessObject;

    public List<AbstractUserModel> getAllUsersList() {
        return userDataAccessObject.getAllUsersList();
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

    private boolean checkUserCredentialsAvailable(String email, String username) {
        return userDataAccessObject.checkUserCredentialsAvailable(email, username);
    }

    public void deleteUserById(Long id) throws ApplicationException {
        userDataAccessObject.deleteUserById(id);
    }

    public void deleteUserByEmail(String email) throws ApplicationException {
        userDataAccessObject.deleteUserByEmail(email);
    }

    public void deleteUserByUsername(String username) throws ApplicationException {
        userDataAccessObject.deleteUserByUsername(username);
    }

    public void addUserToChat(int userId, int chatId) throws InvalidAttributesException {
        userDataAccessObject.addUserToChat(userId, chatId);
    }

    public void removerUserFromChat(int userId, int chatId) throws InvalidAttributesException {
        userDataAccessObject.removeUserFromChat(userId, chatId);
    }

    public void exitFromChat(int chatId) throws InvalidAttributesException {
        Integer userId = UserService.getIdOfCurrentlyAuthenticatedUser();
        if (userId != null) {
            userDataAccessObject.removeUserFromChat(userId, chatId);
        }
    }
}
