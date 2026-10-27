type Props = {
  month: Date; // always the 1st of the month
  onChange: (month: Date) => void;
};

// ‹ October 2026 › used on the dashboard + transactions page. can't go past this month
function MonthSwitcher({ month, onChange }: Props) {
  const today = new Date();
  const isCurrentMonth = month.getFullYear() === today.getFullYear() && month.getMonth() === today.getMonth();
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
        disabled={isCurrentMonth}
        aria-label="Next month"
      >
        ›
      </button>
    </div>
  );
}

export default MonthSwitcher;
