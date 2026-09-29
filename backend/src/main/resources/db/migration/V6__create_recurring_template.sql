CREATE TABLE recurring_template(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    category_id BIGINT NOT NULL REFERENCES category(id),
    name VARCHAR(255) NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    frequency VARCHAR(20) NOT NULL CHECK (frequency IN ('bi-weekly', 'weekly', 'daily', 'monthly', 'annually')),
    next_due_date DATE NOT NULL
);