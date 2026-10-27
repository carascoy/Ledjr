import type { CalendarItem } from "../types/CalendarItem";
import type { Frequency } from "../types/RecurringResponse";
import { fromInputDate, toInputDate } from "./dates";

export const FREQUENCIES: { id: number; value: Frequency; label: string }[] = [
  { id: 1, value: "weekly", label: "Weekly" },
  { id: 2, value: "biweekly", label: "Every 2 weeks" },
  { id: 3, value: "monthly", label: "Monthly" },
  { id: 4, value: "annually", label: "Yearly" },
];

export function frequencyLabel(value: Frequency) {
  return FREQUENCIES.find((f) => f.value === value)?.label ?? value;
}

// a stable key for one thing due on one day
export function itemKey(item: CalendarItem) {
  return `${item.source}-${item.sourceId}-${item.date}`;
}

// what's still owed: 0 once paid, the rest of a debt minimum if it's partly paid
export function amountDue(item: CalendarItem) {
  if (item.paid) return 0;
  return Math.max(0, (item.amount ?? 0) - (item.paidAmount ?? 0));
}

// a debt with some paid toward the minimum but not all of it yet
export function isPartlyPaid(item: CalendarItem) {
  return !item.paid && (item.paidAmount ?? 0) > 0;
}

// not paid and the day already passed
export function isOverdue(item: CalendarItem, today = toInputDate(new Date())) {
  return !item.paid && item.date < today;
}

// when you mark something paid: its due date if that's passed, otherwise today (paying ahead).
// same rule the backend uses for recurring, so debt payments land in the same month
export function paidOnDate(dueDate: string) {
  const today = toInputDate(new Date());
  return dueDate > today ? today : dueDate;
}

// "Tue, Oct 27"
export function formatDayLong(date: string) {
  return fromInputDate(date).toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" });
}

// "Oct 27"
export function formatDayShort(date: string) {
  return fromInputDate(date).toLocaleDateString("en-US", { month: "short", day: "numeric" });
}
