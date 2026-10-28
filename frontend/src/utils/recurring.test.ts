import { afterEach, describe, expect, it, vi } from "vitest";
import { amountDue, frequencyLabel, isOverdue, isPartlyPaid, paidOnDate } from "./recurring";
import type { CalendarItem } from "../types/CalendarItem";

// a $60 card minimum due Oct 4, change whatever the test needs
function item(change: Partial<CalendarItem> = {}): CalendarItem {
  return {
    source: "debt",
    sourceId: 2,
    date: "2026-10-04",
    type: "debt",
    name: "Capital One",
    amount: 60,
    paid: false,
    transactionId: null,
    paidAmount: null,
    categoryName: null,
    categoryColorSlot: null,
    ...change,
  } as CalendarItem;
}

afterEach(() => {
  vi.useRealTimers();
});

describe("amountDue", () => {
  it("the whole amount when nothing's paid", () => {
    expect(amountDue(item())).toBe(60);
  });

  it("what's left after a partial payment", () => {
    expect(amountDue(item({ paidAmount: 20 }))).toBe(40);
  });

  it("0 once it's paid, even if they paid more", () => {
    expect(amountDue(item({ paid: true, paidAmount: 200 }))).toBe(0);
  });
});

describe("isPartlyPaid", () => {
  it("some paid but not all", () => {
    expect(isPartlyPaid(item({ paidAmount: 20 }))).toBe(true);
    expect(isPartlyPaid(item())).toBe(false);
    expect(isPartlyPaid(item({ paid: true, paidAmount: 60 }))).toBe(false);
  });
});

describe("isOverdue", () => {
  it("unpaid and the day already passed", () => {
    expect(isOverdue(item(), "2026-10-05")).toBe(true);
    expect(isOverdue(item(), "2026-10-04")).toBe(false); // due today isn't late yet
    expect(isOverdue(item({ paid: true }), "2026-10-27")).toBe(false);
  });
});

describe("paidOnDate", () => {
  it("a past due date keeps its date, a future one is paid today", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 9, 27, 21, 30)); // 9:30pm in NY, already the 28th in UTC
    expect(paidOnDate("2026-10-04")).toBe("2026-10-04");
    expect(paidOnDate("2026-11-04")).toBe("2026-10-27");
  });
});

describe("frequencyLabel", () => {
  it("reads like a person wrote it", () => {
    expect(frequencyLabel("biweekly")).toBe("Every 2 weeks");
    expect(frequencyLabel("annually")).toBe("Yearly");
  });
});
