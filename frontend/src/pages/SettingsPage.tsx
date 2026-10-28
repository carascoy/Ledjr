import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useUser } from "../context/UserContext";
import { api } from "../api/client";
import { CATEGORY_COLORS, categoryColor } from "../utils/categoryColors";
import type { CategoryResponse } from "../types/CategoryResponse";
import "../styles/Dashboard.css";
import "../styles/BudgetPage.css";
import "../styles/SettingsPage.css";

// same 3 rules as sign up (AuthPage.tsx) and the server (AuthController.passwordProblem)
function passwordRules(pw: string) {
  return [
    { ok: pw.length >= 8, label: "At least 8 characters" },
    { ok: /[0-9]/.test(pw), label: "At least 1 number" },
    { ok: /[^A-Za-z0-9]/.test(pw), label: "At least 1 special character" },
  ];
}

// little "Saved" / error line under a form
function Status({ ok, error }: { ok: string; error: string }) {
  if (error) return <p className="bud-error set-status">{error}</p>;
  if (ok) return <p className="set-status set-status--ok" role="status">{ok}</p>;
  return null;
}

// ---------- your name + email ----------
function ProfileSection() {
  const { user, setUser } = useUser();
  const [first, setFirst] = useState(user?.firstName ?? "");
  const [last, setLast] = useState(user?.lastName ?? "");
  const [email, setEmail] = useState(user?.email ?? "");
  const [saving, setSaving] = useState(false);
  const [ok, setOk] = useState("");
  const [error, setError] = useState("");

  const changed = user != null && (first !== user.firstName || last !== user.lastName || email !== user.email);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setOk("");
    setError("");
    try {
      const response = await api("/me", {
        method: "PUT",
        body: JSON.stringify({ firstName: first, lastName: last, email }),
      });
      if (!response.ok) {
        setError(await response.text());
        return;
      }
      const updated = await response.json();
      setUser(updated);
      setFirst(updated.firstName);
      setLast(updated.lastName);
      setEmail(updated.email);
      setOk("Saved.");
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="pill set-section">
      <h2 className="pill-title">Profile</h2>
      <form className="set-form" onSubmit={save}>
        <div className="set-fields">
          <label className="set-field">
            <span>First name</span>
            <input className="qa-input set-input" value={first} onChange={(e) => setFirst(e.target.value)} autoComplete="given-name" />
          </label>
          <label className="set-field">
            <span>Last name</span>
            <input className="qa-input set-input" value={last} onChange={(e) => setLast(e.target.value)} autoComplete="family-name" />
          </label>
          <label className="set-field set-field--wide">
            <span>Email</span>
            <input className="qa-input set-input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
          </label>
        </div>
        <div className="set-actions">
          <button type="submit" className="bud-save" disabled={saving || !changed}>
            {saving ? "Saving…" : "Save"}
          </button>
          <Status ok={ok} error={error} />
        </div>
      </form>
    </section>
  );
}

// ---------- password ----------
function PasswordSection() {
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [saving, setSaving] = useState(false);
  const [ok, setOk] = useState("");
  const [error, setError] = useState("");

  const rules = passwordRules(next);
  const matches = next !== "" && next === confirm;
  const canSave = current !== "" && rules.every((r) => r.ok) && matches;

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setOk("");
    setError("");
    try {
      const response = await api("/me/password", {
        method: "PUT",
        body: JSON.stringify({ currentPassword: current, newPassword: next }),
      });
      if (!response.ok) {
        setError(await response.text());
        return;
      }
      setCurrent("");
      setNext("");
      setConfirm("");
      setOk("Password changed.");
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="pill set-section">
      <h2 className="pill-title">Password</h2>
      <form className="set-form" onSubmit={save}>
        <div className="set-fields">
          <label className="set-field set-field--wide">
            <span>Current password</span>
            <input className="qa-input set-input" type="password" value={current} onChange={(e) => setCurrent(e.target.value)} autoComplete="current-password" />
          </label>
          <label className="set-field">
            <span>New password</span>
            <input className="qa-input set-input" type="password" value={next} onChange={(e) => setNext(e.target.value)} autoComplete="new-password" />
          </label>
          <label className="set-field">
            <span>New password again</span>
            <input className="qa-input set-input" type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" />
          </label>
        </div>
        {next !== "" && (
          <ul className="set-rules">
            {rules.map((r) => (
              <li key={r.label} className={r.ok ? "set-rule--ok" : ""}>
                {r.ok ? "✓" : "·"} {r.label}
              </li>
            ))}
            {confirm !== "" && <li className={matches ? "set-rule--ok" : ""}>{matches ? "✓" : "·"} Both match</li>}
          </ul>
        )}
        <div className="set-actions">
          <button type="submit" className="bud-save" disabled={saving || !canSave}>
            {saving ? "Saving…" : "Change password"}
          </button>
          <Status ok={ok} error={error} />
        </div>
      </form>
    </section>
  );
}

// ---------- categories: add, rename, recolor, delete ----------
type Editing = { id: number; mode: "rename" | "color" | "delete" } | null;

