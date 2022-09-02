package spring.application.tree.data.messages.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.messages.models.AbstractMessageModel;

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
        final String query = "SELECT id, message, sent_at, author_id FROM messages WHERE chat_id = ?;";
        List<AbstractMessageModel> messages = new ArrayList<>();
        jdbcTemplate.query(query, resultSet -> {
            int id = resultSet.getInt("id");
            String message = resultSet.getString("message");
            Date sendingDate = resultSet.getTimestamp("sent_at");
            int authorId = resultSet.getInt("author_id");
            messages.add(new AbstractMessageModel(id, message, sendingDate, authorId, chatId));
        }, chatId);
        return messages;
    }

    public int addMessage(AbstractMessageModel abstractMessageModel) throws InvalidAttributesException {
        if (abstractMessageModel.getMessage() == null || abstractMessageModel.getMessage().isEmpty() ||
            abstractMessageModel.getAuthorId() <= 0 || abstractMessageModel.getChatId() <= 0) {
            throw new InvalidAttributesException(buildExceptionMessageForValidationOfMessageModel(abstractMessageModel),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "INSERT INTO messages(message, sent_at, author_id, chat_id) VALUES(?, now(), ?, ?) RETURNING id;";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(query, new String[] {"id"});
            preparedStatement.setString(1, abstractMessageModel.getMessage());
            preparedStatement.setInt(2, abstractMessageModel.getAuthorId());
            preparedStatement.setInt(3, abstractMessageModel.getChatId());
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

    public void deleteMessage(int messageId, int chatId) throws InvalidAttributesException {
        if (messageId <= 0 || chatId <= 0) {
            throw new InvalidAttributesException(String.format("Message ID: %s or chat ID: %s is invalid", messageId, chatId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "DELETE FROM messages WHERE id = ? AND chat_id = ?;";
        jdbcTemplate.update(query, messageId, chatId);
    }

    private String buildExceptionMessageForValidationOfMessageModel(AbstractMessageModel abstractMessageModel) {
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
        return exceptionText.toString();
    }
}
