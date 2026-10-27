import { useEffect, useState } from "react";
import { useUser } from "../context/UserContext";
import { api } from "../api/client";
import { toInputDate } from "../utils/dates";
import { amountDue, formatDayShort, isOverdue, isPartlyPaid, itemKey } from "../utils/recurring";
import type { CalendarItem } from "../types/CalendarItem";

type Props = {
  refreshKey?: number; // dashboard bumps this after quick-add
};

const DAYS_AHEAD = 14;
const LOOK_BACK = 45; // still show anything you forgot to pay (late stuff goes first)
const MAX_ROWS = 6;

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

// dashboard snippet: unpaid bills, paychecks and debt payments coming up in the next 2 weeks
function UpcomingPill({ refreshKey = 0 }: Props) {
  const { user } = useUser();
  const [items, setItems] = useState<CalendarItem[] | null>(null);

  useEffect(() => {
    if (!user) return;
    let ignore = false;
    const today = new Date();
    const from = toInputDate(new Date(today.getFullYear(), today.getMonth(), today.getDate() - LOOK_BACK));
    const to = toInputDate(new Date(today.getFullYear(), today.getMonth(), today.getDate() + DAYS_AHEAD));
    api(`/calendar?from=${from}&to=${to}`)
      .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
      .then((data: CalendarItem[]) => {
        if (!ignore) setItems(data);
      })
      .catch(() => {
        if (!ignore) setItems([]);
      });
    return () => {
      ignore = true;
    };
  }, [user, refreshKey]);

  const todayStr = toInputDate(new Date());
  // older days only count if they're still unpaid
  const unpaid = (items ?? []).filter((i) => !i.paid && (i.date >= todayStr || isOverdue(i, todayStr)));
  const lateCount = unpaid.filter((i) => isOverdue(i, todayStr)).length;

  let content;
  if (items === null) {
    content = <p className="pill-empty">Loading…</p>;
  } else if (unpaid.length === 0) {
    content = <p className="pill-empty">Nothing due in the next 2 weeks. Add bills and paychecks on the Recurring page.</p>;
  } else {
    content = (
      <ul className="upcoming-list">
        {unpaid.slice(0, MAX_ROWS).map((i) => (
          <li key={itemKey(i)} className="upcoming-row">
            <span className={`upcoming-date ${isOverdue(i, todayStr) ? "upcoming-date--late" : ""}`}>
              {i.date === todayStr ? "Today" : isOverdue(i, todayStr) ? "Late" : formatDayShort(i.date)}
            </span>
            <span className="upcoming-name">
              <i className={`cal-dot cal-dot--${i.type}`} />
              {i.name}
            </span>
            <span className="upcoming-amount">
              {i.amount == null ? "—" : `${i.type === "income" ? "+" : "−"}${currency.format(amountDue(i))}`}
              {isPartlyPaid(i) && <span className="upcoming-left"> left</span>}
            </span>
          </li>
        ))}
        {unpaid.length > MAX_ROWS && <li className="upcoming-more">+{unpaid.length - MAX_ROWS} more</li>}
      </ul>
    );
  }

  return (
    <>
      <h2 className="pill-title">Upcoming</h2>
      {lateCount > 0 && (
        <p className="upcoming-late-note">
          {lateCount} late {lateCount === 1 ? "item" : "items"} still unpaid
        </p>
      )}
      {content}
    </>
  );
}

export default UpcomingPill;
