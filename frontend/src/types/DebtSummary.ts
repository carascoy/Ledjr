// matches the backend DebtSummary DTO (GET /debts/summary)
// loans = % paid off, cards = % of limit used
export interface DebtSummary {
  totalOwed: number;
  debtCount: number;

  loanCount: number;
  loanOriginal: number;
  loanRemaining: number;
  loanPercentPaid: number;

  cardCount: number;
  cardBalance: number;
  cardLimit: number;
  cardPercentUsed: number; // can go over 100 if over the limit
}
