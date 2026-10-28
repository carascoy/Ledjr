import { useEffect, useState } from "react";
import { useUser } from "../context/UserContext";
import { api } from "../api/client";
import MonthSwitcher from "../components/MonthSwitcher";
import QuickAdd from "../components/QuickAdd";
import CategoryTag from "../components/CategoryTag";
import { fromInputDate } from "../utils/dates";
import { toMonthParam } from "../utils/months";
import { useMonth } from "../context/MonthContext";
import type { TransactionResponse } from "../types/TransactionResponse";
import "../styles/Dashboard.css";
import "../styles/TransactionsPage.css";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

type Filter = "all" | "expense" | "income" | "payment";
const FILTERS: { id: Filter; label: string }[] = [
  { id: "all", label: "All" },
  { id: "expense", label: "Expenses" },
  { id: "income", label: "Income" },
  { id: "payment", label: "Payments" },
];

function formatDay(txnDate: string) {
  return fromInputDate(txnDate).toLocaleDateString("en-US", { month: "short", day: "numeric" });
}

function fallbackName(t: TransactionResponse) {
  return t.type === "income" ? "Income" : t.type === "payment" ? "Payment" : "Expense";
}

function sum(list: TransactionResponse[], type: TransactionResponse["type"]) {
  return list.filter((t) => t.type === type).reduce((total, t) => total + t.amount, 0);
}

