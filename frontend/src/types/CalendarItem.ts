// matches the backend CalendarItem DTO (GET /calendar?from=&to=)
export interface CalendarItem {
  source: "recurring" | "debt";
  sourceId: number; // recurring item id or debt id
  date: string; // "YYYY-MM-DD", the due date
  type: "income" | "expense" | "debt";
  name: string;
  amount: number | null; // a debt's minimum can be missing
  paid: boolean;
  transactionId: number | null; // what paid it, null if unpaid
  paidAmount: number | null;
  categoryName: string | null;
  categoryColorSlot: number | null;
}
