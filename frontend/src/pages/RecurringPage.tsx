import { useEffect, useState } from "react";
import { useUser } from "../context/UserContext";
import { useMonth } from "../context/MonthContext";
import { api } from "../api/client";
import MonthSwitcher from "../components/MonthSwitcher";
import RecurringForm from "../components/RecurringForm";
import CategoryTag from "../components/CategoryTag";
import { fromInputDate, toInputDate } from "../utils/dates";
import { amountDue, formatDayLong, formatDayShort, frequencyLabel, isOverdue, isPartlyPaid, itemKey, paidOnDate } from "../utils/recurring";
import type { CalendarItem } from "../types/CalendarItem";
import type { RecurringResponse } from "../types/RecurringResponse";
import "../styles/Dashboard.css";
import "../styles/RecurringPage.css";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
const WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MAX_CHIPS = 2; // per day, every day is the same size. the rest is "+2 more", the day panel has them all

// just this month's days. the grid starts on the right weekday with blank spaces before the 1st,
// no days from last/next month
function monthDays(month: Date) {
  const count = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  const days: string[] = [];
  for (let d = 1; d <= count; d++) {
    days.push(toInputDate(new Date(month.getFullYear(), month.getMonth(), d)));
  }
  return days;
}

// today if it's in this month, otherwise the 1st
function defaultSelected(month: Date) {
  const today = new Date();
  return today.getFullYear() === month.getFullYear() && today.getMonth() === month.getMonth()
    ? toInputDate(today)
    : toInputDate(month);
}

function statusLabel(item: CalendarItem) {
  if (item.type === "income") return item.paid ? "Received" : "Mark received";
  if (item.type === "debt") return item.paid ? "Paid" : "Pay";
  return item.paid ? "Paid" : "Mark paid";
}

