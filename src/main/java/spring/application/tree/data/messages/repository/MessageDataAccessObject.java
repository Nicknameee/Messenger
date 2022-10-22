package spring.application.tree.data.messages.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.messages.attributes.MessageType;
import spring.application.tree.data.messages.models.AbstractMessageModel;
import spring.application.tree.data.users.service.UserService;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@Slf4j
@RequiredArgsConstructor
public class MessageDataAccessObject {
    private final JdbcTemplate jdbcTemplate;

    public List<AbstractMessageModel> getMessages(int chatId) throws InvalidAttributesException {
        if (chatId <= 0) {
            throw new InvalidAttributesException(String.format("Chat ID is invalid: %s", chatId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "SELECT id, message, sent_at, author_id, type FROM messages WHERE chat_id = ? ORDER BY sent_at DESC;";
        List<AbstractMessageModel> messages = new ArrayList<>();
        jdbcTemplate.query(query, resultSet -> {
            int id = resultSet.getInt("id");
            String message = resultSet.getString("message");
            Date sendingDate = resultSet.getTimestamp("sent_at");
            int authorId = resultSet.getInt("author_id");
            MessageType messageType = MessageType.valueOf(resultSet.getString("type"));
            messages.add(new AbstractMessageModel(id, message, sendingDate, authorId, chatId, messageType));
        }, chatId);
        return messages;
    }

    public int addMessage(AbstractMessageModel abstractMessageModel) throws InvalidAttributesException {
        validateMessageModel(abstractMessageModel);
        final String query = "INSERT INTO messages(message, sent_at, author_id, chat_id, type) VALUES(?, now(), ?, ?, ?) RETURNING id;";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(query, new String[] {"id"});
            preparedStatement.setString(1, abstractMessageModel.getMessage());
            preparedStatement.setInt(2, abstractMessageModel.getAuthorId());
            preparedStatement.setInt(3, abstractMessageModel.getChatId());
            preparedStatement.setString(4, abstractMessageModel.getMessageType().name());
            return preparedStatement;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).intValue();
    }

    public void updateMessage(int messageId, String message) throws InvalidAttributesException {
        if (messageId <= 0 || message == null || message.isEmpty()) {
            throw new InvalidAttributesException(String.format("Message ID: %s or message: %s is invalid", messageId, message),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "UPDATE messages SET message = ? WHERE id = ?;";
        jdbcTemplate.update(query, message, messageId);
    }

    public void updateMessageType(int messageId, MessageType messageType) throws InvalidAttributesException {
        if (messageId <= 0 || messageType == null) {
            throw new InvalidAttributesException(String.format("Message ID: %s or message type: %s is invalid", messageId, messageType),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "UPDATE messages SET type = ? WHERE id = ?";
        jdbcTemplate.update(query, messageType.name(), messageId);
    }

    public void deleteMessage(int messageId) throws InvalidAttributesException {
        if (messageId <= 0) {
            throw new InvalidAttributesException(String.format("Message ID is invalid: %s", messageId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "DELETE FROM messages WHERE id = ?;";
        jdbcTemplate.update(query, messageId);
    }

    private void validateMessageModel(AbstractMessageModel abstractMessageModel) throws InvalidAttributesException {
        StringBuilder exceptionText = new StringBuilder();
        if (abstractMessageModel.getMessage() == null || abstractMessageModel.getMessage().isEmpty()) {
            exceptionText.append(String.format("Message is invalid: %s ", abstractMessageModel.getMessage()));
        }
        if (abstractMessageModel.getAuthorId() <= 0) {
            exceptionText.append(String.format("Author ID is invalid: %s ", abstractMessageModel.getAuthorId()));
        }
        if (abstractMessageModel.getChatId() <= 0) {
            exceptionText.append(String.format("Chat ID is invalid: %s", abstractMessageModel.getChatId()));
        }
        if (abstractMessageModel.getMessageType() == null) {
            exceptionText.append(String.format("Message type is invalid: %s", abstractMessageModel.getMessageType()));
        }
        if (!exceptionText.toString().isEmpty()) {
            throw new InvalidAttributesException(exceptionText.toString(),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
    }
}
