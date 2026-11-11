ALTER TABLE app_user ADD COLUMN last_seen_at TIMESTAMP;

-- one row per "Try the demo" click. demo accounts get deleted after a day, these don't
CREATE TABLE demo_start (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO demo_start (started_at) SELECT created_at FROM app_user WHERE demo;
