import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useUser } from "../context/UserContext";
import { api } from "../api/client";
import type { DebtSummary } from "../types/DebtSummary";
import AddDebtForm from "./AddDebtForm";
import DebtMeter from "./DebtMeter";
import "../styles/DebtsPill.css";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

type Props = {
  refreshKey?: number; // dashboard bumps this after quick-add (payments change balances)
};

function DebtsPill({ refreshKey: outsideRefresh = 0 }: Props) {
  const { user } = useUser();
  const [summary, setSummary] = useState<DebtSummary | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState<React.CSSProperties>({});
  const buttonRef = useRef<HTMLButtonElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);

  // debts don't care about the selected month, just the user (and new debts)
  useEffect(() => {
    if (!user) return;
    let ignore = false;
    api("/debts/summary")
      .then((response) => (response.ok ? response.json() : Promise.reject(response.status)))
      .then((data: DebtSummary) => {
        if (!ignore) setSummary(data);
      })
      .catch(() => {
        if (!ignore) setSummary(null);
      });
    return () => {
      ignore = true;
    };
  }, [user, refreshKey, outsideRefresh]);

  function openForm() {
    const rect = buttonRef.current!.getBoundingClientRect();
    const width = 340;
    setPosition({ top: rect.bottom + 8, left: Math.max(16, rect.right - width) });
    setOpen(true);
  }

  // when open: close on outside click, Escape, or scroll (same as the other popovers)
  useEffect(() => {
    if (!open) return;
    function onPointerDown(e: MouseEvent) {
      const target = e.target as Node;
      if (!popoverRef.current?.contains(target) && !buttonRef.current?.contains(target)) {
        setOpen(false);
      }
    }
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        setOpen(false);
        buttonRef.current?.focus();
      }
    }
    function onScroll() {
      setOpen(false);
    }
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
      window.removeEventListener("scroll", onScroll);
    };
  }, [open]);

  let body;
  if (!user) {
    body = <p className="pill-empty">Sign in to see your debts.</p>;
  } else if (summary === null) {
    body = <p className="pill-empty">Loading…</p>;
  } else if (summary.debtCount === 0) {
    body = <p className="pill-empty">No debts tracked yet. Add one here or on the Debts page.</p>;
  } else {
    body = (
      <div className="debt-body">
        <div className="debt-figure">
          <span className="debt-owed">{currency.format(summary.totalOwed)}</span>
          <span className="debt-label">owed</span>
        </div>
        {summary.loanCount > 0 && (
          <div className="debt-row">
            <span className="debt-row-label">Loans</span>
            <DebtMeter percent={summary.loanPercentPaid} size="small" tone="loan" label="Loans paid off" />
            <span className="debt-row-value"><b>{summary.loanPercentPaid}%</b> paid</span>
          </div>
        )}
        {summary.cardCount > 0 && (
          <div className="debt-row">
            <span className="debt-row-label">Cards</span>
            <DebtMeter percent={summary.cardPercentUsed} size="small" tone="card" label="Credit used" />
            <span className="debt-row-value"><b>{summary.cardPercentUsed}%</b> used</span>
          </div>
        )}
      </div>
    );
  }

  return (
    <>
      <div className="debt-head">
        <h2 className="pill-title">Debts</h2>
        {user && (
          <button
            ref={buttonRef}
            type="button"
            className="debt-add"
            onClick={() => (open ? setOpen(false) : openForm())}
            aria-expanded={open}
          >
            + Add debt
          </button>
        )}
      </div>

      {body}

      {open &&
        createPortal(
          <div ref={popoverRef} className="debt-popover" style={position} role="dialog" aria-label="Add a debt">
            <AddDebtForm
              layout="compact"
              autoFocus
              onSaved={() => {
                setOpen(false);
                setRefreshKey((k) => k + 1);
              }}
            />
          </div>,
          document.body
        )}
    </>
  );
}

export default DebtsPill;
