import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { TooltipContentProps } from "recharts";
import type { TransactionResponse } from "../types/TransactionResponse";

type Props = {
  transactions: TransactionResponse[] | null; // null = still loading
  monthName: string; // "October"
  signedIn: boolean;
};

// Identity colors, validated against the #D9D9D9 pill with the dataviz checker
// (lightness, chroma, colorblind separation, 3:1 contrast). Not good/bad colors.
const IN_COLOR = "#1F7A4D";
const OUT_COLOR = "#6250D6";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
const compact = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  notation: "compact",
  maximumFractionDigits: 1,
});

const axisText = { fontFamily: "IBM Plex Mono, monospace", fontSize: 13, fill: "#52514E" };

type Row = { name: string; value: number; color: string };

// Hover card: value leads, label follows, short line key in the bar's color.
function ChartTooltip({ active, payload }: TooltipContentProps) {
  if (!active || !payload?.length) return null;
  const row = payload[0].payload as Row;
  return (
    <div className="io-tooltip">
      <span>
        <i className="io-line" style={{ background: row.color }} />
        <b>{currency.format(row.value)}</b> {row.name}
      </span>
    </div>
  );
}

function IncomeExpenseChart({ transactions, monthName, signedIn }: Props) {
  const title = <h2 className="pill-title">Income vs Expenses</h2>;

  if (!signedIn || transactions === null) {
    return (
      <>
        {title}
        <p className="pill-empty">{signedIn ? "Loading…" : "Sign in to see your numbers."}</p>
      </>
    );
  }

  // Income in one pile, everything else (expenses, debt payments) in the other.
  const moneyIn = transactions
    .filter((t) => t.type === "income")
    .reduce((sum, t) => sum + t.amount, 0);
  const moneyOut = transactions
    .filter((t) => t.type !== "income")
    .reduce((sum, t) => sum + t.amount, 0);

  if (moneyIn === 0 && moneyOut === 0) {
    return (
      <>
        {title}
        <p className="pill-empty">Nothing logged in {monthName} yet.</p>
      </>
    );
  }

  const net = moneyIn - moneyOut;
  const data: Row[] = [
    { name: "Income", value: moneyIn, color: IN_COLOR },
    { name: "Expenses", value: moneyOut, color: OUT_COLOR },
  ];

  return (
    <>
      {title}

      {/* The headline numbers, big enough to read from across the room */}
      <div className="io-figures">
        <div>
          <span className="io-label"><i className="io-key" style={{ background: IN_COLOR }} />Income</span>
          <span className="io-amount">{currency.format(moneyIn)}</span>
        </div>
        <div>
          <span className="io-label"><i className="io-key" style={{ background: OUT_COLOR }} />Expenses</span>
          <span className="io-amount">{currency.format(moneyOut)}</span>
        </div>
      </div>

      <div className="io-chart">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }} barCategoryGap="28%">
            <CartesianGrid vertical={false} stroke="rgba(0, 0, 0, 0.08)" />
            <XAxis
              dataKey="name"
              tick={axisText}
              tickLine={false}
              axisLine={{ stroke: "rgba(0, 0, 0, 0.25)" }}
            />
            <YAxis
              tickFormatter={(v: number) => compact.format(v)}
              tick={axisText}
              tickLine={false}
              axisLine={false}
              width={64}
            />
            <Tooltip
              content={ChartTooltip}
              cursor={{ fill: "rgba(0, 0, 0, 0.05)", radius: 12 }}
            />
            <Bar
              dataKey="value"
              radius={[6, 6, 0, 0]}
              maxBarSize={140}
              animationDuration={800}
              animationEasing="ease-out"
            >
              {data.map((row) => (
                <Cell key={row.name} fill={row.color} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      <p className="io-verdict">
        Net cash flow for {monthName}:{" "}
        <b>
          {net >= 0 ? "+" : "−"}
          {currency.format(Math.abs(net))}
        </b>
        {net >= 0 ? " surplus" : " deficit"}
      </p>
    </>
  );
}

export default IncomeExpenseChart;
