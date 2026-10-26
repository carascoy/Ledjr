import "../styles/Dashboard.css";
import logo from "../assets/ledjr.svg";
import avatar from "../assets/profile-icon.svg";
import { Link } from "react-router-dom";
import { useEffect, useState } from "react";

function Dashboard() {

  const [scrolled, setScrolled] = useState(false);

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

 
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 40);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <div className="dash-page">
      <nav className={`nav-card ${scrolled ? "scrolled" : ""}`}>
        <Link to="/dashboard" className="nav-logo">
          <img src={logo} alt="Ledjr home" />
        </Link>
        <Link to="/transactions" className="nav-link">
          Transactions
        </Link>
        <Link to="/budget" className="nav-link">
          Budget
        </Link>
        <Link to="/debts" className="nav-link">
          Debts
        </Link>
        <Link to="/recurring" className="nav-link">
          Recurring
        </Link>
        <button type="button" className="nav-avatar">
          <img src={avatar} alt="Account menu" />
        </button>
      </nav>

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
        <button type="button" className="quick-add">
          + Add transaction
        </button>
      </header>

      <main className="dash-grid">
        <section className="pill pill-income">Income vs Expenses</section>
        <section className="pill pill-debts">Debts</section>
        <section className="pill pill-spending">Spending by Category</section>
        <section className="pill pill-recent">Recent Transactions</section>
        <section className="pill pill-upcoming">Upcoming</section>
      </main>
    </div>
  );
}

export default Dashboard;
