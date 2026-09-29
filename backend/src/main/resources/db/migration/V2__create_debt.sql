CREATE TABLE debt(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    creditor_name VARCHAR(255) NOT NULL,
    balance NUMERIC(10,2) NOT NULL,
    apr NUMERIC(5,2),
    min_payment NUMERIC(10,2),
    due_date INTEGER,
    priority_rank INTEGER
);