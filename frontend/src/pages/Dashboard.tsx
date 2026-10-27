import "../styles/Dashboard.css";
import { useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import QuickAdd from "../components/QuickAdd";
import RecentTransactions from "../components/RecentTransactions";
import IncomeExpenseChart from "../components/IncomeExpenseChart";
import SpendingDonut from "../components/SpendingDonut";
import DebtsPill from "../components/DebtsPill";
import { useUser } from "../context/UserContext";
import type { TransactionResponse } from "../types/TransactionResponse";

function Dashboard() {

  const [addOpen, setAddOpen] = useState(false);
  const { user } = useUser();
  const navigate = useNavigate();

  // Clicking a pill opens its full page, unless the click was on something
  // interactive inside it (a button, a link, a form, or a popover rendered from it).
  function openPage(e: React.MouseEvent, path: string) {
    const target = e.target as HTMLElement;
    if (target.closest("button, a, input, select, textarea, [role='dialog']")) return;
    navigate(path);
  }

  // This month's transactions. null = still loading.
  const [transactions, setTransactions] = useState<TransactionResponse[] | null>(null);
  // Bumped after quick-add saves, to make the fetch below run again.
  const [refreshKey, setRefreshKey] = useState(0);

  // The month every pill shows data for. Always stored as the 1st of the month.
  const [month, setMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });

  const today = new Date();
  const isCurrentMonth =
    month.getFullYear() === today.getFullYear() &&
    month.getMonth() === today.getMonth();

  // delta = -1 for previous month, +1 for next. new Date() rolls the year over by itself.
  const shiftMonth = (delta: number) =>
    setMonth((m) => new Date(m.getFullYear(), m.getMonth() + delta, 1));

  const monthLabel = month.toLocaleDateString("en-US", {
    month: "long",
    year: "numeric",
  });
  const monthName = month.toLocaleDateString("en-US", { month: "long" });
  // "2026-10", the format GET /transactions expects
  const monthParam = `${month.getFullYear()}-${String(month.getMonth() + 1).padStart(2, "0")}`;

  // Load the selected month's transactions. Re-runs when the user, month, or refreshKey changes.
  useEffect(() => {
    if (!user) return;

    // If the month changes before this request finishes, ignore its (now stale) result.
    let ignore = false;

    fetch(`http://localhost:8080/transactions?userId=${user.id}&month=${monthParam}`)
      .then((response) => (response.ok ? response.json() : Promise.reject(response.status)))
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


  return (
    <>
      <header className="dash-header">
        <div className="month-switcher">
          <button
            type="button"
            className="month-arrow"
            onClick={() => shiftMonth(-1)}
            aria-label="Previous month"
          >
            ‹
          </button>
          {/* key makes React re-create the h1 on each change, replaying its pop animation */}
          <h1 key={monthLabel} className="month-label">
            {monthLabel}
          </h1>
          <button
            type="button"
            className="month-arrow"
            onClick={() => shiftMonth(1)}
            disabled={isCurrentMonth}
            aria-label="Next month"
          >
            ›
          </button>
        </div>
        <button
          type="button"
          className="quick-add"
          onClick={() => setAddOpen((o) => !o)}
        >
          {addOpen ? "Close" : "+ Add Transaction"}
        </button>
      </header>

      <QuickAdd
        open={addOpen}
        onClose={() => setAddOpen(false)}
        onAdded={() => setRefreshKey((k) => k + 1)}
      />

      <main className="dash-grid">
        <section className="pill pill-link pill-income" onClick={(e) => openPage(e, "/transactions")}>
          <IncomeExpenseChart
            transactions={transactions}
            monthName={monthName}
            signedIn={user !== null}
          />
        </section>
        <section className="pill pill-link pill-debts" onClick={(e) => openPage(e, "/debts")}>
          <DebtsPill refreshKey={refreshKey} />
        </section>
        <section className="pill pill-link pill-spending" onClick={(e) => openPage(e, "/budget")}>
          <SpendingDonut
            transactions={transactions}
            monthName={monthName}
            signedIn={user !== null}
          />
        </section>
        <section className="pill pill-link pill-recent" onClick={(e) => openPage(e, "/transactions")}>
          <RecentTransactions
            transactions={transactions}
            monthName={monthName}
            signedIn={user !== null}
          />
        </section>
        <section className="pill pill-link pill-upcoming" onClick={(e) => openPage(e, "/recurring")}>
          <h2 className="pill-title">Upcoming</h2>
        </section>
      </main>
    </>
  );
}

export default Dashboard;
