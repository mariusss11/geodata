CREATE TABLE IF NOT EXISTS maps (
    map_id              SERIAL PRIMARY KEY,
    name                VARCHAR(255),
    year                INTEGER,
    is_enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    availability_status VARCHAR(50),
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL
);
