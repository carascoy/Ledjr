-- forgot password links. the link has a random code in it, only its SHA-256 hash is stored here,
-- so someone who reads this table still can't use a link. each one works once and expires.
-- deleting the user deletes their links
CREATE TABLE password_reset_token(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX password_reset_token_user_idx ON password_reset_token(user_id);
