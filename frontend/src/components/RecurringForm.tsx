import { useEffect, useState } from "react";
import { useUser } from "../context/UserContext";
import { api } from "../api/client";
import DatePicker from "./DatePicker";
import CategoryPicker from "./CategoryPicker";
import OptionPicker from "./OptionPicker";
import { FREQUENCIES } from "../utils/recurring";
import type { CategoryResponse } from "../types/CategoryResponse";
import type { RecurringResponse } from "../types/RecurringResponse";

type Props = {
  open: boolean;
  // pass one to edit it (PUT). give the component key={editing.id} so it remounts with its values
  editing?: RecurringResponse | null;
  startDate: string; // first due date to start with, e.g. the day you clicked on the calendar
  onClose: () => void;
  onSaved: () => void;
};

// add / edit a recurring bill or paycheck. same slide-down look as quick-add
function RecurringForm({ open, editing = null, startDate, onClose, onSaved }: Props) {
  const { user } = useUser();
  const [type, setType] = useState<"expense" | "income">(editing?.type ?? "expense");
  const [name, setName] = useState(editing?.name ?? "");
  const [amount, setAmount] = useState(editing ? String(editing.amount) : "");
  // OptionPicker works with number ids, FREQUENCIES maps them to "monthly" etc
  const [frequencyId, setFrequencyId] = useState<number | null>(
    FREQUENCIES.find((f) => f.value === (editing?.frequency ?? "monthly"))?.id ?? 3
  );
  const [date, setDate] = useState(editing?.startDate ?? startDate);
  const [categoryId, setCategoryId] = useState<number | null>(editing?.categoryId ?? null);
  const [categories, setCategories] = useState<CategoryResponse[] | null>(null);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  // categories for the picker, the first time the form opens
  useEffect(() => {
    if (!open || !user || categories !== null) return;
    let ignore = false;
    api("/categories")
      .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
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

  // categories are income OR expense, so switching type clears it
  function changeType(next: "expense" | "income") {
    if (next !== type) setCategoryId(null);
    setType(next);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      const response = await api(editing ? `/recurring/${editing.id}` : "/recurring", {
        method: editing ? "PUT" : "POST",
        body: JSON.stringify({
          type,
          name: name.trim(),
          amount: Number(amount),
          frequency: FREQUENCIES.find((f) => f.id === frequencyId)?.value,
          startDate: date,
          categoryId,
        }),
      });
      if (!response.ok) {
        setError(await response.text());
        return;
      }
      onSaved();
      onClose();
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className={`collapsible ${open ? "expanded" : ""}`}>
      <div className="collapsible-inner">
        <form className="qa-form rec-form" onSubmit={handleSubmit}>
          <div className="qa-type">
            <button
              type="button"
              className={type === "expense" ? "qa-type-active" : ""}
              onClick={() => changeType("expense")}
            >
              Bill
            </button>
            <button
              type="button"
              className={type === "income" ? "qa-type-active" : ""}
              onClick={() => changeType("income")}
            >
              Income
            </button>
          </div>

          <input name="name"
            className="qa-input qa-description"
            placeholder={type === "income" ? "Paycheck" : "Rent, Spotify…"}
            value={name}
            onChange={(e) => setName(e.target.value)}
            aria-label="Name"
          />
          <input name="amount"
            className="qa-input qa-amount"
            type="number"
            inputMode="decimal"
            step="0.01"
            min="0.01"
            placeholder="0.00"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            aria-label="Amount"
          />
          <OptionPicker
            ariaLabel="Repeats"
            placeholder="Repeats"
            options={FREQUENCIES.map((f) => ({ id: f.id, label: f.label }))}
            value={frequencyId}
            onChange={setFrequencyId}
          />
          <DatePicker value={date} onChange={setDate} />
          <CategoryPicker
            categories={categories}
            kind={type}
            value={categoryId}
            onChange={setCategoryId}
            onCategoriesChanged={setCategories}
          />

          <button type="submit" className="qa-submit" disabled={saving || !name.trim() || !amount || frequencyId === null}>
            {saving ? "Saving…" : editing ? "Save" : "Add"}
          </button>
        </form>

        {error && <p className="qa-error">{error}</p>}
      </div>
    </div>
  );
}

export default RecurringForm;
