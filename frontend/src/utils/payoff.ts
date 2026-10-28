// payoff math for the debt plans. all pure functions, no fetching
// r = monthly rate = APR / 12 / 100

export type PayoffPlan = {
  months: number;
  payment: number; // per month
  totalPaid: number;
  totalInterest: number;
};

function monthlyRate(apr: number | null) {
  return (apr ?? 0) / 12 / 100;
}

// payment to clear `balance` in `months`:  B·r / (1 − (1+r)^−n)
// with 0% APR the formula divides by 0, so it's just B / n
export function paymentFor(balance: number, apr: number | null, months: number) {
  const r = monthlyRate(apr);
  if (r === 0) return balance / months;
  return (balance * r) / (1 - Math.pow(1 + r, -months));
}

// how long a payment takes:  n = −ln(1 − r·B/P) / ln(1+r)
// null = never, the payment doesn't even cover the monthly interest
export function monthsFor(balance: number, apr: number | null, payment: number): number | null {
  if (payment <= 0) return null;
  const r = monthlyRate(apr);
  if (r === 0) return Math.ceil(balance / payment - 1e-9);
  if (payment <= r * balance) return null;
  const exact = -Math.log(1 - (r * balance) / payment) / Math.log(1 + r);
  // floating point lands on stuff like 12.0000000001 for an exact 12, and ceil would make that 13.
  // so shave off a hair before rounding up
  return Math.ceil(exact - 1e-9);
}

export function planFor(balance: number, apr: number | null, months: number): PayoffPlan {
  const payment = paymentFor(balance, apr, months);
  const totalPaid = payment * months;
  return { months, payment, totalPaid, totalInterest: totalPaid - balance };
}

// "2027-10-01" -> months from this month, e.g. 12. can be 0 or negative if the date passed
export function monthsUntil(target: string, from = new Date()) {
  const [y, m] = target.split("-").map(Number);
  return (y - from.getFullYear()) * 12 + (m - 1 - from.getMonth());
}

// this month + n, as the 1st of that month: "2027-10-01" (what the backend stores)
export function targetFromMonths(months: number, from = new Date()) {
  const d = new Date(from.getFullYear(), from.getMonth() + months, 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-01`;
}

// "2027-10-01" -> "Oct 2027"
export function formatTarget(target: string) {
  const [y, m] = target.split("-").map(Number);
  return new Date(y, m - 1, 1).toLocaleDateString("en-US", { month: "short", year: "numeric" });
}
