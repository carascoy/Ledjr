import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import "../styles/AuthPage.css";

// "forgot your password?" -> type your email -> we send a link (in dev the link is in the backend log)
function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    document.title = "Forgot password · Ledjr";
  }, []);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = await api("/password/forgot", { method: "POST", body: JSON.stringify({ email: email.trim() }) });
      if (response.status === 429) {
        setError(await response.text());
        return;
      }
      if (!response.ok) {
        setError("Something went wrong. Try again in a minute.");
        return;
      }
      // same screen whether or not there's an account, on purpose
      setSent(true);
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-page">
      <h1 className="auth-title">Ledjr</h1>
      {sent ? (
        <div className="auth-card">
          <h2 className="auth-heading">Check your email</h2>
          <p className="auth-note">
            If there's an account for <b>{email.trim()}</b>, we sent it a link to set a new password. The link runs out in 30 minutes.
          </p>
          {/* only shows up when running locally (npm run dev) */}
          {import.meta.env.DEV && (
            <p className="auth-note auth-dev-note">Running locally? Nothing actually gets emailed yet, the link is printed in the backend log.</p>
          )}
          <button type="button" className="auth-link" onClick={() => setSent(false)}>
            Didn't get it? Send another
          </button>
          <Link to="/" className="auth-link">
            Back to sign in
          </Link>
        </div>
      ) : (
        <form className="auth-card" onSubmit={submit}>
          <h2 className="auth-heading">Forgot your password?</h2>
          <p className="auth-note">Type the email you signed up with and we'll send you a link to set a new one.</p>
          <input
            className="auth-input"
            name="email"
            inputMode="email"
            autoComplete="email"
            autoCapitalize="none"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoFocus
          />
          {error && <p className="error-text auth-error">{error}</p>}
          <button type="submit" className="auth-submit" disabled={busy || !email.trim()}>
            {busy ? "Sending…" : "Send reset link"}
          </button>
          <Link to="/" className="auth-link">
            Back to sign in
          </Link>
        </form>
      )}
    </div>
  );
}

export default ForgotPasswordPage;
