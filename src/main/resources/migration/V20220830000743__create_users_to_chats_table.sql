CREATE TABLE IF NOT EXISTS users_to_chats(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    user_id INT8 REFERENCES users(id),
    chat_id INT8 REFERENCES chats(id),
    UNIQUE(user_id, chat_id)
)