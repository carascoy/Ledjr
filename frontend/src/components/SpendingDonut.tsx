import { Cell, Pie, PieChart, Tooltip } from "recharts";
import type { TooltipContentProps } from "recharts";
import type { TransactionResponse } from "../types/TransactionResponse";
import { categoryColor } from "../utils/categoryColors";

type Props = {
  transactions: TransactionResponse[] | null; // null = loading
  monthName: string;
  signedIn: boolean;
};

// pill's small so top 5 get a slice, rest go in "Other"
const MAX_SLICES = 5;
const UNCATEGORIZED_COLOR = "#A8A8A8";
const OTHER_COLOR = "#6E6E6E";
const SIZE = 136;

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

type Slice = { key: string; name: string; value: number; color: string };

// groups the month's expenses by category, biggest first, leftovers -> "Other"
function buildSlices(transactions: TransactionResponse[]): Slice[] {
  const byKey = new Map<string, Slice>();
  for (const t of transactions) {
    if (t.type !== "expense") continue; // income + debt payments aren't spending
    const key = t.categoryId == null ? "none" : String(t.categoryId);
    const existing = byKey.get(key);
    if (existing) {
      existing.value += t.amount;
    } else {
      byKey.set(key, {
        key,
        name: t.categoryName ?? "Uncategorized",
        value: t.amount,
        color: t.categoryId == null ? UNCATEGORIZED_COLOR : categoryColor(t.categoryColorSlot),
      });
    }
  }

  const sorted = [...byKey.values()].sort((a, b) => b.value - a.value);
  if (sorted.length <= MAX_SLICES + 1) return sorted; // no point folding just one

  const top = sorted.slice(0, MAX_SLICES);
  const rest = sorted.slice(MAX_SLICES).reduce((sum, s) => sum + s.value, 0);
  return [...top, { key: "other", name: "Other", value: rest, color: OTHER_COLOR }];
}

function SliceTooltip({ active, payload }: TooltipContentProps) {
  if (!active || !payload?.length) return null;
  const slice = payload[0].payload as Slice;
  return (
    <div className="io-tooltip">
      <span>
        <i className="io-line" style={{ background: slice.color }} />
        <b>{currency.format(slice.value)}</b> {slice.name}
      </span>
    </div>
  );
}

function SpendingDonut({ transactions, monthName, signedIn }: Props) {
  const title = <h2 className="pill-title">Spending by Category</h2>;

  if (!signedIn || transactions === null) {
    return (
      <>
        {title}
        <p className="pill-empty">{signedIn ? "Loading…" : "Sign in to see your spending."}</p>
      </>
    );
  }

  const slices = buildSlices(transactions);
  const total = slices.reduce((sum, s) => sum + s.value, 0);

  if (total === 0) {
    return (
      <>
        {title}
        <p className="pill-empty">No spending logged in {monthName} yet.</p>
      </>
    );
  }

  return (
    <>
      {title}
      <div className="sd-body">
        <div className="sd-donut" style={{ width: SIZE, height: SIZE }}>
          <PieChart width={SIZE} height={SIZE}>
            <Pie
              data={slices}
              dataKey="value"
              nameKey="name"
              innerRadius={46}
              outerRadius={66}
              startAngle={90}
              endAngle={-270}
              paddingAngle={slices.length > 1 ? 2 : 0}
              cornerRadius={3}
              stroke="none"
              animationDuration={800}
              animationEasing="ease-out"
            >
              {slices.map((s) => (
                <Cell key={s.key} fill={s.color} />
              ))}
            </Pie>
            <Tooltip content={SliceTooltip} />
          </PieChart>

          <div className="sd-center" aria-hidden="true">
            <span className="sd-total">{currency.format(total)}</span>
            <span className="sd-caption">spent</span>
          </div>
        </div>

        {/* legend so nobody has to eyeball thin slices */}
        <ul className="sd-legend">
          {slices.map((s) => (
            <li key={s.key} className="sd-row">
              <i className="sd-dot" style={{ background: s.color }} />
              <span className="sd-name">{s.name}</span>
              <span className="sd-amount">{currency.format(s.value)}</span>
              <span className="sd-pct">{Math.round((s.value / total) * 100)}%</span>
            </li>
          ))}
        </ul>
      </div>
    </>
  );
}

export default SpendingDonut;
