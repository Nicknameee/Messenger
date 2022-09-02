CREATE TABLE IF NOT EXISTS users(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    username VARCHAR UNIQUE NOT NULL,
    email VARCHAR UNIQUE NOT NULL,
    password VARCHAR,
    login_time TIMESTAMPTZ,
    logout_time TIMESTAMPTZ,
    role VARCHAR,
    status VARCHAR,
    language VARCHAR
);