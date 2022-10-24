package spring.application.tree.data.chats.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import spring.application.tree.data.chats.attributes.ChatType;
import spring.application.tree.data.chats.models.AbstractChatModel;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.users.service.UserService;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
        final String query = "SELECT chats.id, chats.title, chats.description, chats.private, chats.password, chats.author_id, chats.chat_type " +
                             "FROM chats " +
                             "INNER JOIN users_to_chats ON chats.id = users_to_chats.chat_id " +
                             "WHERE users_to_chats.user_id = ?;";
        List<AbstractChatModel> chats = new ArrayList<>();
        jdbcTemplate.query(query, resultSet -> {
            int id = resultSet.getInt("id");
            String title = resultSet.getString("title");
            String description = resultSet.getString("description");
            boolean isPrivate = resultSet.getBoolean("private");
            String password = resultSet.getString("password");
            int authorId = resultSet.getInt("author_id");
            ChatType chatType = ChatType.valueOf(resultSet.getString("chat_type"));
            chats.add(new AbstractChatModel(id, title, description, isPrivate, password, authorId, chatType));
        }, memberId);
        return chats;
    }

    public int addChat(AbstractChatModel abstractChatModel) throws InvalidAttributesException {
        validateChatModel(abstractChatModel);
        final String query = "INSERT INTO chats(title, description, private, password, author_id, chat_type) VALUES(?, ?, ?, ?, ?, ?) RETURNING id;";
        final String queryUsersToChatAssociationUpdate = "INSERT INTO users_to_chats(user_id, chat_id) VALUES(?, ?);";
        Integer userId = UserService.getIdOfCurrentlyAuthenticatedUser();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(query, new String[] {"id"});
            preparedStatement.setString(1, abstractChatModel.getTitle());
            preparedStatement.setString(2, abstractChatModel.getDescription());
            preparedStatement.setBoolean(3, abstractChatModel.isPrivate());
            preparedStatement.setString(4, abstractChatModel.getPassword());
            preparedStatement.setInt(5, abstractChatModel.getAuthorId());
            preparedStatement.setString(6, abstractChatModel.getChatType().name());
            return preparedStatement;
        }, keyHolder);
        int chatId = Objects.requireNonNull(keyHolder.getKey()).intValue();
        jdbcTemplate.update(queryUsersToChatAssociationUpdate, userId, chatId);
        return chatId;
    }

    public void updateChat(AbstractChatModel abstractChatModel) throws InvalidAttributesException {
        validateChatModel(abstractChatModel);
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
        jdbcTemplate.update(queryMessagesToChatAssociationUpdate, chatId);
        jdbcTemplate.update(queryUsersToChatAssociationUpdate, chatId);
        jdbcTemplate.update(query, chatId);
    }

    public void changeChatOwner(int chatId, int newAuthorId) throws InvalidAttributesException {
        if (chatId <= 0 || newAuthorId <= 0) {
            throw new InvalidAttributesException(String.format("Chat ID: %s or new author ID is invalid: %s", chatId, newAuthorId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "UPDATE chats SET author_id = ? WHERE id = ?;";
        jdbcTemplate.update(query, newAuthorId, chatId);
    }

    public List<Integer> getChatIdsOwnedByUser(int authorId) throws InvalidAttributesException {
        if (authorId <= 0) {
            throw new InvalidAttributesException(String.format("Author ID is invalid: %s", authorId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "SELECT id FROM chats WHERE author_id = ?;";
        List<Integer> chatIds = new ArrayList<>();
        jdbcTemplate.query(query, new Object[]{authorId}, resultSet -> {
            chatIds.add(resultSet.getInt("id")
            );
        });
        return chatIds;
    }

    public boolean checkUserPresenceInChat(int userId, int chatId) throws InvalidAttributesException {
        if (userId <= 0 || chatId <= 0) {
            throw new InvalidAttributesException(String.format("User ID: %s or chat ID: %s is invalid", userId, chatId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "SELECT COUNT(*) FROM users_to_chats WHERE user_id = ? AND chat_id = ?;";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, userId, chatId);
        return (count == null ? 0 : count) == 1;
    }

    public String getChatPassword(int chatId) throws InvalidAttributesException {
        if (chatId <= 0) {
            throw new InvalidAttributesException(String.format("Chat ID: %s is invalid", chatId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "SELECT password FROM chats WHERE id = ?";
        return jdbcTemplate.queryForObject(query, String.class, chatId);
    }

    public ChatType getChatType(int chatId) throws InvalidAttributesException {
        if (chatId <= 0) {
            throw new InvalidAttributesException(String.format("Chat ID: %s is invalid", chatId),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        final String query = "SELECT chat_type FROM chats WHERE id = ?";
        return ChatType.valueOf(jdbcTemplate.queryForObject(query, String.class, chatId));
    }

    private void validateChatModel(AbstractChatModel abstractChatModel) throws InvalidAttributesException {
        StringBuilder exceptionText = new StringBuilder();
        if (abstractChatModel.getTitle() == null || abstractChatModel.getTitle().isEmpty()) {
            exceptionText.append(String.format("Title is invalid: %s ", abstractChatModel.getTitle()));
        }
        if (abstractChatModel.isPrivate() && (abstractChatModel.getPassword() == null || abstractChatModel.getPassword().isEmpty())) {
            exceptionText.append(String.format("Password is invalid: %s ", abstractChatModel.getPassword()));
        }
        if (abstractChatModel.getAuthorId() <= 0) {
            exceptionText.append(String.format("Author ID is invalid: %s", abstractChatModel.getAuthorId()));
        }
        if (abstractChatModel.getChatType() == null) {
            exceptionText.append(String.format("Chat type is invalid: %s", abstractChatModel.getChatType()));
        }
        if (!exceptionText.toString().isEmpty()) {
            throw new InvalidAttributesException(exceptionText.toString(),
                    Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                    LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
    }
}
