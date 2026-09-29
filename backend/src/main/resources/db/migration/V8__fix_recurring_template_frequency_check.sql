-- V6's CHECK constraint used 'bi-weekly' (with a hyphen), but Java enum
-- names can't contain hyphens, so the entity's Frequency.biweekly enum
-- value can never match it. Drop whatever the auto-generated constraint
-- name actually is (looked up dynamically, since Postgres names an inline
-- CHECK constraint automatically and we never gave it one ourselves),
-- then re-add it with 'biweekly' instead.

DO $$
DECLARE
    existing_constraint text;
BEGIN
    SELECT con.conname INTO existing_constraint
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    WHERE rel.relname = 'recurring_template'
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) LIKE '%frequency%';

    IF existing_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE recurring_template DROP CONSTRAINT %I', existing_constraint);
    END IF;
END $$;

ALTER TABLE recurring_template
    ADD CONSTRAINT recurring_template_frequency_check
    CHECK (frequency IN ('daily', 'weekly', 'biweekly', 'monthly', 'annually'));
