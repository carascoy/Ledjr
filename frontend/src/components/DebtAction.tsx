import { useState } from "react";
import { useUser } from "../context/UserContext";
import { toInputDate } from "../utils/dates";
import type { DebtResponse } from "../types/DebtResponse";

type Props = {
  debt: DebtResponse;
  action: "payment" | "charge"; // payment lowers the balance; charge (cards) raises it
  onDone: () => void;
  onCancel: () => void;
};

// The small inline form on a debt card. It posts a normal transaction with debtId set,
// and the backend updates the debt's balance in the same database transaction.
function DebtAction({ debt, action, onDone, onCancel }: Props) {
  const { user } = useUser();
  const [amount, setAmount] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!user) return;
    setSaving(true);
    setError("");
    try {
      const response = await fetch("http://localhost:8080/transactions", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: user.id,
          type: action === "payment" ? "payment" : "expense",
          amount: Number(amount),
          txnDate: toInputDate(new Date()),
          description: action === "payment" ? `Payment to ${debt.creditorName}` : `Charge on ${debt.creditorName}`,
          debtId: debt.id,
        }),
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
    <form className="debt-action" onSubmit={submit}>
      <input
        className="debt-action-input"
        type="number"
        inputMode="decimal"
        step="0.01"
        min="0.01"
        placeholder={action === "payment" ? "Amount paid" : "Amount charged"}
        value={amount}
        onChange={(e) => setAmount(e.target.value)}
        autoFocus
      />
      <button type="submit" className="debt-action-save" disabled={saving || !amount}>
        {saving ? "…" : action === "payment" ? "Pay" : "Charge"}
      </button>
      <button type="button" className="debt-action-cancel" onClick={onCancel}>
        Cancel
      </button>
      {error && <p className="debt-error debt-action-error">{error}</p>}
    </form>
  );
}

export default DebtAction;
