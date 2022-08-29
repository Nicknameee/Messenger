package spring.application.tree.data.chats.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import spring.application.tree.data.chats.models.AbstractChatModel;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.users.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ChatDataAccessObject {
    private final JdbcTemplate jdbcTemplate;

    public List<AbstractChatModel> getChats(int memberId) throws InvalidAttributesException {
        if (memberId <= 0) {
            throw new InvalidAttributesException(String.format("Member ID is invalid: %s", memberId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "SELECT chats.id, chats.title, chats.description, chats.private, chats.password, chats.author_id " +
                             "FROM chats " +
                             "INNER JOIN users_to_chats ON chats.id = users_to_chats.chat_id " +
                             "WHERE users_to_chats.user_id = ?;";
        List<AbstractChatModel> chats = new ArrayList<>();
        jdbcTemplate.query(query, new Object[]{memberId}, resultSet -> {
            int id = resultSet.getInt("id");
            String title = resultSet.getString("title");
            String description = resultSet.getString("description");
            boolean isPrivate = resultSet.getBoolean("private");
            String password = resultSet.getString("password");
            int authorId = resultSet.getInt("author_id");
            chats.add(new AbstractChatModel(id, title, description, isPrivate, password, authorId));
        });
        return chats;
    }

    public int addChat(AbstractChatModel abstractChatModel) throws InvalidAttributesException {
        if (abstractChatModel.getTitle() == null || abstractChatModel.getTitle().isEmpty() ||
            (abstractChatModel.isPrivate() && (abstractChatModel.getPassword() == null || abstractChatModel.getPassword().isEmpty())) ||
            abstractChatModel.getAuthorId() <= 0) {
            throw new InvalidAttributesException(buildExceptionMessageForValidationOfChatModel(abstractChatModel),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "INSERT INTO chats(title, description, private, password, author_id) VALUES(?, ?, ?, ?, ?) RETURNING id;";
        final String queryUsersToChatAssociationUpdate = "INSERT INTO users_to_chats(user_id, chat_id) VALUES(?, ?);";
        int chatId = jdbcTemplate.update(query, abstractChatModel.getTitle(), abstractChatModel.getDescription(), abstractChatModel.isPrivate(),
                                   abstractChatModel.getPassword(), abstractChatModel.getAuthorId());
        Integer userId = UserService.getIdOfCurrentlyAuthenticatedUser();
        jdbcTemplate.update(queryUsersToChatAssociationUpdate, userId, chatId);
        return chatId;
    }

    public void updateChat(AbstractChatModel abstractChatModel) throws InvalidAttributesException {
        if (abstractChatModel.getTitle() == null || abstractChatModel.getTitle().isEmpty() ||
            (abstractChatModel.isPrivate() && (abstractChatModel.getPassword() == null || abstractChatModel.getPassword().isEmpty())) ||
            abstractChatModel.getAuthorId() <= 0) {
            throw new InvalidAttributesException(buildExceptionMessageForValidationOfChatModel(abstractChatModel),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "UPDATE chats SET title = ?, description = ?, private = ?, password = ? WHERE id = ?;";
        jdbcTemplate.update(query, abstractChatModel.getTitle(), abstractChatModel.getDescription(),
                                   abstractChatModel.isPrivate(), abstractChatModel.getPassword(), abstractChatModel.getId());
    }

    public void deleteChat(int chatId) throws InvalidAttributesException {
        if (chatId <= 0) {
            throw new InvalidAttributesException(String.format("Chat ID is invalid: %s", chatId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "DELETE FROM chats WHERE id = ?;";
        final String queryUsersToChatAssociationUpdate = "DELETE FROM users_to_chats WHERE chat_id = ?;";
        final String queryMessagesToChatAssociationUpdate = "DELETE FROM messages WHERE chat_id = ?;";
        jdbcTemplate.update(query, chatId);
        jdbcTemplate.update(queryUsersToChatAssociationUpdate, chatId);
        jdbcTemplate.update(queryMessagesToChatAssociationUpdate, chatId);
    }

    private String buildExceptionMessageForValidationOfChatModel(AbstractChatModel abstractChatModel) {
        StringBuilder exceptionText = new StringBuilder();
        if (abstractChatModel.getTitle() == null || abstractChatModel.getTitle().isEmpty()) {
            exceptionText.append(String.format("Title is invalid: %s ", abstractChatModel.getTitle()));
        }
        if (abstractChatModel.isPrivate() && (abstractChatModel.getPassword() == null || abstractChatModel.getPassword().isEmpty())) {
            exceptionText.append(String.format("Password is invalid: %s ", abstractChatModel.getPassword()));
        }
        if (abstractChatModel.getAuthorId() <= 0) {
            exceptionText.append(String.format("Member ID is invalid: %s", abstractChatModel.getAuthorId()));
        }
        return exceptionText.toString();
    }
}
