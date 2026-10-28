import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useUser } from "../context/UserContext";
import { api } from "../api/client";
import DatePicker from "../components/DatePicker";
import OptionPicker from "../components/OptionPicker";
import AddDebtForm from "../components/AddDebtForm";
import BudgetBreakdown from "../components/BudgetBreakdown";
import { FREQUENCIES } from "../utils/recurring";
import { toInputDate, fromInputDate } from "../utils/dates";
import { toMonthParam } from "../utils/months";
import type { CategoryResponse } from "../types/CategoryResponse";
import type { DebtResponse } from "../types/DebtResponse";
import type { BudgetResponse } from "../types/BudgetResponse";
import "../styles/Dashboard.css";
import "../styles/DebtsPill.css";
import "../styles/BudgetPage.css";
import "../styles/SetupPage.css";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

type Step = "paycheck" | "bills" | "debts" | "done";
const STEPS: Step[] = ["paycheck", "bills", "debts", "done"];

// common bills to tick off. category = which starter category it goes in (if they still have it)
const BILL_PRESETS: { name: string; category: string | null }[] = [
  { name: "Rent", category: "Rent" },
  { name: "Phone", category: "Utilities" },
  { name: "Internet", category: "Utilities" },
  { name: "Electricity", category: "Utilities" },
  { name: "Car insurance", category: "Transportation" },
  { name: "Streaming", category: "Entertainment" },
  { name: "Gym", category: null },
];

type Bill = { key: string; name: string; category: string | null; amount: string; day: string; custom: boolean };

function isPositive(value: string) {
  const n = Number(value);
  return value.trim() !== "" && Number.isFinite(n) && n > 0;
}
function isDay(value: string) {
  const n = Number(value);
  return Number.isInteger(n) && n >= 1 && n <= 31;
}

/**
 * the first date to count a paycheck from. weekly/biweekly get walked back from the next payday
 * to on/before the 1st of this month, so paychecks earlier this month count in the estimate too.
 * monthly/yearly just start at the next payday (walking those back can land on a shorter month and drift)
 */
function paycheckStart(nextPayday: string, frequency: string) {
  const step = frequency === "weekly" ? 7 : frequency === "biweekly" ? 14 : 0;
  if (step === 0) return nextPayday;
  const today = new Date();
  const monthStart = new Date(today.getFullYear(), today.getMonth(), 1);
  const d = fromInputDate(nextPayday);
  while (d > monthStart) d.setDate(d.getDate() - step);
  return toInputDate(d);
}

/**
 * a monthly bill due on `day`. starts in January since every day 1-31 exists there,
 * so a bill on the 31st lands on the 30th in short months and goes back to the 31st after
 * (starting in a 30 day month would drift it to the 30th forever)
 */
function billStart(day: string) {
  return `${new Date().getFullYear()}-01-${String(Number(day)).padStart(2, "0")}`;
}

