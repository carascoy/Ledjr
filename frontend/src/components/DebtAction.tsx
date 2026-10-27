import { useState } from "react";
import { useUser } from "../context/UserContext";
import { toInputDate } from "../utils/dates";
import { api } from "../api/client";
import type { DebtResponse } from "../types/DebtResponse";

type Props = {
  debt: DebtResponse;
  action: "payment" | "charge"; // payment lowers the balance, charge (cards only) raises it
  onDone: () => void;
  onCancel: () => void;
};

// little Pay/Charge form on a debt card. just posts a normal transaction with debtId,
// backend updates the balance in the same db transaction
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
      const response = await api("/transactions", {
        method: "POST",
        body: JSON.stringify({
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
