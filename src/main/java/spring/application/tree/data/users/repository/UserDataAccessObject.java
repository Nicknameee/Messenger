package spring.application.tree.data.users.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.users.attributes.Language;
import spring.application.tree.data.users.attributes.Role;
import spring.application.tree.data.users.attributes.Status;
import spring.application.tree.data.users.models.AbstractUserModel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class UserDataAccessObject {
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public List<AbstractUserModel> getChatMembers(int chatId) throws InvalidAttributesException {
        if (chatId <= 0) {
            throw new InvalidAttributesException(String.format("Chat ID is invalid: %s", chatId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "SELECT u.id, u.username, u.email, u.login_time, u.logout_time, u.role, u.status, u.language, u.timezone " +
                             "FROM users u INNER JOIN users_to_chats utc ON u.id = utc.user_id WHERE utc.chat_id = ?;";
        List<AbstractUserModel> members = new ArrayList<>();
        jdbcTemplate.query(query, resultSet -> {
            AbstractUserModel abstractUserModel = new AbstractUserModel();
            abstractUserModel.setId(resultSet.getInt("id"));
            abstractUserModel.setUsername(resultSet.getString("username"));
            abstractUserModel.setEmail(resultSet.getString("email"));
            abstractUserModel.setLoginTime(resultSet.getTimestamp("login_time"));
            abstractUserModel.setLogoutTime(resultSet.getTimestamp("logout_time"));
            abstractUserModel.setRole(Role.valueOf(resultSet.getString("role")));
            abstractUserModel.setStatus(Status.valueOf(resultSet.getString("status")));
            abstractUserModel.setLanguage(Language.valueOf(resultSet.getString("language")));
            abstractUserModel.setTimezone(resultSet.getString("timezone"));
            members.add(abstractUserModel);
        }, chatId);
        return members;
    }

    public AbstractUserModel getUserByLoginCredentials(String login) throws ApplicationException {
        if (login == null || login.isEmpty()) {
            throw new InvalidAttributesException(String.format("Username/email is invalid: %s", login),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        return userRepository.findUserByLogin(login);
    }

    public AbstractUserModel getUserById(Integer id) throws InvalidAttributesException {
        if (id == null || id <= 0) {
            throw new InvalidAttributesException(String.format("User ID is invalid: %s", id),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        Optional<AbstractUserModel> abstractUserModelOptional =  userRepository.findById(id);
        return abstractUserModelOptional.orElse(null);
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

    public boolean checkUserCredentialsAvailable(String email, String username) throws InvalidAttributesException {
        if (username == null || username.isEmpty() || email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Username: %s or email is invalid: %s", username, email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        return userRepository.countAbstractUserModelsWithFollowingEmailOrUsername(email, username) == 0;
    }

    public void saveUser(AbstractUserModel abstractUserModel) throws ApplicationException {
        validateUserModel(abstractUserModel);
        userRepository.save(abstractUserModel);
    }

    public void updateUser(AbstractUserModel abstractUserModel) throws InvalidAttributesException {
        validateUserModel(abstractUserModel);
        userRepository.save(abstractUserModel);
    }

    public void deleteUserById(Integer id) throws ApplicationException {
        if (id <= 0L) {
            throw new InvalidAttributesException(String.format("ID is invalid: %s", id),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "DELETE FROM users_to_chats WHERE user_id = ?";
        jdbcTemplate.update(query, id);
        userRepository.deleteAbstractUserModelById(id);
    }

    private void validateUserModel(AbstractUserModel abstractUserModel) throws InvalidAttributesException {
        StringBuilder exceptionText = new StringBuilder();
        if (abstractUserModel.getUsername() == null || abstractUserModel.getUsername().isEmpty()) {
            exceptionText.append(String.format("Username is invalid: %s ", abstractUserModel.getUsername()));
        }
        if (abstractUserModel.getEmail() == null || abstractUserModel.getEmail().isEmpty()) {
            exceptionText.append(String.format("Email is invalid: %s ", abstractUserModel.getEmail()));
        }
        if (abstractUserModel.getPassword() == null || abstractUserModel.getPassword().isEmpty()) {
            exceptionText.append(String.format("Password is invalid: %s ", abstractUserModel.getPassword()));
        }
        if (abstractUserModel.getTimezone() == null || abstractUserModel.getTimezone().isEmpty() || !ZoneId.getAvailableZoneIds().contains(abstractUserModel.getTimezone())) {
            exceptionText.append(String.format("Timezone is invalid: %s ", abstractUserModel.getTimezone()));
        }
        if (!exceptionText.toString().isEmpty()) {
            throw new InvalidAttributesException(exceptionText.toString(),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
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

    public int countChatMembers(int chatId) throws InvalidAttributesException {
        if (chatId <= 0) {
            throw new InvalidAttributesException(String.format("Chat ID is invalid: %s", chatId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "SELECT COUNT(*) FROM users_to_chats WHERE chat_id = ?;";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, chatId);
        return count == null ? 0 : count;
    }

    public int countCreatedChatsByUser(int userId) throws InvalidAttributesException {
        if (userId <= 0) {
            throw new InvalidAttributesException(String.format("User ID is invalid: %s", userId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "SELECT COUNT(*) FROM chats WHERE author_id = ?;";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, userId);
        return count == null ? 0 : count;
    }

    public int getRandomChatSimpleMemberId(int chatId, int userId) throws InvalidAttributesException {
        if (userId <= 0 || chatId <= 0) {
            throw new InvalidAttributesException(String.format("User/Chat ID is invalid - user ID: %s, chat ID: %s", userId, chatId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "SELECT user_id FROM users_to_chats utc INNER JOIN chats c on utc.chat_id = c.id WHERE c.id = ? AND utc.user_id != ? LIMIT 1;";
        Integer memberId = jdbcTemplate.queryForObject(query, Integer.class, chatId, userId);
        return memberId == null ? -1 : memberId;
    }

    public void updateUserLoginTime(String username) throws InvalidAttributesException {
        if (username == null || username.isEmpty()) {
            throw new InvalidAttributesException(String.format("Username is invalid: %s", username),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userRepository.updateUserLoginTimeByUsername(username);
    }

    public void updateUserLogoutTime(String username) throws InvalidAttributesException {
        if (username == null || username.isEmpty()) {
            throw new InvalidAttributesException(String.format("Username is invalid: %s", username),
                    Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                    LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userRepository.updateUserLogoutTimeByUsername(username);
    }

    public void updateUserPassword(String login, String newPassword) throws InvalidAttributesException {
        if (login == null || login.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            throw new InvalidAttributesException(String.format("Username: %s or email is invalid: %s", login, newPassword),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userRepository.updateUserPassword(login, newPassword);
    }

    public void enableUser(String email) throws InvalidAttributesException {
        if (email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email is invalid: %s", email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userRepository.enableUser(email);
    }

    public void disableUser(String email) throws InvalidAttributesException {
        if (email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email is invalid: %s", email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userRepository.disableUser(email);
    }

    public void deleteActivationExpiredAccountByLogin(String login) throws InvalidAttributesException {
        if (login == null || login.isEmpty()) {
            throw new InvalidAttributesException(String.format("Username/email is invalid: %s", login),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userRepository.deleteDisabledUser(login);
    }
}
