CREATE TABLE IF NOT EXISTS users(
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR UNIQUE,
    email VARCHAR UNIQUE,
    password VARCHAR,
    login_time TIMESTAMPTZ,
    logout_time TIMESTAMPTZ,
    role VARCHAR,
    status VARCHAR,
    language VARCHAR
);