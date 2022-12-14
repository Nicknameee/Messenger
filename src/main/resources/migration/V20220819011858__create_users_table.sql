CREATE TABLE IF NOT EXISTS users(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    username VARCHAR UNIQUE NOT NULL,
    email VARCHAR UNIQUE NOT NULL,
    password VARCHAR NOT NULL,
    login_time TIMESTAMPTZ NOT NULL,
    logout_time TIMESTAMPTZ NOT NULL,
    role VARCHAR NOT NULL,
    status VARCHAR NOT NULL,
    language VARCHAR NOT NULL,
    timezone VARCHAR NOT NULL
);