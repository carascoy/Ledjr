import { useEffect, useState } from "react";
import { useUser } from "../context/UserContext";
import { toInputDate } from "../utils/dates";
import DatePicker from "./DatePicker";
import CategoryPicker from "./CategoryPicker";
import type { CategoryResponse } from "../types/CategoryResponse";

type TxnType = "expense" | "income";

type Props = {
  open: boolean;
  onClose: () => void;
  onAdded: () => void;
};

function QuickAdd({ open, onClose, onAdded }: Props) {
  const { user } = useUser();
  const today = toInputDate(new Date());

  const [type, setType] = useState<TxnType>("expense");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [date, setDate] = useState(today);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [categories, setCategories] = useState<CategoryResponse[] | null>(null);

  // Load the user's categories the first time the form opens.
  useEffect(() => {
    if (!open || !user || categories !== null) return;
    let ignore = false;
    fetch(`http://localhost:8080/categories?userId=${user.id}`)
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

  // Switching Expense/Income clears the category, since categories belong to one kind.
  function changeType(next: TxnType) {
    if (next !== type) setCategoryId(null);
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
      const response = await fetch("http://localhost:8080/transactions", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: user.id,
          type,
          amount: Number(amount),
          txnDate: date,
          description,
          categoryId,
        }),
      });

      if (response.ok) {
        setAmount("");
        setDescription("");
        setDate(today);
        setCategoryId(null);
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
          </div>

          <input
            className="qa-input qa-amount"
            type="number"
            inputMode="decimal"
            step="0.01"
            min="0.01"
            placeholder="0.00"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          <input
            className="qa-input qa-description"
            placeholder="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <CategoryPicker
            categories={categories}
            kind={type}
            value={categoryId}
            onChange={setCategoryId}
            onCategoriesChanged={setCategories}
          />
          <DatePicker value={date} onChange={setDate} max={new Date()} />

          <button
            type="submit"
            className="qa-submit"
            disabled={saving || !amount}
          >
            {saving ? "Saving…" : "Add"}
          </button>
        </form>

        {error && <p className="qa-error">{error}</p>}
      </div>
    </div>
  );
}

export default QuickAdd;
