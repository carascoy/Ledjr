import type { BudgetResponse } from "../types/BudgetResponse";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

// "$4,950.00 from 3 paychecks on Recurring − $373.00 debt minimums − $45.00 bills with no category".
// used on the budget page + the end of setup
function BudgetBreakdown({ data }: { data: BudgetResponse }) {
  const from =
    data.incomeBasis === "recurring"
      ? `from ${data.paycheckCount} ${data.paycheckCount === 1 ? "paycheck" : "paychecks"} on Recurring`
      : data.incomeBasis === "logged"
        ? "income logged this month"
        : "last month's income";
  return (
    <p className="bud-breakdown">
      <b>{currency.format(data.expectedIncome)}</b> {from}
      {data.debtMinimums > 0 && (
        <>
          {" "}
          − <b>{currency.format(data.debtMinimums)}</b> debt minimums
        </>
      )}
      {data.otherBills > 0 && (
        <>
          {" "}
          − <b>{currency.format(data.otherBills)}</b> bills with no category
        </>
      )}
    </p>
  );
}

export default BudgetBreakdown;
