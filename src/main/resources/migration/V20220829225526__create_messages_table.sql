CREATE TABLE IF NOT EXISTS messages(
   id BIGSERIAL PRIMARY KEY NOT NULL,
   message VARCHAR,
   sent_at TIMESTAMPTZ,
   author_id INT8 REFERENCES users(id),
   chat_id INT8 REFERENCES chats(id)
);