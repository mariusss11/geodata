CREATE TABLE IF NOT EXISTS users (
    user_id   SERIAL PRIMARY KEY,
    username  VARCHAR(255) UNIQUE NOT NULL,
    name      VARCHAR(255) NOT NULL,
    password  VARCHAR(255) NOT NULL,
    role      VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE
);
