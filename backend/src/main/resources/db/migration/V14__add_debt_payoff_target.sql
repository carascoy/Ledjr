-- the month you want a debt paid off by (stored as the 1st of that month).
-- null = no plan. storing a date instead of "12 months" so it doesn't drift,
-- the monthly payment just gets recalculated from whatever's left
ALTER TABLE debt ADD COLUMN payoff_target DATE;
