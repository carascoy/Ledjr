-- 1. Two kinds of debt, measured differently:
--    loan = installment debt (car, student, personal): has a starting amount, shown as "% paid off"
--    card = revolving debt (credit/store card): has a credit limit, shown as "% used" (utilization)
ALTER TABLE debt
    ADD COLUMN kind VARCHAR(10) NOT NULL DEFAULT 'loan',
    ADD COLUMN credit_limit NUMERIC(10,2);

ALTER TABLE debt
    ADD CONSTRAINT debt_kind_check CHECK (kind IN ('loan', 'card'));

-- A card is meaningless without its limit (utilization = balance / limit).
ALTER TABLE debt
    ADD CONSTRAINT debt_card_has_limit CHECK (kind <> 'card' OR (credit_limit IS NOT NULL AND credit_limit > 0));

-- 2. Transaction types. 'payment' = money sent to a debt: it lowers that debt's balance and is
--    NOT spending (otherwise buying $100 of groceries on a card and then paying the card $100
--    would count as $200 spent).
ALTER TABLE transaction
    ADD CONSTRAINT transaction_type_check CHECK (type IN ('income', 'expense', 'payment'));

-- A payment always says which debt it paid.
ALTER TABLE transaction
    ADD CONSTRAINT transaction_payment_has_debt CHECK (type <> 'payment' OR debt_id IS NOT NULL);
