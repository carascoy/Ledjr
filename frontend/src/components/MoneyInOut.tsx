import type { TransactionResponse } from "../types/TransactionResponse";

type Props = {
  transactions: TransactionResponse[] | null; // null = still loading
  monthName: string; // "October"
  signedIn: boolean;
};

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

// The selected month's money in vs money out: two big numbers, a bar each, and a verdict.
function MoneyInOut({ transactions, monthName, signedIn }: Props) {
  const title = <h2 className="pill-title">Income vs Expenses</h2>;

  if (!signedIn || transactions === null) {
    return (
      <>
        {title}
        <p className="pill-empty">{signedIn ? "Loading…" : "Sign in to see your numbers."}</p>
      </>
    );
  }

  // Add up the month: income in one pile, everything else (expenses, debt payments) in the other.
  const moneyIn = transactions
    .filter((t) => t.type === "income")
    .reduce((sum, t) => sum + t.amount, 0);
  const moneyOut = transactions
    .filter((t) => t.type !== "income")
    .reduce((sum, t) => sum + t.amount, 0);

  if (moneyIn === 0 && moneyOut === 0) {
    return (
      <>
        {title}
        <p className="pill-empty">Nothing logged in {monthName} yet.</p>
      </>
    );
  }

  // The bigger of the two fills the full width; the other is drawn to scale against it.
  const biggest = Math.max(moneyIn, moneyOut);
  const width = (value: number) => `${(value / biggest) * 100}%`;
  const net = moneyIn - moneyOut;

  return (
    <>
      {title}
      <div className="io-body">
        <div className="io-row">
          <div className="io-figure">
            <span className="io-label">Money in</span>
            <span className="io-amount">{currency.format(moneyIn)}</span>
          </div>
          <div className="io-bar io-in" style={{ width: width(moneyIn) }} />
        </div>

        <div className="io-row">
          <div className="io-figure">
            <span className="io-label">Money out</span>
            <span className="io-amount">{currency.format(moneyOut)}</span>
          </div>
          <div className="io-bar io-out" style={{ width: width(moneyOut) }} />
        </div>

        <p className="io-verdict">
          {net >= 0 ? (
            <>You kept <b>{currency.format(net)}</b> in {monthName}.</>
          ) : (
            <>You spent <b>{currency.format(-net)}</b> more than you made in {monthName}.</>
          )}
        </p>
      </div>
    </>
  );
}

export default MoneyInOut;