// the full list for one month: search, filter by type, edit + delete
function TransactionsPage() {
  const { user } = useUser();
  const { month, setMonth } = useMonth();
  const [transactions, setTransactions] = useState<TransactionResponse[] | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const [filter, setFilter] = useState<Filter>("all");
  const [search, setSearch] = useState("");

  const [addOpen, setAddOpen] = useState(false);
  const [editing, setEditing] = useState<TransactionResponse | null>(null);
  // delete asks "you sure?" inline instead of a browser popup
  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [rowError, setRowError] = useState<{ id: number; message: string } | null>(null);
  // rows that got deleted on the server and are playing their exit animation
  const [removingIds, setRemovingIds] = useState<number[]>([]);

  const monthParam = toMonthParam(month);
  const monthName = month.toLocaleDateString("en-US", { month: "long" });

  useEffect(() => {
    if (!user) return;
    // if the month changes before this finishes, ignore the old result
    let ignore = false;
    api(`/transactions?month=${monthParam}`)
      .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
      .then((data: TransactionResponse[]) => {
        if (!ignore) setTransactions(data);
      })
      .catch(() => {
        if (!ignore) setTransactions([]);
      });
    return () => {
      ignore = true;
    };
  }, [user, monthParam, refreshKey]);

  function changeMonth(next: Date) {
    setMonth(next);
    setTransactions(null);
    setEditing(null);
    setConfirmingId(null);
  }

  function startEdit(t: TransactionResponse) {
    setAddOpen(false);
    setConfirmingId(null);
    setRowError(null);
    setEditing(t);
    // the form lives at the top, so bring it into view
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function closeForm() {
    setAddOpen(false);
    setEditing(null);
  }

  // take it out of the list locally once its exit animation is done (the server already deleted it)
  function dropRow(id: number) {
    setTransactions((list) => (list ? list.filter((t) => t.id !== id) : list));
    setRemovingIds((ids) => ids.filter((x) => x !== id));
  }

  async function remove(t: TransactionResponse) {
    if (!user) return;
    setDeletingId(t.id);
    setRowError(null);
    try {
      const response = await api(`/transactions/${t.id}`, { method: "DELETE" });
      if (!response.ok) {
        setRowError({ id: t.id, message: await response.text() });
        return;
      }
      if (editing?.id === t.id) setEditing(null);
      // no animation for reduced motion, so just drop it straight away
      if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        dropRow(t.id);
      } else {
        setRemovingIds((ids) => [...ids, t.id]);
      }
    } catch {
      setRowError({ id: t.id, message: "Couldn't reach the server." });
    } finally {
      setDeletingId(null);
      setConfirmingId(null);
    }
  }

  const query = search.trim().toLowerCase();
  const visible = (transactions ?? []).filter((t) => {
    if (filter !== "all" && t.type !== filter) return false;
    if (!query) return true;
    const haystack = [t.description, t.categoryName, t.debtName, fallbackName(t)].join(" ").toLowerCase();
    return haystack.includes(query);
  });

  const formOpen = addOpen || editing !== null;

  // summary numbers. the bar is all of the income split into spent / debt payments / left over
  const list = transactions ?? [];
  const income = sum(list, "income");
  const spent = sum(list, "expense");
  const paid = sum(list, "payment");
  const leftOver = income - spent - paid;
  const pct = (part: number) => Math.round((part / income) * 100);

  return (
    <>
      <header className="dash-header">
        <MonthSwitcher month={month} onChange={changeMonth} />
        {user && (
          <button
            type="button"
            className="quick-add"
            onClick={() => (formOpen ? closeForm() : setAddOpen(true))}
          >
            {formOpen ? "Close" : "+ Add Transaction"}
          </button>
        )}
      </header>

      {editing && (
        <p className="txns-editing">
          Editing <b>{editing.description || fallbackName(editing)}</b> from {formatDay(editing.txnDate)}
        </p>
      )}
      <QuickAdd
        key={editing?.id ?? "new"}
        open={formOpen}
        editing={editing}
        onClose={closeForm}
        onAdded={() => setRefreshKey((k) => k + 1)}
      />

      {!user ? (
        <p className="pill-empty txns-message">Sign in to see your transactions.</p>
      ) : (
        <>
          {transactions && transactions.length > 0 && (
            <section className="pill txns-summary">
              {/* income has no color key, it's the whole bar. the other 3 are its pieces */}
              <div className="txns-summary-figures">
                <div>
                  <span className="txns-summary-label">Income</span>
                  <span className="txns-summary-value">+{currency.format(income)}</span>
                </div>
                <div>
                  <span className="txns-summary-label">
                    <i className="txns-key txns-key--spent" />
                    Spent
                  </span>
                  <span className="txns-summary-value">−{currency.format(spent)}</span>
                </div>
                <div>
                  <span className="txns-summary-label">
                    <i className="txns-key txns-key--paid" />
                    Debt payments
                  </span>
                  <span className="txns-summary-value">−{currency.format(paid)}</span>
                </div>
                <div>
                  <span className="txns-summary-label">
                    {leftOver >= 0 && <i className="txns-key txns-key--left" />}
                    {leftOver >= 0 ? "Left over" : "Over by"}
                  </span>
                  <span className="txns-summary-value">{currency.format(Math.abs(leftOver))}</span>
                </div>
              </div>

              {income > 0 && (
                // each piece's flex-grow is its dollar amount, so the sizes are right and the gaps
                // between them don't push anything off the end. key={monthParam} replays the grow animation
                <div
                  key={monthParam}
                  className="txns-bar"
                  role="img"
                  aria-label={
                    leftOver >= 0
                      ? `Of ${monthName}'s income: ${pct(spent)}% spent, ${pct(paid)}% to debt payments, ${pct(leftOver)}% left over`
                      : `${currency.format(-leftOver)} more went out than came in`
                  }
                >
                  {spent > 0 && <span className="txns-bar-seg txns-bar-seg--spent" style={{ flexGrow: spent }} />}
                  {paid > 0 && <span className="txns-bar-seg txns-bar-seg--paid" style={{ flexGrow: paid }} />}
                  {leftOver > 0 && <span className="txns-bar-seg txns-bar-seg--left" style={{ flexGrow: leftOver }} />}
                </div>
              )}
              <p className="txns-bar-caption">
                {income > 0 && leftOver < 0 && (
                  <>
                    <b>{currency.format(-leftOver)}</b> more went out than came in ·{" "}
                  </>
                )}
                {transactions.length} {transactions.length === 1 ? "transaction" : "transactions"} logged in {monthName}
              </p>
            </section>
          )}

          <section className="pill txns-list-pill">
            <div className="txns-toolbar">
              <div className="qa-type txns-filter" role="group" aria-label="Show">
                {FILTERS.map((f) => (
                  <button
                    key={f.id}
                    type="button"
                    className={filter === f.id ? "qa-type-active" : ""}
                    onClick={() => setFilter(f.id)}
                  >
                    {f.label}
                  </button>
                ))}
              </div>
              <input
                className="qa-input txns-search"
                type="search"
                placeholder="Search description, category, card…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                aria-label="Search transactions"
              />
            </div>

            {transactions === null ? (
              <p className="pill-empty">Loading…</p>
            ) : transactions.length === 0 ? (
              <p className="pill-empty">Nothing logged in {monthName}. Add something with + Add Transaction.</p>
            ) : visible.length === 0 ? (
              <p className="pill-empty">Nothing matches that.</p>
            ) : (
              <ul className="txns-list">
                {visible.map((t) => (
                  <li
                    key={t.id}
                    className={`txns-row ${editing?.id === t.id ? "txns-row--editing" : ""} ${removingIds.includes(t.id) ? "txns-row--removing" : ""}`}
                    onAnimationEnd={(e) => {
                      if (e.animationName === "txns-row-out") dropRow(t.id);
                    }}
                  >
                    <span className="txn-date">{formatDay(t.txnDate)}</span>
                    <span className="txn-desc">
                      <span className="txn-desc-text">{t.description || fallbackName(t)}</span>
                      {t.categoryName && <CategoryTag name={t.categoryName} colorSlot={t.categoryColorSlot} />}
                      {t.debtName && (
                        <span className="txn-debt">{t.type === "payment" ? `→ ${t.debtName}` : `on ${t.debtName}`}</span>
                      )}
                    </span>
                    <span className="txn-amount">
                      {t.type === "income" ? "+" : "−"}
                      {currency.format(t.amount)}
                    </span>
                    <span className="txns-actions">
                      {confirmingId === t.id ? (
                        <>
                          <span className="txns-confirm">Delete?</span>
                          <button
                            type="button"
                            className="txns-btn txns-btn--danger"
                            disabled={deletingId === t.id}
                            onClick={() => remove(t)}
                          >
                            {deletingId === t.id ? "…" : "Yes"}
                          </button>
                          <button type="button" className="txns-btn" onClick={() => setConfirmingId(null)}>
                            No
                          </button>
                        </>
                      ) : (
                        <>
                          <button type="button" className="txns-btn" onClick={() => startEdit(t)}>
                            Edit
                          </button>
                          <button
                            type="button"
                            className="txns-btn"
                            onClick={() => {
                              setRowError(null);
                              setConfirmingId(t.id);
                            }}
                          >
                            Delete
                          </button>
                        </>
                      )}
                    </span>
                    {rowError?.id === t.id && <p className="txns-row-error">{rowError.message}</p>}
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </>
  );
}

export default TransactionsPage;
