import { useEffect, useState } from "react";
import { useUser } from "../context/UserContext";
import { api } from "../api/client";
import { toInputDate } from "../utils/dates";
import DatePicker from "./DatePicker";
import CategoryPicker from "./CategoryPicker";
import OptionPicker from "./OptionPicker";
import type { CategoryResponse } from "../types/CategoryResponse";
import type { DebtResponse } from "../types/DebtResponse";
import type { TransactionResponse } from "../types/TransactionResponse";

// payment = paying off a debt (lowers the balance, NOT spending)
type TxnType = "expense" | "income" | "payment";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

type Props = {
  open: boolean;
  onClose: () => void;
  onAdded: () => void;
  // pass a transaction to edit it instead (saves with PUT). give the component
  // key={editing.id} so it remounts and picks up the new starting values
  editing?: TransactionResponse | null;
};

function QuickAdd({ open, onClose, onAdded, editing = null }: Props) {
  const { user } = useUser();
  const today = toInputDate(new Date());

  const [type, setType] = useState<TxnType>(editing?.type ?? "expense");
  const [amount, setAmount] = useState(editing ? String(editing.amount) : "");
  const [description, setDescription] = useState(editing?.description ?? "");
  const [date, setDate] = useState(editing?.txnDate ?? today);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [categoryId, setCategoryId] = useState<number | null>(editing?.categoryId ?? null);
  const [categories, setCategories] = useState<CategoryResponse[] | null>(null);
  // expense: which card (null = bank/cash). payment: which debt
  const [debtId, setDebtId] = useState<number | null>(editing?.debtId ?? null);
  const [debts, setDebts] = useState<DebtResponse[]>([]);

  useEffect(() => {
    if (!open || !user || categories !== null) return;
    let ignore = false;
    api("/categories")
      .then((response) => (response.ok ? response.json() : Promise.reject(response.status)))
      .then((data: CategoryResponse[]) => {
        if (!ignore) setCategories(data);
      })
      .catch(() => {
        if (!ignore) setCategories([]);
      });
    return () => {
      ignore = true;
    };
  }, [open, user, categories]);

  // refetch debts every time it opens so the balances are up to date
  useEffect(() => {
    if (!open || !user) return;
    let ignore = false;
    api("/debts")
      .then((response) => (response.ok ? response.json() : Promise.reject(response.status)))
      .then((data: DebtResponse[]) => {
        if (!ignore) setDebts(data);
      })
      .catch(() => {
        if (!ignore) setDebts([]);
      });
    return () => {
      ignore = true;
    };
  }, [open, user]);

  const cardOptions = debts
    .filter((d) => d.kind === "card")
    .map((d) => ({ id: d.id, label: d.creditorName, hint: currency.format(d.balance) }));
  const debtOptions = debts.map((d) => ({ id: d.id, label: d.creditorName, hint: currency.format(d.balance) }));

  // switching type clears category + debt bc they only fit one type
  function changeType(next: TxnType) {
    if (next !== type) {
      setCategoryId(null);
      setDebtId(null);
    }
    setType(next);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!user) {
      setError("You've been signed out. Sign in again.");
      return;
    }

    setSaving(true);
    setError("");
    try {
      const path = editing ? `/transactions/${editing.id}` : "/transactions";
      const response = await api(path, {
        method: editing ? "PUT" : "POST",
        body: JSON.stringify({
          type,
          amount: Number(amount),
          txnDate: date,
          description,
          categoryId: type === "payment" ? null : categoryId,
          debtId: type === "income" ? null : debtId,
        }),
      });

      if (response.ok) {
        setAmount("");
        setDescription("");
        setDate(today);
        setCategoryId(null);
        setDebtId(null);
        onAdded();
        onClose();
      } else {
        setError(await response.text());
      }
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className={`collapsible ${open ? "expanded" : ""}`}>
      <div className="collapsible-inner">
        <form className="qa-form" onSubmit={handleSubmit}>
          <div className="qa-type">
            <button
              type="button"
              className={type === "expense" ? "qa-type-active" : ""}
              onClick={() => changeType("expense")}
            >
              Expense
            </button>
            <button
              type="button"
              className={type === "income" ? "qa-type-active" : ""}
              onClick={() => changeType("income")}
            >
              Income
            </button>
            <button
              type="button"
              className={type === "payment" ? "qa-type-active" : ""}
              onClick={() => changeType("payment")}
            >
              Payment
            </button>
          </div>

          <input name="amount"
            className="qa-input qa-amount"
            type="number"
            inputMode="decimal"
            step="0.01"
            min="0.01"
            placeholder="0.00"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          <input name="description"
            className="qa-input qa-description"
            placeholder={type === "payment" ? "Note (optional)" : "Description"}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          {type === "payment" ? (
            <OptionPicker
              ariaLabel="Debt being paid"
              placeholder="Which debt?"
              options={debtOptions}
              value={debtId}
              onChange={setDebtId}
              emptyMessage="No debts yet. Add one on the Debts page."
            />
          ) : (
            <CategoryPicker
              categories={categories}
              kind={type}
              value={categoryId}
              onChange={setCategoryId}
              onCategoriesChanged={setCategories}
            />
          )}
          {type === "expense" && cardOptions.length > 0 && (
            <OptionPicker
              ariaLabel="Paid with"
              placeholder="Paid with"
              noneLabel="Bank / cash"
              options={cardOptions}
              value={debtId}
              onChange={setDebtId}
            />
          )}
          <DatePicker value={date} onChange={setDate} max={new Date()} />

          <button
            type="submit"
            className="qa-submit"
            disabled={saving || !amount || (type === "payment" && debtId === null)}
          >
            {saving ? "Saving…" : editing ? "Save" : "Add"}
          </button>
        </form>

        {error && <p className="qa-error">{error}</p>}
      </div>
    </div>
  );
}

export default QuickAdd;
