-- has this person finished (or skipped) the optional setup after sign-up?
-- new accounts start false, everyone who already had an account is past it
ALTER TABLE app_user ADD COLUMN setup_done BOOLEAN NOT NULL DEFAULT false;
UPDATE app_user SET setup_done = true;
