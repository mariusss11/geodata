CREATE TABLE IF NOT EXISTS borrows (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER NOT NULL,
    map_id      INTEGER NOT NULL,
    borrow_date TIMESTAMP,
    return_date DATE,
    status      VARCHAR(50)
);
