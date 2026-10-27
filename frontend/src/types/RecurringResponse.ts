// matches the backend RecurringResponse DTO (GET/POST/PUT /recurring)
export type Frequency = "weekly" | "biweekly" | "monthly" | "annually";

export interface RecurringResponse {
  id: number;
  type: "income" | "expense";
  name: string;
  amount: number;
  frequency: Frequency;
  startDate: string; // "YYYY-MM-DD", the first due date
  nextDue: string | null; // next due date from today on
  categoryId: number | null;
  categoryName: string | null;
  categoryColorSlot: number | null;
}
