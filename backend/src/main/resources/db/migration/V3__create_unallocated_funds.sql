CREATE TABLE unallocated_funds(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    balance NUMERIC(10,2)
);