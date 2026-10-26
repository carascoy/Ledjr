// Dates as "YYYY-MM-DD" strings, the format the backend's LocalDate expects.
// Both helpers work in LOCAL time on purpose:
//  - toISOString() would use UTC, which is already "tomorrow" after 8pm in New York.
//  - new Date("2026-10-22") is parsed as UTC midnight, which is still the 21st in New York.

export function toInputDate(d: Date): string {
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${month}-${day}`;
}

export function fromInputDate(s: string): Date {
  const [year, month, day] = s.split("-").map(Number);
  return new Date(year, month - 1, day);
}
