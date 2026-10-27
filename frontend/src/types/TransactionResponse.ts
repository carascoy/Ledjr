// matches the backend TransactionResponse DTO (GET/POST /transactions)
export interface TransactionResponse {
  id: number;
  // payment = paying off a debt, lowers the balance and is NOT spending
  type: "income" | "expense" | "payment";
  amount: number;
  txnDate: string; // "YYYY-MM-DD"
  description: string | null;
  categoryId: number | null; // all 3 category fields are null if uncategorized
  categoryName: string | null;
  categoryColorSlot: number | null;
  debtId: number | null; // payment: the debt paid. expense: the card it went on
  debtName: string | null;
}
