// Mirrors the backend's TransactionResponse DTO (what GET/POST /transactions return).
export interface TransactionResponse {
  id: number;
  type: "income" | "expense";
  amount: number;
  txnDate: string; // "YYYY-MM-DD"
  description: string | null;
}
