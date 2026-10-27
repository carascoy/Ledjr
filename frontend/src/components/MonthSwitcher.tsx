type Props = {
  month: Date; // always the 1st of the month
  onChange: (month: Date) => void;
  allowFuture?: boolean; // the recurring calendar can look ahead, money pages can't
};

// ‹ October 2026 › used on the dashboard, transactions + recurring pages.
// can't go past this month unless allowFuture (the month is shared, so the calendar can leave it in the future)
function MonthSwitcher({ month, onChange, allowFuture = false }: Props) {
  const today = new Date();
  const atOrPastThisMonth = month.getTime() >= new Date(today.getFullYear(), today.getMonth(), 1).getTime();
  const label = month.toLocaleDateString("en-US", { month: "long", year: "numeric" });

  // -1 = prev month, +1 = next. new Date() handles the year rollover itself
  const shift = (delta: number) => onChange(new Date(month.getFullYear(), month.getMonth() + delta, 1));

  return (
    <div className="month-switcher">
      <button type="button" className="month-arrow" onClick={() => shift(-1)} aria-label="Previous month">
        ‹
      </button>
      {/* key forces a new h1 each change so the pop animation replays */}
      <h1 key={label} className="month-label">
        {label}
      </h1>
      <button
        type="button"
        className="month-arrow"
        onClick={() => shift(1)}
        disabled={!allowFuture && atOrPastThisMonth}
        aria-label="Next month"
      >
        ›
      </button>
    </div>
  );
}

export default MonthSwitcher;
