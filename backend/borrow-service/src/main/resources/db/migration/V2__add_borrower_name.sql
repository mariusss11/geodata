ALTER TABLE borrows
    ADD COLUMN borrower_name    VARCHAR(255),
    ADD COLUMN actual_return_date TIMESTAMP;

ALTER TABLE borrows ALTER COLUMN user_id DROP NOT NULL;
