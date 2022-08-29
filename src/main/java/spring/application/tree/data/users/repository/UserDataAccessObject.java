package spring.application.tree.data.users.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.users.models.AbstractUserModel;
import spring.application.tree.data.users.service.UserService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Repository
@Slf4j
@RequiredArgsConstructor
public class UserDataAccessObject {
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public List<AbstractUserModel> getAllUsersList() {
        return userRepository.findAll();
    }

    public AbstractUserModel getUserByLoginCredentials(String login) throws ApplicationException {
        if (login == null || login.isEmpty()) {
            throw new InvalidAttributesException(String.format("Username/email is invalid: %s", login),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        return userRepository.findUserByLogin(login);
    }

    public boolean checkUsernameAvailability(String username) throws ApplicationException {
        if (username == null || username.isEmpty()) {
            throw new InvalidAttributesException(String.format("Username is invalid: %s", username),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        return userRepository.countAbstractUserModelsWithFollowingUsername(username) == 0;
    }

    public boolean checkEmailAvailability(String email) throws ApplicationException {
        if (email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email is invalid: %s", email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        return userRepository.countAbstractUserModelsWithFollowingEMail(email) == 0;
    }

    public boolean checkUserCredentialsAvailable(String email, String username) {
        return userRepository.countAbstractUserModelsWithFollowingEmailOrUsername(email, username) == 0;
    }

    public void saveUser(AbstractUserModel abstractUserModel) throws ApplicationException {
        if (abstractUserModel.getUsername() == null || abstractUserModel.getUsername().isEmpty() ||
            abstractUserModel.getEmail() == null    || abstractUserModel.getEmail().isEmpty() ||
            abstractUserModel.getPassword() == null || abstractUserModel.getPassword().isEmpty()) {
            throw new InvalidAttributesException(buildExceptionMessageForValidationOfUserModel(abstractUserModel),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userRepository.save(abstractUserModel);
    }

    public void deleteUserById(Long id) throws ApplicationException {
        if (id <= 0L) {
            throw new InvalidAttributesException(String.format("ID is invalid: %s", id),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userRepository.deleteAbstractUserModelById(id);
    }

    public void deleteUserByEmail(String email) throws ApplicationException {
        if (email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email is invalid: %s", email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userRepository.deleteAbstractUserModelByEmail(email);
    }

    public void deleteUserByUsername(String username) throws ApplicationException {
        if (username == null || username.isEmpty()) {
            throw new InvalidAttributesException(String.format("Username is invalid: %s", username),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userRepository.deleteAbstractUserModelByUsername(username);
    }

    private String buildExceptionMessageForValidationOfUserModel(AbstractUserModel abstractUserModel) {
        StringBuilder exceptionText = new StringBuilder();
        if (abstractUserModel.getUsername() == null || abstractUserModel.getUsername().isEmpty()) {
            exceptionText.append(String.format("Username is invalid: %s ", abstractUserModel.getUsername()));
        }
        if (abstractUserModel.getEmail() == null || abstractUserModel.getEmail().isEmpty()) {
            exceptionText.append(String.format("Email is invalid: %s ", abstractUserModel.getEmail()));
        }
        if (abstractUserModel.getPassword() == null || abstractUserModel.getPassword().isEmpty()) {
            exceptionText.append(String.format("Password is invalid: %s", abstractUserModel.getPassword()));
        }
        return exceptionText.toString();
    }

    public void addUserToChat(int userId, int chatId) throws InvalidAttributesException {
        if (userId <= 0 || chatId <= 0) {
            throw new InvalidAttributesException(String.format("User/Chat ID is invalid - user ID: %s, chat ID: %s", userId, chatId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "INSERT INTO users_to_chats(user_id, chat_id) VALUES(?, ?);";
        jdbcTemplate.update(query, userId, chatId);
    }

    public void removeUserFromChat(int userId, int chatId) throws InvalidAttributesException {
        if (userId <= 0 || chatId <= 0) {
            throw new InvalidAttributesException(String.format("User/Chat ID is invalid - user ID: %s, chat ID: %s", userId, chatId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "DELETE FROM users_to_chats WHERE user_id = ? AND chat_id = ?;";
        jdbcTemplate.update(query, userId, chatId);
    }
}
