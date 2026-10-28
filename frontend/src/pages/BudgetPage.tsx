import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useUser } from "../context/UserContext";
import { api } from "../api/client";
import MonthSwitcher from "../components/MonthSwitcher";
import CategoryTag from "../components/CategoryTag";
import BudgetBreakdown from "../components/BudgetBreakdown";
import { toMonthParam } from "../utils/months";
import { useMonth } from "../context/MonthContext";
import type { BudgetLine, BudgetResponse } from "../types/BudgetResponse";
import "../styles/Dashboard.css";
import "../styles/BudgetPage.css";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

// how full the bar is, capped at 100 so going over doesn't break the layout
function percent(spent: number, budget: number) {
  return budget > 0 ? Math.min(100, (spent / budget) * 100) : 0;
}

// no paycheck set up and no income logged, so there's nothing to estimate from
function AddPaycheckNudge() {
  return (
    <p className="bud-nudge">
      Add your paycheck on <Link to="/recurring">Recurring</Link> and this works out how much you can budget each month.
    </p>
  );
}

// per category budgets for one month: spent vs planned, set/change amounts inline
function BudgetPage() {
  const { user } = useUser();
  const { month, setMonth } = useMonth();
  const [data, setData] = useState<BudgetResponse | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);

  // one row editable at a time
  const [editingId, setEditingId] = useState<number | null>(null);
  const [draft, setDraft] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  // copy last month / fill suggestions share one busy flag + error
  const [bulkBusy, setBulkBusy] = useState(false);
  const [bulkError, setBulkError] = useState("");
  // "+ New category" form at the top
  const [newOpen, setNewOpen] = useState(false);
  const [newName, setNewName] = useState("");
  const [creating, setCreating] = useState(false);
  const [newError, setNewError] = useState("");

  const monthParam = toMonthParam(month);
  const monthName = month.toLocaleDateString("en-US", { month: "long" });
  const lastMonthName = new Date(month.getFullYear(), month.getMonth() - 1, 1).toLocaleDateString("en-US", { month: "long" });

  useEffect(() => {
    if (!user) return;
    // if the month changes before this finishes, ignore the old result
    let ignore = false;
    api(`/budget?month=${monthParam}`)
      .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
      .then((body: BudgetResponse) => {
        if (!ignore) setData(body);
      })
      .catch(() => {
        if (!ignore) setLoadFailed(true);
      });
    return () => {
      ignore = true;
    };
  }, [user, monthParam]);

  function changeMonth(next: Date) {
    setMonth(next);
    setData(null);
    setLoadFailed(false);
    setEditingId(null);
    setBulkError("");
  }

  // opens with the current budget, or the suggestion if there's no budget yet
  function startEdit(line: BudgetLine) {
    setEditingId(line.categoryId);
    const start = line.budgeted ?? line.suggested;
    setDraft(start != null ? String(start) : "");
    setError("");
  }

  // make the category, reload the month, then open its budget box so you can set it right away
  async function createCategory(e: React.FormEvent) {
    e.preventDefault();
    const name = newName.trim();
    if (!name) return;
    setCreating(true);
    setNewError("");
    try {
      const response = await api("/categories", {
        method: "POST",
        body: JSON.stringify({ name, kind: "expense" }),
      });
      if (!response.ok) {
        setNewError(await response.text());
        return;
      }
      const created: { id: number } = await response.json();
      const month = await api(`/budget?month=${monthParam}`);
      if (!month.ok) return;
      const body: BudgetResponse = await month.json();
      setData(body);
      setNewOpen(false);
      setNewName("");
      const line = body.lines.find((l) => l.categoryId === created.id);
      if (line) {
        startEdit(line);
        // wait a frame so the row exists, then bring it into view
        requestAnimationFrame(() => {
          const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
          document.getElementById(`bud-row-${created.id}`)?.scrollIntoView({ block: "center", behavior: reduce ? "auto" : "smooth" });
        });
      }
    } catch {
      setNewError("Couldn't reach the server.");
    } finally {
      setCreating(false);
    }
  }

  // amount null = take the budget off this category for the month
  async function save(categoryId: number, amount: number | null) {
    setSaving(true);
    setError("");
    try {
      const response = await api(`/budget/${categoryId}?month=${monthParam}`, {
        method: "PUT",
        body: JSON.stringify({ amount }),
      });
      if (!response.ok) {
        setError(await response.text());
        return;
      }
      // server sends the whole month back, no 2nd request needed
      setData(await response.json());
      setEditingId(null);
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setSaving(false);
    }
  }

  // "copy" = last month's amounts, "fill" = every suggestion that isn't budgeted yet
  async function bulk(action: "copy" | "fill") {
    setBulkBusy(true);
    setBulkError("");
    setEditingId(null);
    try {
      const response = await api(`/budget/${action}?month=${monthParam}`, { method: "POST" });
      if (!response.ok) {
        setBulkError(await response.text());
        return;
      }
      setData(await response.json());
    } catch {
      setBulkError("Couldn't reach the server.");
    } finally {
      setBulkBusy(false);
    }
  }

  const budgeted = data?.lines.filter((l) => l.budgeted != null) ?? [];
  const notBudgeted = data?.lines.filter((l) => l.budgeted == null) ?? [];
  const canFill = notBudgeted.some((l) => l.suggested != null);
  const left = data ? data.totalBudgeted - data.totalSpent : 0;
  const toAssign = data ? data.canBudget - data.totalBudgeted : 0;

  function renderRow(line: BudgetLine) {
    const has = line.budgeted != null;
    const over = has && line.spent > (line.budgeted ?? 0);
    const editing = editingId === line.categoryId;
    const draftNumber = Number(draft);
    const draftOk = draft.trim() !== "" && Number.isFinite(draftNumber) && draftNumber > 0;

    return (
      <li key={line.categoryId} id={`bud-row-${line.categoryId}`} className={`bud-row ${has ? "" : "bud-row--none"}`}>
        <span className="bud-name">
          <CategoryTag name={line.name} colorSlot={line.colorSlot} />
        </span>

        {has ? (
          <>
            {/* key={monthParam} replays the grow animation when you switch months */}
            <div
              key={monthParam}
              className={`bud-bar bud-bar--small ${over ? "bud-bar--over" : ""}`}
              role="meter"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={Math.round(percent(line.spent, line.budgeted ?? 0))}
              aria-label={`${line.name} budget used`}
            >
              <span className="bud-bar-fill" style={{ width: `${percent(line.spent, line.budgeted ?? 0)}%` }} />
            </div>
            <button type="button" className="bud-amount" onClick={() => startEdit(line)} aria-label={`Change ${line.name} budget`}>
              <b>{currency.format(line.spent)}</b> of {currency.format(line.budgeted ?? 0)}
            </button>
            <span className={`bud-left ${over ? "bud-left--over" : ""}`}>
              {over
                ? `${currency.format(line.spent - (line.budgeted ?? 0))} over`
                : `${currency.format((line.budgeted ?? 0) - line.spent)} left`}
            </span>
          </>
        ) : (
          <>
            <span className="bud-spent-only">
              {line.spent > 0 ? (
                <>
                  <b>{currency.format(line.spent)}</b> spent
                </>
              ) : (
                "Nothing spent"
              )}
              {line.suggested != null && (
                <span className="bud-suggest">
                  {" · "}suggested <b>{currency.format(line.suggested)}</b>
                </span>
              )}
            </span>
            <span className="bud-set">
              {!editing && (
                <button type="button" className="bud-btn" onClick={() => startEdit(line)}>
                  Set budget
                </button>
              )}
            </span>
          </>
        )}

        {editing && (
          <form
            className="bud-edit"
            onSubmit={(e) => {
              e.preventDefault();
              if (draftOk) save(line.categoryId, draftNumber);
            }}
            onKeyDown={(e) => {
              if (e.key === "Escape") setEditingId(null);
            }}
          >
            <label className="bud-edit-label" htmlFor={`bud-${line.categoryId}`}>
              {monthName} budget
            </label>
            <input
              id={`bud-${line.categoryId}`}
              className="qa-input bud-edit-input"
              type="number"
              inputMode="decimal"
              step="0.01"
              min="0.01"
              placeholder="0.00"
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              autoFocus
            />
            <button type="submit" className="bud-save" disabled={saving || !draftOk}>
              {saving ? "…" : "Save"}
            </button>
            {has && (
              <button type="button" className="bud-cancel" disabled={saving} onClick={() => save(line.categoryId, null)}>
                Remove
              </button>
            )}
            <button type="button" className="bud-cancel" onClick={() => setEditingId(null)}>
              Cancel
            </button>
            {line.suggested != null && (
              <p className="bud-edit-hint">
                {line.suggestionReason === "bill"
                  ? "Your recurring bills here come to "
                  : "You usually spend about "}
                <button type="button" className="bud-hint-amount" onClick={() => setDraft(String(line.suggested))}>
                  {currency.format(line.suggested)}
                </button>
                {line.suggestionReason === "bill" ? " this month." : " a month here."}
              </p>
            )}
            {error && <p className="bud-error bud-edit-error">{error}</p>}
          </form>
        )}
      </li>
    );
  }

  return (
    <>
      <header className="dash-header">
        <MonthSwitcher month={month} onChange={changeMonth} allowFuture />
        {user && (
          <button
            type="button"
            className="quick-add"
            onClick={() => {
              setNewOpen((o) => !o);
              setNewError("");
            }}
          >
            {newOpen ? "Close" : "+ New category"}
          </button>
        )}
      </header>

      <div className={`collapsible ${newOpen ? "expanded" : ""}`}>
        <div className="collapsible-inner">
          <form className="bud-new" onSubmit={createCategory}>
            <input
              className="qa-input bud-new-input"
              placeholder="Category name, like Pets or Gifts"
              maxLength={40}
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              aria-label="New category name"
              tabIndex={newOpen ? 0 : -1}
            />
            <button type="submit" className="bud-save bud-new-add" disabled={creating || !newName.trim()} tabIndex={newOpen ? 0 : -1}>
              {creating ? "Adding…" : "Add"}
            </button>
            <p className="bud-new-hint">
              Rename, recolor or delete categories in{" "}
              <Link to="/settings#categories" tabIndex={newOpen ? 0 : -1}>
                Settings
              </Link>
              .
            </p>
            {newError && <p className="bud-error bud-new-error">{newError}</p>}
          </form>
        </div>
      </div>

      {!user ? (
        <p className="pill-empty bud-message">Sign in to see your budget.</p>
      ) : loadFailed ? (
        <p className="pill-empty bud-message">Couldn't load your budget. Try refreshing.</p>
      ) : data === null ? (
        <p className="pill-empty bud-message">Loading…</p>
      ) : (
        <>
          {budgeted.length === 0 ? (
            <section className="pill bud-overview">
              <h2 className="pill-title">No budget for {monthName} yet</h2>
              {data.incomeBasis === "none" ? (
                <AddPaycheckNudge />
              ) : (
                <div className="bud-estimate">
                  <p className="bud-estimate-line">
                    You can budget about <span className="bud-estimate-amount">{currency.format(data.canBudget)}</span>
                  </p>
                  <BudgetBreakdown data={data} />
                </div>
              )}
              {(canFill || data.canCopyLastMonth) && (
                <div className="bud-actions">
                  {canFill && (
                    <button type="button" className="bud-save" disabled={bulkBusy} onClick={() => bulk("fill")}>
                      Fill in suggestions
                    </button>
                  )}
                  {data.canCopyLastMonth && (
                    <button
                      type="button"
                      className={canFill ? "bud-btn bud-btn--tall" : "bud-save"}
                      disabled={bulkBusy}
                      onClick={() => bulk("copy")}
                    >
                      Copy {lastMonthName}'s budget
                    </button>
                  )}
                </div>
              )}
              {canFill && <p className="bud-hint">Suggestions come from your recurring bills and what you usually spend. Change any of them after.</p>}
              {bulkError && <p className="bud-error">{bulkError}</p>}
              {data.savedTotal > 0 && (
                <p className="bud-saved-line">
                  Saved so far: <b>{currency.format(data.savedTotal)}</b>
                </p>
              )}
            </section>
          ) : (
            <section className="pill bud-overview">
              <div className="bud-figure">
                <span className="bud-big">{currency.format(data.totalSpent)}</span>
                <span className="bud-big-label">spent of {currency.format(data.totalBudgeted)} budgeted</span>
              </div>
              <div
                key={monthParam}
                className={`bud-bar bud-bar--large ${left < 0 ? "bud-bar--over" : ""}`}
                role="meter"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={Math.round(percent(data.totalSpent, data.totalBudgeted))}
                aria-label="Budget used"
              >
                <span className="bud-bar-fill" style={{ width: `${percent(data.totalSpent, data.totalBudgeted)}%` }} />
              </div>

              <dl className="bud-figures">
                <div>
                  <dt>{left < 0 ? "Over budget" : "Left to spend"}</dt>
                  <dd>{currency.format(Math.abs(left))}</dd>
                </div>
                {data.incomeBasis !== "none" && (
                  <div>
                    <dt>You can budget</dt>
                    <dd>{currency.format(data.canBudget)}</dd>
                  </div>
                )}
                {data.incomeBasis !== "none" && (
                  <div>
                    <dt>{toAssign < 0 ? "Budgeted over that" : "Not budgeted yet"}</dt>
                    <dd>{currency.format(Math.abs(toAssign))}</dd>
                  </div>
                )}
                <div>
                  <dt>Saved so far</dt>
                  <dd>{currency.format(data.savedTotal)}</dd>
                </div>
              </dl>
              {data.incomeBasis === "none" ? <AddPaycheckNudge /> : <BudgetBreakdown data={data} />}
              {data.unbudgetedSpent > 0 && (
                <p className="bud-note">
                  Plus <b>{currency.format(data.unbudgetedSpent)}</b> spent outside the budget this month.
                </p>
              )}
            </section>
          )}

          {budgeted.length > 0 && (
            <section className="pill bud-list-pill">
              <h2 className="pill-title">Budgeted</h2>
              <ul className="bud-list">{budgeted.map(renderRow)}</ul>
            </section>
          )}

          {notBudgeted.length > 0 && (
            <section className="pill bud-list-pill">
              <div className="bud-list-head">
                <h2 className="pill-title">{budgeted.length > 0 ? "Not budgeted" : "Categories"}</h2>
                {budgeted.length > 0 && canFill && (
                  <button type="button" className="bud-btn" disabled={bulkBusy} onClick={() => bulk("fill")}>
                    Fill in suggestions
                  </button>
                )}
              </div>
              {budgeted.length > 0 && bulkError && <p className="bud-error bud-list-error">{bulkError}</p>}
              <ul className="bud-list">{notBudgeted.map(renderRow)}</ul>
            </section>
          )}

          {data.lines.length === 0 && (
            <p className="pill-empty bud-message">No expense categories yet. Make one from + Add Transaction.</p>
          )}

          <p className="bud-footnote">
            Whatever's left in a budget when the month ends counts as saved. Going over just adds $0 that month, each month starts fresh.{" "}
            <Link to="/settings#categories">Manage categories</Link>
          </p>
        </>
      )}
    </>
  );
}

export default BudgetPage;