// optional setup after sign-up: paycheck -> bills -> debts -> "here's what you can budget".
// every step can be skipped, and skipping the whole thing never brings it back
function SetupPage() {
  const { user, setUser } = useUser();
  const navigate = useNavigate();
  const [step, setStep] = useState<Step>("paycheck");
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  // step 1
  const [payAmount, setPayAmount] = useState("");
  const [payFrequencyId, setPayFrequencyId] = useState<number | null>(2); // every 2 weeks, the most common
  const [payday, setPayday] = useState(toInputDate(new Date()));

  // step 2
  const [bills, setBills] = useState<Bill[]>([]);

  // step 3
  const [debts, setDebts] = useState<DebtResponse[]>([]);

  // step 4
  const [budget, setBudget] = useState<BudgetResponse | null>(null);

  const thisMonth = toMonthParam(new Date(new Date().getFullYear(), new Date().getMonth(), 1));

  useEffect(() => {
    let ignore = false;
    api("/categories")
      .then((r) => (r.ok ? r.json() : []))
      .then((data: CategoryResponse[]) => {
        if (!ignore) setCategories(data);
      })
      .catch(() => {});
    return () => {
      ignore = true;
    };
  }, []);

  // the estimate for the last step, once we get there
  useEffect(() => {
    if (step !== "done") return;
    let ignore = false;
    api(`/budget?month=${thisMonth}`)
      .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
      .then((data: BudgetResponse) => {
        if (!ignore) setBudget(data);
      })
      .catch(() => {});
    return () => {
      ignore = true;
    };
  }, [step, thisMonth]);

  function categoryId(name: string | null, kind: "income" | "expense") {
    if (!name) return null;
    return categories.find((c) => c.kind === kind && c.name.toLowerCase() === name.toLowerCase())?.id ?? null;
  }

  function goTo(next: Step) {
    setError("");
    setStep(next);
    window.scrollTo({ top: 0 });
  }

  // finished or skipped: remember it so setup never pops up again, then leave
  async function finish(to: string) {
    setBusy(true);
    try {
      await api("/me/setup-done", { method: "POST" });
    } catch {
      // not worth blocking them over, worst case the dashboard offers setup again
    }
    if (user) setUser({ ...user, setupDone: true });
    navigate(to);
  }

  async function savePaycheck() {
    setBusy(true);
    setError("");
    const frequency = FREQUENCIES.find((f) => f.id === payFrequencyId)?.value ?? "biweekly";
    try {
      const response = await api("/recurring", {
        method: "POST",
        body: JSON.stringify({
          type: "income",
          name: "Paycheck",
          amount: Number(payAmount),
          frequency,
          startDate: paycheckStart(payday, frequency),
          categoryId: categoryId("Paycheck", "income"),
        }),
      });
      if (!response.ok) {
        setError(await response.text());
        return;
      }
      goTo("bills");
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setBusy(false);
    }
  }

  function toggleBill(name: string, category: string | null) {
    setBills((list) =>
      list.some((b) => b.key === name)
        ? list.filter((b) => b.key !== name)
        : [...list, { key: name, name, category, amount: "", day: "1", custom: false }]
    );
  }

  function addCustomBill() {
    setBills((list) => [...list, { key: `custom-${Date.now()}`, name: "", category: null, amount: "", day: "1", custom: true }]);
  }

  function updateBill(key: string, change: Partial<Bill>) {
    setBills((list) => list.map((b) => (b.key === key ? { ...b, ...change } : b)));
  }

  const billsOk = bills.every((b) => b.name.trim() && isPositive(b.amount) && isDay(b.day));

  async function saveBills() {
    setBusy(true);
    setError("");
    try {
      // one at a time, and drop each one from the list once it's saved so a retry doesn't double it
      for (const b of bills) {
        const response = await api("/recurring", {
          method: "POST",
          body: JSON.stringify({
            type: "expense",
            name: b.name.trim(),
            amount: Number(b.amount),
            frequency: "monthly",
            startDate: billStart(b.day),
            categoryId: categoryId(b.category, "expense"),
          }),
        });
        if (!response.ok) {
          setError(`${b.name}: ${await response.text()}`);
          return;
        }
        setBills((list) => list.filter((x) => x.key !== b.key));
      }
      goTo("debts");
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setBusy(false);
    }
  }

  function refreshDebts() {
    api("/debts")
      .then((r) => (r.ok ? r.json() : []))
      .then((data: DebtResponse[]) => setDebts(data))
      .catch(() => {});
  }

  async function buildBudget() {
    setBusy(true);
    setError("");
    try {
      const response = await api(`/budget/fill?month=${thisMonth}`, { method: "POST" });
      if (!response.ok) {
        setError(await response.text());
        setBusy(false);
        return;
      }
      await finish("/budget");
    } catch {
      setError("Couldn't reach the server.");
      setBusy(false);
    }
  }

  const stepNumber = STEPS.indexOf(step) + 1;
  const canFill = budget?.lines.some((l) => l.budgeted == null && l.suggested != null) ?? false;

  return (
    <div className="setup">
      <header className="setup-head">
        <div>
          <h1 className="page-title setup-title">{step === "done" ? "You're set" : `Hey ${user?.firstName ?? "there"}, let's get you set up`}</h1>
          <p className="setup-sub">
            {step === "done"
              ? "Change any of this later on Recurring, Debts and Budget."
              : "A couple of quick questions so Budget can do the math for you. Skip anything you want."}
          </p>
        </div>
        {step !== "done" && (
          <button type="button" className="bud-cancel setup-skip-all" disabled={busy} onClick={() => finish("/dashboard")}>
            Skip setup
          </button>
        )}
      </header>

      <ol className="setup-progress" aria-label={`Step ${stepNumber} of ${STEPS.length}`}>
        {STEPS.map((s, i) => (
          <li key={s} className={i < stepNumber ? "setup-progress-on" : ""} />
        ))}
      </ol>

      {/* key={step} so each step pops in */}
      <section key={step} className="pill setup-card">
        {step === "paycheck" && (
          <>
            <h2 className="pill-title">How do you get paid?</h2>
            <p className="setup-help">Your take-home pay, after taxes. This becomes a recurring Paycheck you can change any time.</p>
            <div className="setup-fields">
              <label className="setup-field">
                <span>Amount</span>
                <input name="payAmount"
                  className="qa-input setup-input"
                  type="number"
                  inputMode="decimal"
                  step="0.01"
                  min="0.01"
                  placeholder="0.00"
                  value={payAmount}
                  onChange={(e) => setPayAmount(e.target.value)}
                  autoFocus
                />
              </label>
              <div className="setup-field">
                <span>How often</span>
                <OptionPicker
                  ariaLabel="How often you get paid"
                  placeholder="How often"
                  options={FREQUENCIES.map((f) => ({ id: f.id, label: f.label }))}
                  value={payFrequencyId}
                  onChange={setPayFrequencyId}
                />
              </div>
              <div className="setup-field">
                <span>Next payday</span>
                <DatePicker value={payday} onChange={setPayday} />
              </div>
            </div>
            <div className="setup-actions">
              <button type="button" className="bud-cancel" disabled={busy} onClick={() => goTo("bills")}>
                Skip this step
              </button>
              <button type="button" className="bud-save" disabled={busy || !isPositive(payAmount) || payFrequencyId === null} onClick={savePaycheck}>
                {busy ? "Saving…" : "Next"}
              </button>
            </div>
          </>
        )}

        {step === "bills" && (
          <>
            <h2 className="pill-title">What bills do you pay every month?</h2>
            <p className="setup-help">Tap the ones you have, then fill in the amount and the day it's due.</p>
            <div className="setup-chips">
              {BILL_PRESETS.map((p) => {
                const on = bills.some((b) => b.key === p.name);
                return (
                  <button
                    key={p.name}
                    type="button"
                    className={`setup-chip ${on ? "setup-chip--on" : ""}`}
                    aria-pressed={on}
                    onClick={() => toggleBill(p.name, p.category)}
                  >
                    {on ? "✓ " : "+ "}
                    {p.name}
                  </button>
                );
              })}
              <button type="button" className="setup-chip" onClick={addCustomBill}>
                + Something else
              </button>
            </div>

            {bills.length > 0 && (
              <ul className="setup-bills">
                {bills.map((b) => (
                  <li key={b.key} className="setup-bill">
                    {b.custom ? (
                      <input name="billName"
                        className="qa-input setup-input setup-bill-name"
                        placeholder="Bill name"
                        value={b.name}
                        onChange={(e) => updateBill(b.key, { name: e.target.value })}
                        aria-label="Bill name"
                        autoFocus
                      />
                    ) : (
                      <span className="setup-bill-label">{b.name}</span>
                    )}
                    <input name="billAmount"
                      className="qa-input setup-input setup-bill-amount"
                      type="number"
                      inputMode="decimal"
                      step="0.01"
                      min="0.01"
                      placeholder="0.00"
                      value={b.amount}
                      onChange={(e) => updateBill(b.key, { amount: e.target.value })}
                      aria-label={`${b.name || "Bill"} amount`}
                    />
                    <label className="setup-bill-day">
                      <span>due on the</span>
                      <input name="billDay"
                        className="qa-input setup-input"
                        type="number"
                        inputMode="numeric"
                        min="1"
                        max="31"
                        value={b.day}
                        onChange={(e) => updateBill(b.key, { day: e.target.value })}
                        aria-label={`${b.name || "Bill"} due day of the month`}
                      />
                    </label>
                    <button
                      type="button"
                      className="bud-cancel setup-bill-remove"
                      onClick={() => setBills((list) => list.filter((x) => x.key !== b.key))}
                      aria-label={`Remove ${b.name || "bill"}`}
                    >
                      ✕
                    </button>
                  </li>
                ))}
              </ul>
            )}

            <div className="setup-actions">
              <button type="button" className="bud-cancel" disabled={busy} onClick={() => goTo("debts")}>
                Skip this step
              </button>
              <button
                type="button"
                className="bud-save"
                disabled={busy || bills.length === 0 || !billsOk}
                onClick={saveBills}
              >
                {busy ? "Saving…" : "Next"}
              </button>
            </div>
          </>
        )}

        {step === "debts" && (
          <>
            <h2 className="pill-title">Any loans or credit cards?</h2>
            <p className="setup-help">Their minimum payments come out of what you can budget. Add as many as you want.</p>
            {debts.length > 0 && (
              <ul className="setup-added">
                {debts.map((d) => (
                  <li key={d.id}>
                    ✓ {d.creditorName} <b>{currency.format(d.balance)}</b>
                  </li>
                ))}
              </ul>
            )}
            <div className="setup-debt-form">
              <AddDebtForm layout="compact" onSaved={refreshDebts} />
            </div>
            <div className="setup-actions">
              {/* quiet while there's nothing added, so it doesn't fight the Add button */}
              <button
                type="button"
                className={debts.length > 0 ? "bud-save" : "bud-btn bud-btn--tall setup-push-right"}
                onClick={() => goTo("done")}
              >
                {debts.length > 0 ? "Next" : "Skip, I don't have any"}
              </button>
            </div>
          </>
        )}

        {step === "done" && (
          <>
            {budget === null ? (
              <p className="setup-help">Working it out…</p>
            ) : budget.incomeBasis === "none" ? (
              <>
                <h2 className="pill-title">Almost there</h2>
                <p className="setup-help">
                  Without a paycheck there's nothing to estimate from yet. Add it on Recurring whenever you want and Budget fills in the rest.
                </p>
              </>
            ) : (
              <>
                <p className="bud-estimate-line">
                  You can budget about <span className="bud-estimate-amount">{currency.format(budget.canBudget)}</span> this month
                </p>
                <BudgetBreakdown data={budget} />
                {canFill && (
                  <p className="setup-help">
                    We can start your budget from your bills
                    {budget.lines.some((l) => l.suggestionReason === "average") ? " and what you usually spend" : ""}. You can change any of it.
                  </p>
                )}
              </>
            )}
            <div className="setup-actions">
              <button type="button" className="bud-cancel" disabled={busy} onClick={() => finish("/dashboard")}>
                Go to dashboard
              </button>
              {canFill ? (
                <button type="button" className="bud-save" disabled={busy} onClick={buildBudget}>
                  {busy ? "Building…" : "Build my budget"}
                </button>
              ) : (
                <button type="button" className="bud-save" disabled={busy} onClick={() => finish("/budget")}>
                  Open Budget
                </button>
              )}
            </div>
          </>
        )}

        {error && <p className="bud-error">{error}</p>}
      </section>
    </div>
  );
}

export default SetupPage;
