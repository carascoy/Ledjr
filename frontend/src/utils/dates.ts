// dates as "YYYY-MM-DD" strings, what the backend's LocalDate wants.
// both of these use LOCAL time on purpose:
//  - toISOString() uses UTC, which is already "tomorrow" after 8pm in NY
//  - new Date("2026-10-22") parses as UTC midnight, which is still the 21st in NY

export function toInputDate(d: Date): string {
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${month}-${day}`;
}

export function fromInputDate(s: string): Date {
  const [year, month, day] = s.split("-").map(Number);
  return new Date(year, month - 1, day);
}
