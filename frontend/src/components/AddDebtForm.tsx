import { useState } from "react";
import { useUser } from "../context/UserContext";

type Props = {
  layout: "compact" | "wide"; // compact = 2-column popover, wide = one row on the Debts page
  onSaved: () => void;
  autoFocus?: boolean;
};

type Kind = "loan" | "card";

const API = "http://localhost:8080";

// "" -> null, otherwise a number. Optional fields are sent as null when left blank.
const numberOrNull = (value: string) => (value.trim() === "" ? null : Number(value));

// The add-debt form, shared by the dashboard pill's popover and the Debts page.
// Loans ask for what they started at (-> % paid off); cards ask for the credit limit (-> % used).
function AddDebtForm({ layout, onSaved, autoFocus = false }: Props) {
  const { user } = useUser();
  const [kind, setKind] = useState<Kind>("loan");
  const [name, setName] = useState("");
  const [balance, setBalance] = useState("");
  const [original, setOriginal] = useState("");
  const [limit, setLimit] = useState("");
  const [apr, setApr] = useState("");
  const [minPayment, setMinPayment] = useState("");
  const [dueDay, setDueDay] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    e.stopPropagation(); // this form can live in a portal: don't let React bubble the submit upward
    if (!user) {
      setError("You've been signed out. Sign in again.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      const response = await fetch(`${API}/debts`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: user.id,
          kind,
          creditorName: name,
          balance: numberOrNull(balance),
          originalBalance: kind === "loan" ? numberOrNull(original) : null,
          creditLimit: kind === "card" ? numberOrNull(limit) : null,
          apr: numberOrNull(apr),
          minPayment: numberOrNull(minPayment),
          dueDate: numberOrNull(dueDay),
        }),
      });
      if (!response.ok) {
        setError(await response.text());
        return;
      }
      setName("");
      setBalance("");
      setOriginal("");
      setLimit("");
      setApr("");
      setMinPayment("");
      setDueDay("");
      onSaved();
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setSaving(false);
    }
  }

  const missingRequired = !name.trim() || !balance || (kind === "card" && !limit);

  return (
    <>
      <div className="qa-type debt-kind" role="group" aria-label="Kind of debt">
        <button type="button" className={kind === "loan" ? "qa-type-active" : ""} onClick={() => setKind("loan")}>
          Loan
        </button>
        <button type="button" className={kind === "card" ? "qa-type-active" : ""} onClick={() => setKind("card")}>
          Credit card
        </button>
      </div>

      <form className={`debt-form debt-form--${layout}`} onSubmit={submit}>
        <label className="debt-field debt-field-name">
          <span>Name</span>
          <input
            value={name}
            maxLength={60}
            placeholder={kind === "card" ? "Discover card" : "Car loan"}
            onChange={(e) => setName(e.target.value)}
            autoFocus={autoFocus}
          />
        </label>
        <label className="debt-field">
          <span>Balance now</span>
          <input type="number" inputMode="decimal" step="0.01" min="0" placeholder={kind === "card" ? "1200" : "9000"} value={balance} onChange={(e) => setBalance(e.target.value)} />
        </label>
        {kind === "loan" ? (
          <label className="debt-field">
            <span>Started at <em>optional</em></span>
            <input type="number" inputMode="decimal" step="0.01" min="0" placeholder="15000" value={original} onChange={(e) => setOriginal(e.target.value)} />
          </label>
        ) : (
          <label className="debt-field">
            <span>Credit limit</span>
            <input type="number" inputMode="decimal" step="0.01" min="0" placeholder="5000" value={limit} onChange={(e) => setLimit(e.target.value)} />
          </label>
        )}
        <label className="debt-field">
          <span>APR % <em>optional</em></span>
          <input type="number" inputMode="decimal" step="0.01" min="0" max="100" placeholder="22.99" value={apr} onChange={(e) => setApr(e.target.value)} />
        </label>
        <label className="debt-field">
          <span>Min payment <em>optional</em></span>
          <input type="number" inputMode="decimal" step="0.01" min="0" placeholder="150" value={minPayment} onChange={(e) => setMinPayment(e.target.value)} />
        </label>
        <label className="debt-field">
          <span>Due day <em>optional</em></span>
          <input type="number" inputMode="numeric" step="1" min="1" max="31" placeholder="15" value={dueDay} onChange={(e) => setDueDay(e.target.value)} />
        </label>
        <button type="submit" className="debt-submit" disabled={saving || missingRequired}>
          {saving ? "Saving…" : kind === "card" ? "Add card" : "Add loan"}
        </button>
      </form>
      {error && <p className="debt-error">{error}</p>}
    </>
  );
}

export default AddDebtForm;
