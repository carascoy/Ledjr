CREATE TABLE category(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    name VARCHAR(255) NOT NULL,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('income', 'expense', 'debt')),
    budgeted_amount NUMERIC(10,2)
);