// big month calendar of recurring bills, paychecks and debt due days
function RecurringPage() {
  const { user } = useUser();
  const { month, setMonth } = useMonth();
  const [items, setItems] = useState<CalendarItem[] | null>(null);
  const [recurring, setRecurring] = useState<RecurringResponse[] | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [selected, setSelected] = useState(() => defaultSelected(month));

  // add/edit form at the top
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<RecurringResponse | null>(null);
  const [formDate, setFormDate] = useState(selected);

  // the day panel: amount you're paying per item, which one is saving, errors
  const [payAmounts, setPayAmounts] = useState<Record<string, string>>({});
  const [busyKey, setBusyKey] = useState<string | null>(null);
  const [itemError, setItemError] = useState<{ key: string; message: string } | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

  const days = monthDays(month);
  const from = days[0];
  const to = days[days.length - 1];
  const leadingBlanks = month.getDay(); // 0 = the 1st is a Sunday
  const todayStr = toInputDate(new Date());

  // the calendar's items + the list of recurring things, again after any change
  useEffect(() => {
    if (!user) return;
    let ignore = false;
    Promise.all([
      api(`/calendar?from=${from}&to=${to}`).then((r) => (r.ok ? r.json() : Promise.reject(r.status))),
      api("/recurring").then((r) => (r.ok ? r.json() : Promise.reject(r.status))),
    ])
      .then(([cal, list]: [CalendarItem[], RecurringResponse[]]) => {
        if (ignore) return;
        setItems(cal);
        setRecurring(list);
      })
      .catch(() => {
        if (ignore) return;
        setItems([]);
        setRecurring([]);
      });
    return () => {
      ignore = true;
    };
  }, [user, from, to, refreshKey]);

  function changeMonth(next: Date) {
    setMonth(next);
    setSelected(defaultSelected(next));
    setItems(null);
  }

  function refresh() {
    setRefreshKey((k) => k + 1);
  }

  function openAdd(date: string) {
    setEditing(null);
    setFormDate(date);
    setFormOpen(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function openEdit(r: RecurringResponse) {
    setEditing(r);
    setFormOpen(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function closeForm() {
    setFormOpen(false);
    setEditing(null);
  }

  // mark a bill paid / paycheck received / pay a debt. all of them become real transactions
  async function markPaid(item: CalendarItem) {
    const key = itemKey(item);
    const typed = payAmounts[key];
    const amount = typed !== undefined && typed !== "" ? Number(typed) : isPartlyPaid(item) ? amountDue(item) : item.amount;
    if (!amount || amount <= 0) {
      setItemError({ key, message: "Enter an amount." });
      return;
    }
    setBusyKey(key);
    setItemError(null);
    try {
      const response =
        item.source === "recurring"
          ? await api(`/recurring/${item.sourceId}/pay`, {
              method: "POST",
              body: JSON.stringify({ dueDate: item.date, amount }),
            })
          : await api("/transactions", {
              method: "POST",
              body: JSON.stringify({
                type: "payment",
                amount,
                txnDate: paidOnDate(item.date),
                description: `Payment to ${item.name}`,
                debtId: item.sourceId,
              }),
            });
      if (!response.ok) {
        setItemError({ key, message: await response.text() });
        return;
      }
      refresh();
    } catch {
      setItemError({ key, message: "Couldn't reach the server." });
    } finally {
      setBusyKey(null);
    }
  }

  // undo = delete the transaction that paid it (the backend puts debt balances back)
  async function undoPaid(item: CalendarItem) {
    if (item.transactionId == null) return;
    const key = itemKey(item);
    setBusyKey(key);
    setItemError(null);
    try {
      const response = await api(`/transactions/${item.transactionId}`, { method: "DELETE" });
      if (!response.ok) {
        setItemError({ key, message: await response.text() });
        return;
      }
      refresh();
    } catch {
      setItemError({ key, message: "Couldn't reach the server." });
    } finally {
      setBusyKey(null);
    }
  }

  async function removeRecurring(id: number) {
    try {
      const response = await api(`/recurring/${id}`, { method: "DELETE" });
      if (response.ok) {
        if (editing?.id === id) closeForm();
        refresh();
      }
    } finally {
      setConfirmDeleteId(null);
    }
  }

  // group by day for the grid
  const byDate = new Map<string, CalendarItem[]>();
  for (const item of items ?? []) {
    const list = byDate.get(item.date) ?? [];
    list.push(item);
    byDate.set(item.date, list);
  }
  // in each day, stuff you still have to deal with goes first, paid stuff last
  for (const list of byDate.values()) {
    list.sort((a, b) => Number(a.paid) - Number(b.paid));
  }
  const selectedItems = byDate.get(selected) ?? [];

  // month totals
  const inMonth = items ?? [];
  const sum = (list: CalendarItem[]) => list.reduce((total, i) => total + (i.paid ? i.paidAmount ?? 0 : i.amount ?? 0), 0);
  const incomeItems = inMonth.filter((i) => i.type === "income");
  const billItems = inMonth.filter((i) => i.type !== "income");
  const stillToPay = billItems.reduce((total, i) => total + amountDue(i), 0);
  const paidCount = billItems.filter((i) => i.paid).length;
  const monthName = month.toLocaleDateString("en-US", { month: "long" });

  return (
    <>
      <header className="dash-header">
        <MonthSwitcher month={month} onChange={changeMonth} allowFuture />
        {user && (
          <button type="button" className="quick-add" onClick={() => (formOpen ? closeForm() : openAdd(selected))}>
            {formOpen ? "Close" : "+ Add recurring"}
          </button>
        )}
      </header>

      {editing && (
        <p className="rec-editing">
          Editing <b>{editing.name}</b>
        </p>
      )}
      <RecurringForm
        key={editing?.id ?? `new-${formDate}`}
        open={formOpen}
        editing={editing}
        startDate={formDate}
        onClose={closeForm}
        onSaved={refresh}
      />

      <section className="pill rec-summary">
        <div>
          <span className="rec-summary-label">
            <i className="cal-dot cal-dot--income" />
            Income expected
          </span>
          <span className="rec-summary-value">+{currency.format(sum(incomeItems))}</span>
        </div>
        <div>
          <span className="rec-summary-label">
            <i className="cal-dot cal-dot--expense" />
            Bills + debt payments
          </span>
          <span className="rec-summary-value">−{currency.format(sum(billItems))}</span>
        </div>
        <div>
          <span className="rec-summary-label">Still to pay in {monthName}</span>
          <span className="rec-summary-value">{currency.format(stillToPay)}</span>
        </div>
        <div>
          <span className="rec-summary-label">Paid</span>
          <span className="rec-summary-value">
            {paidCount} of {billItems.length}
          </span>
        </div>
      </section>

      <div className="rec-layout">
        <section className="pill rec-calendar" aria-label={`${monthName} calendar`}>
          <div className="cal-weekdays">
            {WEEKDAYS.map((d) => (
              <span key={d}>{d}</span>
            ))}
          </div>
          <div className="cal-grid">
            {Array.from({ length: leadingBlanks }, (_, i) => (
              <span key={`blank-${i}`} className="cal-blank" aria-hidden="true" />
            ))}
            {days.map((day) => {
              const dayItems = byDate.get(day) ?? [];
              const classes = [
                "cal-day",
                day === todayStr ? "cal-day--today" : "",
                day === selected ? "cal-day--selected" : "",
              ].join(" ");
              return (
                <button
                  key={day}
                  type="button"
                  className={classes}
                  onClick={() => setSelected(day)}
                  aria-label={`${formatDayLong(day)}, ${dayItems.length} due`}
                  aria-pressed={day === selected}
                >
                  <span className="cal-day-num">{fromInputDate(day).getDate()}</span>
                  {dayItems.slice(0, MAX_CHIPS).map((i) => (
                    <span
                      key={itemKey(i)}
                      className={`cal-chip cal-chip--${i.type} ${i.paid ? "cal-chip--paid" : ""} ${isOverdue(i, todayStr) ? "cal-chip--late" : ""}`}
                    >
                      <span className="cal-chip-name">
                        {i.paid && "✓ "}
                        {i.name}
                      </span>
                      {i.amount != null && (
                        <span className="cal-chip-amount">
                          {isPartlyPaid(i) ? `${currency.format(amountDue(i))} left` : currency.format(i.amount)}
                        </span>
                      )}
                    </span>
                  ))}
                  {dayItems.length > MAX_CHIPS && <span className="cal-more">+{dayItems.length - MAX_CHIPS} more</span>}
                </button>
              );
            })}
          </div>
        </section>

        <div className="rec-side">
          <section className="pill rec-day">
            <div className="rec-day-head">
              <h2 className="pill-title">{formatDayLong(selected)}</h2>
              <button type="button" className="rec-btn" onClick={() => openAdd(selected)}>
                + Add here
              </button>
            </div>

            {items === null ? (
              <p className="pill-empty">Loading…</p>
            ) : selectedItems.length === 0 ? (
              <p className="pill-empty">Nothing due. Use “+ Add here” for a bill or paycheck that lands on this day.</p>
            ) : (
              <ul className="rec-day-list">
                {selectedItems.map((i) => {
                  const key = itemKey(i);
                  const template = i.source === "recurring" ? recurring?.find((r) => r.id === i.sourceId) : undefined;
                  return (
                    <li key={key} className="rec-item">
                      <div className="rec-item-top">
                        <span className="rec-item-name">
                          <i className={`cal-dot cal-dot--${i.type}`} />
                          {i.name}
                        </span>
                        <span className="rec-item-amount">
                          {i.amount == null ? "—" : `${i.type === "income" ? "+" : "−"}${currency.format(i.amount)}`}
                        </span>
                      </div>
                      <div className="rec-item-meta">
                        {i.type === "debt" ? (
                          <span>Debt payment{i.amount != null && " (minimum)"}</span>
                        ) : i.categoryName ? (
                          <CategoryTag name={i.categoryName} colorSlot={i.categoryColorSlot} />
                        ) : (
                          <span>{i.type === "income" ? "Income" : "Bill"}</span>
                        )}
                        {isOverdue(i, todayStr) && <span className="rec-late">Late</span>}
                      </div>
                      {isPartlyPaid(i) && (
                        <p className="rec-partial">
                          <b>{currency.format(i.paidAmount ?? 0)}</b> paid so far, <b>{currency.format(amountDue(i))}</b> left of the minimum
                        </p>
                      )}

                      {i.paid ? (
                        <div className="rec-item-actions">
                          <span className="rec-paid">
                            ✓ {statusLabel(i)} {i.paidAmount != null && currency.format(i.paidAmount)}
                          </span>
                          <button type="button" className="rec-btn" disabled={busyKey === key} onClick={() => undoPaid(i)}>
                            Undo
                          </button>
                        </div>
                      ) : (
                        <div className="rec-item-actions">
                          <input name="paidAmount"
                            className="rec-pay-input"
                            type="number"
                            inputMode="decimal"
                            step="0.01"
                            min="0.01"
                            placeholder={i.amount != null ? String(i.amount) : "Amount"}
                            value={payAmounts[key] ?? (i.amount != null ? String(isPartlyPaid(i) ? amountDue(i) : i.amount) : "")}
                            onChange={(e) => setPayAmounts((p) => ({ ...p, [key]: e.target.value }))}
                            aria-label={`Amount for ${i.name}`}
                          />
                          <button type="button" className="rec-pay-save" disabled={busyKey === key} onClick={() => markPaid(i)}>
                            {busyKey === key ? "…" : statusLabel(i)}
                          </button>
                        </div>
                      )}
                      {template &&
                        (confirmDeleteId === template.id ? (
                          <div className="rec-confirm">
                            <span>
                              Stop <b>{template.name}</b>? Days you already paid stay in your transactions.
                            </span>
                            <span className="rec-all-actions">
                              <button type="button" className="rec-btn rec-btn--danger" onClick={() => removeRecurring(template.id)}>
                                Delete
                              </button>
                              <button type="button" className="rec-btn" onClick={() => setConfirmDeleteId(null)}>
                                Keep
                              </button>
                            </span>
                          </div>
                        ) : (
                          <span className="rec-links">
                            <button type="button" className="rec-link" onClick={() => openEdit(template)}>
                              Edit {template.name}
                            </button>
                            <button type="button" className="rec-link" onClick={() => setConfirmDeleteId(template.id)}>
                              Delete
                            </button>
                          </span>
                        ))}
                      {itemError?.key === key && <p className="rec-error">{itemError.message}</p>}
                    </li>
                  );
                })}
              </ul>
            )}
          </section>

          <section className="pill rec-all">
            <h2 className="pill-title">All recurring</h2>
            {recurring === null ? (
              <p className="pill-empty">Loading…</p>
            ) : recurring.length === 0 ? (
              <p className="pill-empty">Nothing yet. Add rent, subscriptions and paychecks with + Add recurring.</p>
            ) : (
              <ul className="rec-all-list">
                {recurring.map((r) => (
                  <li key={r.id} className="rec-all-row">
                    <div className="rec-all-info">
                      <span className="rec-item-name">
                        <i className={`cal-dot cal-dot--${r.type}`} />
                        {r.name}
                      </span>
                      <span className="rec-all-meta">
                        {frequencyLabel(r.frequency)}
                        {r.nextDue && ` · next ${formatDayShort(r.nextDue)}`}
                      </span>
                    </div>
                    <span className="rec-item-amount">
                      {r.type === "income" ? "+" : "−"}
                      {currency.format(r.amount)}
                    </span>
                    <span className="rec-all-actions">
                      {confirmDeleteId === r.id ? (
                        <>
                          <button type="button" className="rec-btn rec-btn--danger" onClick={() => removeRecurring(r.id)}>
                            Delete
                          </button>
                          <button type="button" className="rec-btn" onClick={() => setConfirmDeleteId(null)}>
                            Keep
                          </button>
                        </>
                      ) : (
                        <>
                          <button type="button" className="rec-btn" onClick={() => openEdit(r)}>
                            Edit
                          </button>
                          <button type="button" className="rec-btn" onClick={() => setConfirmDeleteId(r.id)}>
                            Delete
                          </button>
                        </>
                      )}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      </div>
    </>
  );
}

export default RecurringPage;
