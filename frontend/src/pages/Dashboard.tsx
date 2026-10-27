import "../styles/Dashboard.css";
import { useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import QuickAdd from "../components/QuickAdd";
import RecentTransactions from "../components/RecentTransactions";
import IncomeExpenseChart from "../components/IncomeExpenseChart";
import SpendingDonut from "../components/SpendingDonut";
import DebtsPill from "../components/DebtsPill";
import UpcomingPill from "../components/UpcomingPill";
import SetupBanner from "../components/SetupBanner";
import MonthSwitcher from "../components/MonthSwitcher";
import { toMonthParam } from "../utils/months";
import { useMonth } from "../context/MonthContext";
import { useUser } from "../context/UserContext";
import { api } from "../api/client";
import type { TransactionResponse } from "../types/TransactionResponse";

function Dashboard() {

  const [addOpen, setAddOpen] = useState(false);
  const { user } = useUser();
  const navigate = useNavigate();

  // clicking a pill opens its page, UNLESS you clicked something inside it
  // (button, link, form, or a popover that came from it)
  function openPage(e: React.MouseEvent, path: string) {
    const target = e.target as HTMLElement;
    if (target.closest("button, a, input, select, textarea, [role='dialog']")) return;
    navigate(path);
  }

  // this month's transactions, null = loading
  const [transactions, setTransactions] = useState<TransactionResponse[] | null>(null);
  // bump after quick-add saves so the fetch below runs again
  const [refreshKey, setRefreshKey] = useState(0);

  // the month all the pills show. always stored as the 1st
  const { month, setMonth } = useMonth();

  const monthName = month.toLocaleDateString("en-US", { month: "long" });
  const monthParam = toMonthParam(month);

  useEffect(() => {
    if (!user) return;

    // if the month changes before this finishes, ignore the old result
    let ignore = false;

    api(`/transactions?month=${monthParam}`)
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
        <MonthSwitcher month={month} onChange={setMonth} />
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

      <SetupBanner />

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
          <UpcomingPill refreshKey={refreshKey} />
        </section>
      </main>
    </>
  );
}

export default Dashboard;
