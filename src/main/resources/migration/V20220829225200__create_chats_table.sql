CREATE TABLE IF NOT EXISTS chats(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    title VARCHAR NOT NULL,
    description VARCHAR,
    private BOOLEAN DEFAULT FALSE,
    password VARCHAR,
    author_id INT8 REFERENCES users(id)
)