// Mirrors the backend's TransactionResponse DTO (what GET/POST /transactions return).
export interface TransactionResponse {
  id: number;
  // payment = money sent to a debt: lowers its balance, and is NOT counted as spending
  type: "income" | "expense" | "payment";
  amount: number;
  txnDate: string; // "YYYY-MM-DD"
  description: string | null;
  categoryId: number | null; // the three category fields are null when uncategorized
  categoryName: string | null;
  categoryColorSlot: number | null;
  debtId: number | null; // payment: the debt paid; expense: the card it went on
  debtName: string | null;
}
