type Props = {
  percent: number; // 0-100 (cards can go over; the bar just stays full)
  size?: "small" | "large";
  tone?: "loan" | "card"; // loan = green "paid off", card = amber "used"
  label?: string;
};

// A progress bar: the unfilled track is a light step of the same color as the fill.
function DebtMeter({ percent, size = "large", tone = "loan", label = "Paid off" }: Props) {
  const width = Math.max(0, Math.min(100, percent));
  return (
    <div
      className={`debt-meter debt-meter--${size} debt-meter--${tone}`}
      role="meter"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.max(0, percent)}
      aria-label={label}
    >
      <div className="debt-meter-fill" style={{ width: `${width}%` }} />
    </div>
  );
}

export default DebtMeter;
