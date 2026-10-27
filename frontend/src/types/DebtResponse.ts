// matches the backend DebtResponse DTO (GET/POST /debts)
export interface DebtResponse {
  id: number;
  kind: "loan" | "card";
  creditorName: string;
  balance: number;
  originalBalance: number; // loans: what it started at
  creditLimit: number | null; // cards only
  apr: number | null;
  minPayment: number | null;
  dueDate: number | null; // day of the month 1-31
}
