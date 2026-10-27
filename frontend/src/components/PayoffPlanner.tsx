import { useState } from "react";
import { useUser } from "../context/UserContext";
import { formatTarget, monthsFor, monthsUntil, planFor, targetFromMonths } from "../utils/payoff";
import type { DebtResponse } from "../types/DebtResponse";

type Props = {
  debt: DebtResponse;
  onDone: () => void;
  onCancel: () => void;
};

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

// slider for "pay this off in N months" -> monthly payment + interest, then save it as a target month
function PayoffPlanner({ debt, onDone, onCancel }: Props) {
  const { user } = useUser();
  const maxMonths = debt.kind === "card" ? 60 : 120;
  // start from the saved plan if there is one, otherwise a year
  const saved = debt.payoffTarget ? monthsUntil(debt.payoffTarget) : null;
  const [months, setMonths] = useState(saved && saved >= 1 ? Math.min(saved, maxMonths) : Math.min(12, maxMonths));
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const plan = planFor(debt.balance, debt.apr, months);
  const target = targetFromMonths(months);

  // what just paying the minimum looks like, to compare against
  const minMonths = debt.minPayment ? monthsFor(debt.balance, debt.apr, debt.minPayment) : null;
  const minInterest = debt.minPayment && minMonths ? debt.minPayment * minMonths - debt.balance : null;

  async function save(payoffTarget: string | null) {
    if (!user) return;
    setSaving(true);
    setError("");
    try {
      const response = await fetch(`http://localhost:8080/debts/${debt.id}/plan`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: user.id, payoffTarget }),
      });
      if (!response.ok) {
        setError(await response.text());
        return;
      }
      onDone();
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="payoff">
      <label className="payoff-label" htmlFor={`payoff-${debt.id}`}>
        Pay off in <b>{months} {months === 1 ? "month" : "months"}</b>{" "}
        <span className="payoff-by">· by <b>{formatTarget(target)}</b></span>
      </label>
      <input
        id={`payoff-${debt.id}`}
        className="payoff-slider"
        type="range"
        min={1}
        max={maxMonths}
        value={months}
        onChange={(e) => setMonths(Number(e.target.value))}
      />

      <dl className="payoff-figures">
        <div className="payoff-monthly">
          <dt>Monthly</dt>
          <dd>{currency.format(plan.payment)}</dd>
        </div>
        <div>
          <dt>Interest</dt>
          <dd>{currency.format(plan.totalInterest)}</dd>
        </div>
        <div>
          <dt>Total</dt>
          <dd>{currency.format(plan.totalPaid)}</dd>
        </div>
      </dl>

      {debt.apr == null && <p className="payoff-note">No APR saved, so this assumes 0% interest.</p>}
      {debt.minPayment != null && plan.payment < debt.minPayment && (
        <p className="payoff-note">That's under your {currency.format(debt.minPayment)} minimum, you'd still have to pay that.</p>
      )}
      {debt.minPayment != null && debt.minPayment > 0 && (
        <p className="payoff-note">
          {minMonths === null ? (
            <>Just the {currency.format(debt.minPayment)} minimum doesn't even cover the interest.</>
          ) : (
            <>
              Just the {currency.format(debt.minPayment)} minimum: <b>{minMonths} months</b>, about{" "}
              <b>{currency.format(Math.max(0, minInterest ?? 0))}</b> interest.
            </>
          )}
        </p>
      )}

      <div className="payoff-actions">
        <button type="button" className="debt-action-save" disabled={saving} onClick={() => save(target)}>
          {saving ? "…" : "Save plan"}
        </button>
        {debt.payoffTarget && (
          <button type="button" className="debt-action-cancel" disabled={saving} onClick={() => save(null)}>
            Remove plan
          </button>
        )}
        <button type="button" className="debt-action-cancel" onClick={onCancel}>
          Cancel
        </button>
      </div>
      {error && <p className="debt-error">{error}</p>}
    </div>
  );
}

export default PayoffPlanner;