function CategoriesSection() {
  const [categories, setCategories] = useState<CategoryResponse[] | null>(null);
  const [editing, setEditing] = useState<Editing>(null);
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  // add form
  const [newName, setNewName] = useState("");
  const [newKind, setNewKind] = useState<"expense" | "income">("expense");
  const [addError, setAddError] = useState("");
  const location = useLocation();

  useEffect(() => {
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
  }, []);

  // coming from Budget's "Manage categories" link (/settings#categories) -> scroll down to here
  useEffect(() => {
    if (location.hash !== "#categories" || categories === null) return;
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    document.getElementById("categories")?.scrollIntoView({ behavior: reduce ? "auto" : "smooth", block: "start" });
  }, [location.hash, categories]);

  function open(c: CategoryResponse, mode: "rename" | "color" | "delete") {
    setError("");
    if (editing?.id === c.id && editing.mode === mode) {
      setEditing(null);
      return;
    }
    setEditing({ id: c.id, mode });
    setDraft(c.name);
  }

  // PUT with just the field that changed
  async function update(c: CategoryResponse, change: { name?: string; colorSlot?: number }) {
    setBusy(true);
    setError("");
    try {
      const response = await api(`/categories/${c.id}`, { method: "PUT", body: JSON.stringify(change) });
      if (!response.ok) {
        setError(await response.text());
        return;
      }
      const updated: CategoryResponse = await response.json();
      setCategories((list) =>
        (list ?? []).map((x) => (x.id === updated.id ? updated : x)).sort((a, b) => a.name.localeCompare(b.name))
      );
      setEditing(null);
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setBusy(false);
    }
  }

  async function remove(c: CategoryResponse) {
    setBusy(true);
    setError("");
    try {
      const response = await api(`/categories/${c.id}`, { method: "DELETE" });
      if (!response.ok) {
        setError(await response.text());
        return;
      }
      setCategories((list) => (list ?? []).filter((x) => x.id !== c.id));
      setEditing(null);
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setBusy(false);
    }
  }

  async function add(e: React.FormEvent) {
    e.preventDefault();
    const name = newName.trim();
    if (!name) return;
    setBusy(true);
    setAddError("");
    try {
      const response = await api("/categories", { method: "POST", body: JSON.stringify({ name, kind: newKind }) });
      if (!response.ok) {
        setAddError(await response.text());
        return;
      }
      const created: CategoryResponse = await response.json();
      setCategories((list) => [...(list ?? []), created].sort((a, b) => a.name.localeCompare(b.name)));
      setNewName("");
    } catch {
      setAddError("Couldn't reach the server.");
    } finally {
      setBusy(false);
    }
  }

  function row(c: CategoryResponse) {
    const mode = editing?.id === c.id ? editing.mode : null;
    return (
      <li key={c.id} className="set-cat">
        <div className="set-cat-main">
          <button
            type="button"
            className="set-cat-dot"
            style={{ background: categoryColor(c.colorSlot) }}
            onClick={() => open(c, "color")}
            aria-label={`Change ${c.name}'s color`}
            aria-expanded={mode === "color"}
          />
          {mode === "rename" ? (
            <form
              className="set-cat-rename"
              onSubmit={(e) => {
                e.preventDefault();
                if (draft.trim() && draft.trim() !== c.name) update(c, { name: draft.trim() });
                else setEditing(null);
              }}
              onKeyDown={(e) => {
                if (e.key === "Escape") setEditing(null);
              }}
            >
              <input
                className="qa-input set-input set-cat-input"
                value={draft}
                maxLength={40}
                onChange={(e) => setDraft(e.target.value)}
                aria-label={`New name for ${c.name}`}
                autoFocus
              />
              <button type="submit" className="bud-save" disabled={busy || !draft.trim()}>
                Save
              </button>
              <button type="button" className="bud-cancel" onClick={() => setEditing(null)}>
                Cancel
              </button>
            </form>
          ) : (
            <>
              <span className="set-cat-name">{c.name}</span>
              <span className="set-cat-actions">
                <button type="button" className="bud-btn" onClick={() => open(c, "rename")}>
                  Rename
                </button>
                <button type="button" className="bud-cancel" onClick={() => open(c, "delete")}>
                  Delete
                </button>
              </span>
            </>
          )}
        </div>

        {mode === "color" && (
          <div className="set-swatches" role="group" aria-label={`Colors for ${c.name}`}>
            {CATEGORY_COLORS.map((color, slot) => (
              <button
                key={color}
                type="button"
                className={`set-swatch ${slot === c.colorSlot ? "set-swatch--on" : ""}`}
                style={{ background: color }}
                disabled={busy}
                onClick={() => (slot === c.colorSlot ? setEditing(null) : update(c, { colorSlot: slot }))}
                aria-label={`Color ${slot + 1}`}
                aria-pressed={slot === c.colorSlot}
              />
            ))}
          </div>
        )}

        {mode === "delete" && (
          <div className="set-confirm">
            <p>
              Delete <b>{c.name}</b>? Its transactions and recurring bills stay, they just become uncategorized.
              {c.kind === "expense" && " Its budgets are removed."}
            </p>
            <div className="set-actions">
              <button type="button" className="bud-save" disabled={busy} onClick={() => remove(c)}>
                {busy ? "Deleting…" : "Delete"}
              </button>
              <button type="button" className="bud-cancel" onClick={() => setEditing(null)}>
                Cancel
              </button>
            </div>
          </div>
        )}

        {editing?.id === c.id && error && <p className="bud-error set-status">{error}</p>}
      </li>
    );
  }

  const expense = categories?.filter((c) => c.kind === "expense") ?? [];
  const income = categories?.filter((c) => c.kind === "income") ?? [];

  return (
    <section className="pill set-section" id="categories">
      <h2 className="pill-title">Categories</h2>
      {categories === null ? (
        <p className="pill-empty">Loading…</p>
      ) : (
        <>
          <form className="set-cat-add" onSubmit={add}>
            <div className="qa-type set-kind" role="group" aria-label="Kind">
              <button type="button" className={newKind === "expense" ? "qa-type-active" : ""} onClick={() => setNewKind("expense")}>
                Expense
              </button>
              <button type="button" className={newKind === "income" ? "qa-type-active" : ""} onClick={() => setNewKind("income")}>
                Income
              </button>
            </div>
            <input
              className="qa-input set-input set-cat-new"
              placeholder="New category"
              maxLength={40}
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              aria-label="New category name"
            />
            <button type="submit" className="bud-save" disabled={busy || !newName.trim()}>
              Add
            </button>
            {addError && <p className="bud-error set-status set-cat-add-error">{addError}</p>}
          </form>

          <h3 className="set-subtitle">Expenses</h3>
          {expense.length === 0 ? <p className="pill-empty">None yet.</p> : <ul className="set-cats">{expense.map(row)}</ul>}
          <h3 className="set-subtitle">Income</h3>
          {income.length === 0 ? <p className="pill-empty">None yet.</p> : <ul className="set-cats">{income.map(row)}</ul>}
        </>
      )}
    </section>
  );
}

