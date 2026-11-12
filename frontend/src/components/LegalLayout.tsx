import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import logo from "../assets/ledjr.svg";
import "../styles/LegalPage.css";

export const LEGAL_UPDATED = "November 12, 2026";
export const CONTACT_EMAIL = "support@ledjr.dev";

function LegalLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="legal">
      <Link to="/" className="legal-home" aria-label="Ledjr home">
        <img src={logo} alt="" className="legal-logo" />
        <span>Ledjr</span>
      </Link>

      <article className="legal-body">
        <h1 className="legal-title">{title}</h1>
        <p className="legal-updated">Last updated {LEGAL_UPDATED}</p>
        {children}
      </article>

      <footer className="legal-footer">
        <Link to="/privacy">Privacy Policy</Link>
        <span aria-hidden="true">·</span>
        <Link to="/terms">Terms of Service</Link>
        <span aria-hidden="true">·</span>
        <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>
      </footer>
    </div>
  );
}

export default LegalLayout;
