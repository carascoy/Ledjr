// Mirrors the backend's DebtSummary DTO (GET /debts/summary).
// Loans are measured as "% paid off", cards as "% of limit used".
export interface DebtSummary {
  totalOwed: number;
  debtCount: number;

  loanCount: number;
  loanOriginal: number;
  loanRemaining: number;
  loanPercentPaid: number; // 0-100

  cardCount: number;
  cardBalance: number;
  cardLimit: number;
  cardPercentUsed: number; // can exceed 100 when over the limit
}
