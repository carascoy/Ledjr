import { describe, expect, it } from "vitest";
import { fromInputDate, toInputDate } from "./dates";
import { toMonthParam } from "./months";

// these run on New York time (vite.config.ts), which is where the UTC bugs would show up
describe("dates use local time, not UTC", () => {
  it("9pm on Oct 22 is still the 22nd (toISOString would say the 23rd)", () => {
    const evening = new Date(2026, 9, 22, 21, 0);
    expect(toInputDate(evening)).toBe("2026-10-22");
    expect(evening.toISOString().slice(0, 10)).toBe("2026-10-23"); // the bug this avoids
  });

  it('"2026-10-22" reads back as the 22nd (new Date("2026-10-22") would be the 21st)', () => {
    expect(fromInputDate("2026-10-22").getDate()).toBe(22);
    expect(new Date("2026-10-22").getDate()).toBe(21); // the bug this avoids
  });

  it("round trips", () => {
    for (const s of ["2026-01-01", "2028-02-29", "2026-12-31"]) {
      expect(toInputDate(fromInputDate(s))).toBe(s);
    }
  });
});

describe("toMonthParam", () => {
  it("pads the month", () => {
    expect(toMonthParam(new Date(2026, 0, 1))).toBe("2026-01");
    expect(toMonthParam(new Date(2026, 9, 1))).toBe("2026-10");
  });
});
