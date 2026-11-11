-- demo accounts: made by the "Try the demo" button, filled with fake data, deleted after 24 hours
ALTER TABLE app_user ADD COLUMN demo BOOLEAN NOT NULL DEFAULT false;

-- the cleanup job looks for old demo accounts every hour. this index only holds demo rows,
-- so it stays tiny no matter how many real users there are
CREATE INDEX idx_app_user_demo_created ON app_user (created_at) WHERE demo;
