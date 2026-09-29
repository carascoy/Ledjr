CREATE TABLE transaction(
     id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    category_id BIGINT NOT NULL REFERENCES category(id),
    debt_id BIGINT REFERENCES debt(id),
    type VARCHAR(255),
    amount NUMERIC(10,2) NOT NULL,
    txn_date DATE,
    description VARCHAR
);