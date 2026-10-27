import type { TransactionResponse } from "../types/TransactionResponse";
import { fromInputDate } from "../utils/dates";
import CategoryTag from "./CategoryTag";

type Props = {
  transactions: TransactionResponse[] | null; // null = still loading
  monthName: string; // e.g. "October", for the empty message
  signedIn: boolean;
};

const MAX_ROWS = 4;

const currency = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
});

function formatDay(txnDate: string) {
  return fromInputDate(txnDate).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
  });
}

function RecentTransactions({ transactions, monthName, signedIn }: Props) {
  let content;

  if (!signedIn) {
    content = <p className="pill-empty">Sign in to see your transactions.</p>;
  } else if (transactions === null) {
    content = <p className="pill-empty">Loading…</p>;
  } else if (transactions.length === 0) {
    content = <p className="pill-empty">Nothing logged in {monthName} yet.</p>;
  } else {
    content = (
      <ul className="txn-list">
        {transactions.slice(0, MAX_ROWS).map((t) => (
          <li key={t.id} className="txn-row">
            <span className="txn-date">{formatDay(t.txnDate)}</span>
            <span className="txn-desc">
              <span className="txn-desc-text">
                {t.description || (t.type === "income" ? "Income" : "Expense")}
              </span>
              {t.categoryName && (
                <CategoryTag name={t.categoryName} colorSlot={t.categoryColorSlot} />
              )}
            </span>
            <span className="txn-amount">
              {t.type === "income" ? "+" : "−"}
              {currency.format(t.amount)}
            </span>
          </li>
        ))}
      </ul>
    );
  }

  return (
    <>
      <h2 className="pill-title">Recent Transactions</h2>
      {content}
    </>
  );
}

export default RecentTransactions;
