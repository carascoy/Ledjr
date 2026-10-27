import { useNavigate } from "react-router-dom";
import { useUser } from "../context/UserContext";
import { api } from "../api/client";
import "../styles/BudgetPage.css";
import "../styles/SetupPage.css";

// only shows if someone left setup halfway (closed the tab etc). Dismiss = never again
function SetupBanner() {
  const { user, setUser } = useUser();
  const navigate = useNavigate();
  if (!user || user.setupDone !== false) return null;

  async function dismiss() {
    if (!user) return;
    setUser({ ...user, setupDone: true }); // hide it right away, the request can catch up
    try {
      await api("/me/setup-done", { method: "POST" });
    } catch {
      // worst case it shows up again next visit
    }
  }

  return (
    <div className="setup-banner" role="status">
      <p>Add your paycheck and bills and Ledjr can work out your budget for you. Takes a minute.</p>
      <div className="setup-banner-actions">
        <button type="button" className="bud-save" onClick={() => navigate("/setup")}>
          Set up
        </button>
        <button type="button" className="bud-cancel" onClick={dismiss}>
          Dismiss
        </button>
      </div>
    </div>
  );
}

export default SetupBanner;
