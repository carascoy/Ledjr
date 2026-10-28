import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api } from "../api/client";
import "../styles/AuthPage.css";

type Stage = "checking" | "form" | "bad-link" | "done";

// where the email link goes: /reset-password?token=...
function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const [stage, setStage] = useState<Stage>(token ? "checking" : "bad-link");
  const [badLinkMessage, setBadLinkMessage] = useState("This reset link is missing its code. Ask for a new one.");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    document.title = "Set a new password · Ledjr";
  }, []);

  // check the link before showing the form, so an old link says so right away
  useEffect(() => {
    if (!token) return;
    let ignore = false;
    api(`/password/reset/check?token=${encodeURIComponent(token)}`)
      .then(async (r) => {
        if (ignore) return;
        if (r.ok) {
          setStage("form");
        } else {
          setBadLinkMessage(await r.text());
          setStage("bad-link");
        }
      })
      .catch(() => {
        if (!ignore) setError("Couldn't reach the server.");
      });
    return () => {
      ignore = true;
    };
  }, [token]);

  // same 3 rules as sign up
  const rules = [
    { ok: password.length >= 8, label: "At least 8 characters." },
    { ok: /[0-9]/.test(password), label: "At least 1 number." },
    { ok: /[^A-Za-z0-9]/.test(password), label: "At least 1 special character." },
  ];
  const matches = password !== "" && password === confirm;
  const canSubmit = rules.every((r) => r.ok) && matches;

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = await api("/password/reset", {
        method: "POST",
        body: JSON.stringify({ token, newPassword: password }),
      });
      if (response.ok) {
        setStage("done");
        return;
      }
      const message = await response.text();
      // a rule problem stays on the form, a dead link gets the dead link screen
      if (message.startsWith("Password")) {
        setError(message);
      } else {
        setBadLinkMessage(message);
        setStage("bad-link");
      }
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-page">
      <h1 className="auth-title">Ledjr</h1>

      {stage === "checking" && (
        <div className="auth-card">
          <p className="auth-note">{error || "Checking your link…"}</p>
        </div>
      )}

      {stage === "bad-link" && (
        <div className="auth-card">
          <h2 className="auth-heading">That link doesn't work</h2>
          <p className="auth-note">{badLinkMessage}</p>
          <Link to="/forgot-password" className="auth-submit auth-submit-link">
            Get a new link
          </Link>
          <Link to="/" className="auth-link">
            Back to sign in
          </Link>
        </div>
      )}

      {stage === "done" && (
        <div className="auth-card">
          <h2 className="auth-heading">Password changed</h2>
          <p className="auth-note">You've been signed out everywhere. Sign in with your new password.</p>
          <Link to="/" className="auth-submit auth-submit-link">
            Sign in
          </Link>
        </div>
      )}

      {stage === "form" && (
        <form className="auth-card" onSubmit={submit}>
          <h2 className="auth-heading">Set a new password</h2>
          <input
            className="auth-input"
            type="password"
            name="newPassword"
            autoComplete="new-password"
            placeholder="New password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoFocus
          />
          <input
            className="auth-input"
            type="password"
            name="confirmPassword"
            autoComplete="new-password"
            placeholder="New password again"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
          />
          <div className="password-hints auth-rules">
            {rules.map((r) => (
              <p key={r.label} className={r.ok ? "correct" : "wrong"}>
                {r.label}
              </p>
            ))}
            {confirm !== "" && <p className={matches ? "correct" : "wrong"}>{matches ? "Both match." : "They don't match yet."}</p>}
          </div>
          {error && <p className="error-text auth-error">{error}</p>}
          <button type="submit" className="auth-submit" disabled={busy || !canSubmit}>
            {busy ? "Saving…" : "Set new password"}
          </button>
        </form>
      )}
    </div>
  );
}

export default ResetPasswordPage;
