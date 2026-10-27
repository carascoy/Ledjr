-- recurring bills + paychecks for the calendar page

-- 1. income or expense. paychecks repeat too, and the calendar colors them differently
ALTER TABLE recurring_template ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'expense';
ALTER TABLE recurring_template ADD CONSTRAINT recurring_template_type_check CHECK (type IN ('income', 'expense'));

-- 2. category is optional now, same as transactions (V11)
ALTER TABLE recurring_template ALTER COLUMN category_id DROP NOT NULL;

-- 3. next_due_date -> start_date. the calendar works out every occurrence from the FIRST one
--    (start + 1 month, + 2 months...), so a "next" date that keeps moving isn't needed.
--    counting from the start also stops the 31st from drifting to the 28th after February
ALTER TABLE recurring_template RENAME COLUMN next_due_date TO start_date;

-- 4. when it was added. the calendar skips due dates from before the month you added it
--    (unless you paid them), otherwise a bill with an old start date would make every
--    month before you used the app look "late"
ALTER TABLE recurring_template ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();

-- same for debts: due dates before you added the debt don't count (not shown, can't be "late",
-- and they don't soak up payments). debts you already had count from the start of this month
ALTER TABLE debt ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
UPDATE debt SET created_at = date_trunc('month', now());

-- 5. which recurring item (and which due date of it) a transaction paid.
--    this is how a calendar day knows it's been paid. deleting the recurring item keeps the
--    transactions, they just stop pointing at it
ALTER TABLE transaction ADD COLUMN recurring_template_id BIGINT REFERENCES recurring_template(id) ON DELETE SET NULL;
ALTER TABLE transaction ADD COLUMN due_date DATE;
CREATE INDEX transaction_recurring_due_idx ON transaction (recurring_template_id, due_date);
