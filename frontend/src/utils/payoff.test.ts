import { describe, expect, it } from "vitest";
import { formatTarget, monthsFor, monthsUntil, paymentFor, planFor, targetFromMonths } from "./payoff";

describe("paymentFor", () => {
  it("matches the loan formula: $10,000 at 12% over 12 months is $888.49/mo", () => {
    expect(paymentFor(10000, 12, 12)).toBeCloseTo(888.49, 2);
  });

  it("0% APR is just the balance split evenly (the formula would divide by 0)", () => {
    expect(paymentFor(1200, 0, 12)).toBe(100);
    expect(paymentFor(1200, null, 12)).toBe(100);
  });
});

describe("monthsFor", () => {
  it("is the reverse of paymentFor", () => {
    expect(monthsFor(10000, 12, paymentFor(10000, 12, 12))).toBe(12);
    expect(monthsFor(3579, 28.99, paymentFor(3579, 28.99, 24))).toBe(24);
  });

  it("rounds up, a last partial month is still a month", () => {
    expect(monthsFor(1000, null, 300)).toBe(4);
  });

  it("is null when the payment doesn't even cover the interest", () => {
    // $10,000 at 24% = $200/mo of interest
    expect(monthsFor(10000, 24, 200)).toBeNull();
    expect(monthsFor(10000, 24, 150)).toBeNull();
    expect(monthsFor(10000, 24, 0)).toBeNull();
  });
});

describe("planFor", () => {
  it("adds up the total paid and the interest", () => {
    const plan = planFor(10000, 12, 12);
    expect(plan.totalPaid).toBeCloseTo(10661.85, 1);
    expect(plan.totalInterest).toBeCloseTo(661.85, 1);
  });

  it("no APR, no interest", () => {
    expect(planFor(5000, null, 10).totalInterest).toBeCloseTo(0, 6);
  });
});

describe("target months", () => {
  const oct27 = new Date(2026, 9, 27);

  it("12 months from October 2026 is October 2027, stored as the 1st", () => {
    expect(targetFromMonths(12, oct27)).toBe("2027-10-01");
  });

  it("goes over the new year", () => {
    expect(targetFromMonths(3, new Date(2026, 10, 15))).toBe("2027-02-01");
  });

  it("monthsUntil undoes targetFromMonths, and goes negative once the date passes", () => {
    expect(monthsUntil("2027-10-01", oct27)).toBe(12);
    expect(monthsUntil("2026-10-01", oct27)).toBe(0);
    expect(monthsUntil("2026-08-01", oct27)).toBe(-2);
  });

  it("formats like Oct 2027", () => {
    expect(formatTarget("2027-10-01")).toBe("Oct 2027");
  });
});
