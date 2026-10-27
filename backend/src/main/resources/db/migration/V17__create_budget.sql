-- one row per category per month: "Groceries, Oct 2026, $400".
-- month is always the 1st so "Oct 2026" only has one way to be stored
CREATE TABLE budget(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    category_id BIGINT NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    month DATE NOT NULL CHECK (EXTRACT(DAY FROM month) = 1),
    amount NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    UNIQUE (category_id, month)
);

CREATE INDEX budget_user_month_idx ON budget(user_id, month);

-- the old "one number per category forever" idea, never got used. budget table replaces it
ALTER TABLE category DROP COLUMN budgeted_amount;
