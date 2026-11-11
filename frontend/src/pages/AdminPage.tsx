import { useEffect, useState } from "react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { TooltipContentProps } from "recharts";
import { api } from "../api/client";
import type { AdminStats } from "../types/AdminStats";
import { fromInputDate } from "../utils/dates";
import NotFound from "./NotFound";
import "../styles/Dashboard.css";
import "../styles/AdminPage.css";

// same pair as Income vs Expenses, checked on the gray pill
const SIGNUP_COLOR = "#1F7A4D";
const DEMO_COLOR = "#D98F2B";
const axisText = { fontFamily: "IBM Plex Mono, monospace", fontSize: 12, fill: "#52514E" };

function shortDay(day: string) {
  return fromInputDate(day).toLocaleDateString("en-US", { month: "short", day: "numeric" });
}

function when(timestamp: string | null) {
  if (!timestamp) return "—";
  const d = new Date(timestamp);
  const today = new Date();
  if (d.toDateString() === today.toDateString()) {
    return "Today " + d.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" });
  }
  return d.toLocaleDateString("en-US", { month: "short", day: "numeric" });
}

function DayTooltip({ active, payload }: TooltipContentProps) {
  if (!active || !payload?.length) return null;
  const row = payload[0].payload as AdminStats["days"][number];
  return (
    <div className="io-tooltip adm-tooltip">
      <span className="adm-tooltip-day">{shortDay(row.day)}</span>
      <span><i className="io-line" style={{ background: SIGNUP_COLOR }} /><b>{row.signups}</b> sign-ups</span>
      <span><i className="io-line" style={{ background: DEMO_COLOR }} /><b>{row.demos}</b> demos</span>
    </div>
  );
}

function Tile({ label, value, sub }: { label: string; value: number | string; sub: string }) {
  return (
    <div className="pill adm-tile">
      <span className="adm-tile-label">{label}</span>
      <span className="adm-tile-value">{value}</span>
      <span className="adm-tile-sub">{sub}</span>
    </div>
  );
}

function AdminPage() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [status, setStatus] = useState<"loading" | "ok" | "hidden" | "error">("loading");
  const [loadedAt, setLoadedAt] = useState<Date | null>(null);
  const [refreshes, setRefreshes] = useState(0);

  useEffect(() => {
    let ignore = false;
    api("/admin/stats")
      .then(async (response) => {
        if (ignore) return;
        if (response.status === 404) {
          setStatus("hidden");
          return;
        }
        if (!response.ok) throw new Error(String(response.status));
        const data: AdminStats = await response.json();
        if (ignore) return;
        setStats(data);
        setLoadedAt(new Date());
        setStatus("ok");
      })
      .catch(() => {
        if (!ignore) setStatus("error");
      });
    return () => {
      ignore = true;
    };
  }, [refreshes]);

  if (status === "hidden") return <NotFound />;

  return (
    <div className="adm-page">
      <header className="page-header">
        <h1 className="page-title">Admin</h1>
        <div className="adm-refresh">
          {loadedAt && <span>Updated {loadedAt.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" })}</span>}
          <button type="button" className="bud-btn" onClick={() => setRefreshes((n) => n + 1)}>Refresh</button>
        </div>
      </header>

      {status === "loading" && <p className="pill-empty">Loading…</p>}
      {status === "error" && <p className="pill-empty">Couldn't load the stats. Try Refresh.</p>}

      {stats && (
        <>
          <div className="adm-tiles">
            <Tile label="Real users" value={stats.users.total} sub={`+${stats.users.last7Days} this week · +${stats.users.today} today`} />
            <Tile label="Active this week" value={stats.users.activeLast7Days} sub={`${stats.users.addedData} ${stats.users.addedData === 1 ? "has" : "have"} added data`} />
            <Tile label="Demos tried" value={stats.demos.total} sub={`+${stats.demos.last7Days} this week · +${stats.demos.today} today`} />
            <Tile label="Demos live now" value={stats.demos.liveNow} sub="deleted after 24 hours" />
          </div>

          <div className="adm-row">
            <section className="pill adm-chart-pill">
              <div className="adm-chart-head">
                <h2 className="pill-title">Last 30 days</h2>
                <div className="adm-legend">
                  <span><i className="io-key" style={{ background: SIGNUP_COLOR }} />Sign-ups</span>
                  <span><i className="io-key" style={{ background: DEMO_COLOR }} />Demos</span>
                </div>
              </div>
              <div className="adm-chart">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={stats.days} margin={{ top: 8, right: 8, bottom: 0, left: 0 }} barGap={2} barCategoryGap="20%">
                    <CartesianGrid vertical={false} stroke="rgba(0, 0, 0, 0.08)" />
                    <XAxis
                      dataKey="day"
                      tickFormatter={shortDay}
                      tick={axisText}
                      tickLine={false}
                      axisLine={{ stroke: "rgba(0, 0, 0, 0.25)" }}
                      interval={6}
                    />
                    <YAxis allowDecimals={false} tick={axisText} tickLine={false} axisLine={false} width={32} />
                    <Tooltip content={DayTooltip} cursor={{ fill: "rgba(0, 0, 0, 0.05)" }} />
                    <Bar dataKey="signups" fill={SIGNUP_COLOR} radius={[4, 4, 0, 0]} maxBarSize={16} animationDuration={800} animationEasing="ease-out" />
                    <Bar dataKey="demos" fill={DEMO_COLOR} radius={[4, 4, 0, 0]} maxBarSize={16} animationDuration={800} animationEasing="ease-out" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </section>

            <section className="pill adm-totals">
              <h2 className="pill-title">In the database</h2>
              <dl className="adm-list">
                <div><dt>Transactions</dt><dd>{stats.content.transactions}</dd></div>
                <div><dt>Recurring items</dt><dd>{stats.content.recurring}</dd></div>
                <div><dt>Debts</dt><dd>{stats.content.debts}</dd></div>
                <div><dt>Budgets</dt><dd>{stats.content.budgets}</dd></div>
                <div><dt>Database size</dt><dd>{stats.databaseSize}</dd></div>
              </dl>
              <p className="adm-note">Real accounts only, demos left out.</p>
            </section>
          </div>

          <section className="pill adm-recent">
            <h2 className="pill-title">Recent sign-ups</h2>
            {stats.recentSignups.length === 0 ? (
              <p className="pill-empty">Nobody yet.</p>
            ) : (
              <table className="adm-table">
                <thead>
                  <tr><th>Name</th><th>Joined</th><th>Last seen</th><th className="adm-num">Transactions</th></tr>
                </thead>
                <tbody>
                  {stats.recentSignups.map((u, i) => (
                    <tr key={i}>
                      <td>{u.firstName}</td>
                      <td>{when(u.joined)}</td>
                      <td>{when(u.lastSeen)}</td>
                      <td className="adm-num">{u.transactions}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>

        </>
      )}
    </div>
  );
}

export default AdminPage;
