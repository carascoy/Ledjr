-- Starting amount of the debt, kept so the app can show "% paid off".
-- Added nullable first, backfilled from the current balance for existing rows,
-- then locked to NOT NULL.
ALTER TABLE debt
    ADD COLUMN original_balance NUMERIC(10,2);

UPDATE debt
    SET original_balance = balance
    WHERE original_balance IS NULL;

ALTER TABLE debt
    ALTER COLUMN original_balance SET NOT NULL;
