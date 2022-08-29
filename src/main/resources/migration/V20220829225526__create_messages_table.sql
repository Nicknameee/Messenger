CREATE TABLE IF NOT EXISTS messages(
   id BIGSERIAL PRIMARY KEY,
   message VARCHAR,
   sent_at TIMESTAMPTZ,
   author_id INT8 REFERENCES users(id),
   chat_id INT8 REFERENCES chats(id)
);