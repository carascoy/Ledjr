import { useEffect, useState } from "react";
import { useUser } from "../context/UserContext";
import AddDebtForm from "../components/AddDebtForm";
import DebtMeter from "../components/DebtMeter";
import DebtAction from "../components/DebtAction";
import PayoffPlanner from "../components/PayoffPlanner";
import { formatTarget, monthsUntil, paymentFor } from "../utils/payoff";
import type { DebtResponse } from "../types/DebtResponse";
import type { DebtSummary } from "../types/DebtSummary";
import "../styles/DebtsPill.css";
import "../styles/DebtsPage.css";

const API = "http://localhost:8080";
const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

// 1 -> "1st", 2 -> "2nd", 3 -> "3rd", 11 -> "11th", 22 -> "22nd"
function ordinal(n: number) {
  const tens = n % 100;
  if (tens >= 11 && tens <= 13) return `${n}th`;
  return `${n}${{ 1: "st", 2: "nd", 3: "rd" }[n % 10] ?? "th"}`;
}

function percentPaid(d: DebtResponse) {
  if (d.originalBalance <= 0) return 0;
  return Math.max(0, Math.round(((d.originalBalance - d.balance) / d.originalBalance) * 100));
}

function percentUsed(d: DebtResponse) {
  if (!d.creditLimit || d.creditLimit <= 0) return 0;
  return Math.round((d.balance / d.creditLimit) * 100);
}

// the saved plan, recalculated from what's left now
function PlanLine({ debt }: { debt: DebtResponse }) {
  if (!debt.payoffTarget) return null;
  if (debt.balance <= 0) return <p className="debt-plan">Paid off, plan done.</p>;
  const left = monthsUntil(debt.payoffTarget);
  if (left < 1) {
    return <p className="debt-plan">Plan date ({formatTarget(debt.payoffTarget)}) passed, set a new one.</p>;
  }
  return (
    <p className="debt-plan">
      Plan: <b>{currency.format(paymentFor(debt.balance, debt.apr, left))}/mo</b> to be done by{" "}
      <b>{formatTarget(debt.payoffTarget)}</b>
    </p>
  );
}