// ---------- delete account ----------
function DeleteAccountSection() {
  const { setUser } = useUser();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function deleteAccount(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = await api("/me", { method: "DELETE", body: JSON.stringify({ password }) });
      if (!response.ok) {
        setError(await response.text());
        setBusy(false);
        return;
      }
      setUser(null);
      navigate("/", { replace: true });
    } catch {
      setError("Couldn't reach the server.");
      setBusy(false);
    }
  }

  return (
    <section className="pill set-section set-danger">
      <h2 className="pill-title">Delete account</h2>
      <p className="set-text">
        Deletes your account and everything in it: transactions, budgets, recurring bills, debts and categories. This can't be undone.
      </p>
      {!open ? (
        <div className="set-actions">
          <button type="button" className="bud-btn bud-btn--tall" onClick={() => setOpen(true)}>
            Delete my account…
          </button>
        </div>
      ) : (
        <form className="set-form" onSubmit={deleteAccount}>
          <label className="set-field set-field--wide">
            <span>Type your password to confirm</span>
            <input
              className="qa-input set-input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              autoFocus
            />
          </label>
          <div className="set-actions">
            <button type="submit" className="bud-save" disabled={busy || password === ""}>
              {busy ? "Deleting…" : "Permanently delete everything"}
            </button>
            <button
              type="button"
              className="bud-cancel"
              onClick={() => {
                setOpen(false);
                setPassword("");
                setError("");
              }}
            >
              Cancel
            </button>
          </div>
          {error && <p className="bud-error set-status">{error}</p>}
        </form>
      )}
    </section>
  );
}

// the whole page. each section saves on its own
function SettingsPage() {
  const navigate = useNavigate();
  return (
    <div className="set-page">
      <header className="page-header">
        <h1 className="page-title">Settings</h1>
      </header>

      <ProfileSection />
      <PasswordSection />
      <CategoriesSection />

      <section className="pill set-section">
        <h2 className="pill-title">Setup</h2>
        <p className="set-text">Add your paycheck, bills and debts in one go, then build a budget from them.</p>
        <div className="set-actions">
          <button type="button" className="bud-btn bud-btn--tall" onClick={() => navigate("/setup")}>
            Run setup again
          </button>
        </div>
      </section>

      <section className="pill set-section">
        <h2 className="pill-title">About Ledjr</h2>
        <p className="set-text">A personal budgeting app. What it keeps about you:</p>
        <ul className="set-list">
          <li>Your name, email and password. The password is stored hashed, never as the actual text.</li>
          <li>What you put in yourself: transactions, budgets, recurring bills, debts and categories.</li>
          <li>Nothing from your bank. Ledjr doesn't connect to any bank accounts.</li>
        </ul>
        <p className="set-text">Deleting your account removes all of it.</p>
      </section>

      <DeleteAccountSection />
    </div>
  );
}

export default SettingsPage;
