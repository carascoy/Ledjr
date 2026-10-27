import { useEffect, useState } from "react";
import { Link, NavLink } from "react-router-dom";
import logo from "../assets/ledjr.svg";
import avatar from "../assets/profile-icon.svg";

const LINKS = [
  { to: "/transactions", label: "Transactions" },
  { to: "/budget", label: "Budget" },
  { to: "/debts", label: "Debts" },
  { to: "/recurring", label: "Recurring" },
];

const linkClass = ({ isActive }: { isActive: boolean }) => `nav-link ${isActive ? "is-active" : ""}`;

function NavBar() {
  const [scrolled, setScrolled] = useState(false);

  // frost the nav once stuff scrolls under it
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 40);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <nav className={`nav-card ${scrolled ? "scrolled" : ""}`}>
      <Link to="/dashboard" className="nav-logo">
        <img src={logo} alt="Ledjr home" />
      </Link>
      {LINKS.map((link) => (
        <NavLink key={link.to} to={link.to} className={linkClass}>
          {link.label}
        </NavLink>
      ))}
      <button type="button" className="nav-avatar">
        <img src={avatar} alt="Account menu" />
      </button>
    </nav>
  );
}

export default NavBar;