function DebtsPage() {
  const { user } = useUser();
  const [debts, setDebts] = useState<DebtResponse[] | null>(null);
  const [summary, setSummary] = useState<DebtSummary | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  // which card has Pay/Charge open (one at a time)
  const [activeAction, setActiveAction] = useState<{ id: number; action: "payment" | "charge" | "plan" } | null>(null);

  // load list + totals together, both rerun after adding a debt
  useEffect(() => {
    if (!user) return;
    let ignore = false;
    Promise.all([
      fetch(`${API}/debts?userId=${user.id}`).then((r) => (r.ok ? r.json() : Promise.reject(r.status))),
      fetch(`${API}/debts/summary?userId=${user.id}`).then((r) => (r.ok ? r.json() : Promise.reject(r.status))),
    ])
      .then(([list, totals]: [DebtResponse[], DebtSummary]) => {
        if (ignore) return;
        setDebts(list);
        setSummary(totals);
      })
      .catch(() => {
        if (ignore) return;
        setDebts([]);
        setSummary(null);
      });
    return () => {
      ignore = true;
    };
  }, [user, refreshKey]);

  return (
    <>
      <header className="page-header">
        <h1 className="page-title">Debts</h1>
        {user && (
          <button type="button" className="btn-primary" onClick={() => setAddOpen((o) => !o)}>
            {addOpen ? "Close" : "+ Add debt"}
          </button>
        )}
      </header>

      <div className={`collapsible debts-add ${addOpen ? "expanded" : ""}`}>
        <div className="collapsible-inner">
          <div className="debts-add-panel">
            <AddDebtForm
              layout="wide"
              onSaved={() => {
                setAddOpen(false);
                setRefreshKey((k) => k + 1);
              }}
            />
          </div>
        </div>
      </div>

      {!user ? (
        <p className="pill-empty debts-message">Sign in to see your debts.</p>
      ) : debts === null ? (
        <p className="pill-empty debts-message">Loading…</p>
      ) : debts.length === 0 ? (
        <section className="pill debts-empty">
          <h2 className="pill-title">No debts tracked yet</h2>
          <p className="pill-empty">
            Add a loan or card with <b>+ Add debt</b>. Put in what it started at, and you'll see how
            much you've already paid off.
          </p>
        </section>
      ) : (
        <>
          {summary && (
            <section className="pill debts-overview">
              <div className="debts-overview-figure">
                <span className="debt-pct">{currency.format(summary.totalOwed)}</span>
                <span className="debt-label">owed across {summary.debtCount} {summary.debtCount === 1 ? "debt" : "debts"}</span>
              </div>
              <div className="debts-overview-kinds">
                {summary.loanCount > 0 && (
                  <div className="debts-overview-kind">
                    <span className="debts-kind-title">Loans</span>
                    <DebtMeter percent={summary.loanPercentPaid} tone="loan" label="Loans paid off" />
                    <span className="debts-kind-stats">
                      <b>{summary.loanPercentPaid}%</b> paid off · <b>{currency.format(summary.loanRemaining)}</b> left of{" "}
                      <b>{currency.format(summary.loanOriginal)}</b>
                    </span>
                  </div>
                )}
                {summary.cardCount > 0 && (
                  <div className="debts-overview-kind">
                    <span className="debts-kind-title">Credit cards</span>
                    <DebtMeter percent={summary.cardPercentUsed} tone="card" label="Credit used" />
                    <span className="debts-kind-stats">
                      <b>{summary.cardPercentUsed}%</b> of limits used · <b>{currency.format(summary.cardBalance)}</b> of{" "}
                      <b>{currency.format(summary.cardLimit)}</b>
                    </span>
                  </div>
                )}
              </div>
            </section>
          )}

          <div className="debt-cards">
            {debts.map((d, i) => {
              const isCard = d.kind === "card";
              const pct = isCard ? percentUsed(d) : percentPaid(d);
              const open = activeAction?.id === d.id ? activeAction.action : null;
              return (
                <article key={d.id} className="pill debt-card" style={{ animationDelay: `${i * 60}ms` }}>
                  <div className="debt-card-head">
                    <h2 className="pill-title">{d.creditorName}</h2>
                    <span className={`debt-kind-badge debt-kind-badge--${d.kind}`}>{isCard ? "Card" : "Loan"}</span>
                  </div>
                  <div className="debt-card-figure">
                    <span className="debt-card-balance">{currency.format(d.balance)}</span>
                    <span className="debt-label">{isCard ? "balance" : "left"}</span>
                  </div>
                  <DebtMeter
                    percent={pct}
                    size="small"
                    tone={d.kind}
                    label={isCard ? `${d.creditorName} credit used` : `${d.creditorName} paid off`}
                  />
                  <p className="debt-caption">
                    {isCard ? (
                      <><b>{pct}%</b> of <b>{currency.format(d.creditLimit ?? 0)}</b> limit used</>
                    ) : (
                      <><b>{pct}%</b> paid of <b>{currency.format(d.originalBalance)}</b></>
                    )}
                  </p>
                  <dl className="debt-meta">
                    <div>
                      <dt>APR</dt>
                      <dd>{d.apr != null ? `${d.apr}%` : "—"}</dd>
                    </div>
                    <div>
                      <dt>Minimum</dt>
                      <dd>{d.minPayment != null ? currency.format(d.minPayment) : "—"}</dd>
                    </div>
                    <div>
                      <dt>Due</dt>
                      <dd>{d.dueDate != null ? `the ${ordinal(d.dueDate)}` : "—"}</dd>
                    </div>
                  </dl>

                  <PlanLine debt={d} />

                  {open === "plan" ? (
                    <PayoffPlanner
                      debt={d}
                      onCancel={() => setActiveAction(null)}
                      onDone={() => {
                        setActiveAction(null);
                        setRefreshKey((k) => k + 1);
                      }}
                    />
                  ) : open ? (
                    <DebtAction
                      debt={d}
                      action={open}
                      onCancel={() => setActiveAction(null)}
                      onDone={() => {
                        setActiveAction(null);
                        setRefreshKey((k) => k + 1);
                      }}
                    />
                  ) : (
                    <div className="debt-card-actions">
                      <button
                        type="button"
                        className="debt-action-btn"
                        onClick={() => setActiveAction({ id: d.id, action: "payment" })}
                        disabled={d.balance <= 0}
                      >
                        Pay
                      </button>
                      {isCard && (
                        <button
                          type="button"
                          className="debt-action-btn"
                          onClick={() => setActiveAction({ id: d.id, action: "charge" })}
                        >
                          + Charge
                        </button>
                      )}
                      <button
                        type="button"
                        className="debt-action-btn"
                        onClick={() => setActiveAction({ id: d.id, action: "plan" })}
                        disabled={d.balance <= 0}
                      >
                        Plan
                      </button>
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        </>
      )}
    </>
  );
}

export default DebtsPage;
