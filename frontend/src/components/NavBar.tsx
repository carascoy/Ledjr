import { useEffect, useRef, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useUser } from "../context/UserContext";
import logo from "../assets/ledjr.svg";
import avatar from "../assets/profile-icon.svg";
import settingsIcon from "../assets/settings-icon.svg";
import exitIcon from "../assets/exit-icon.svg";

const LINKS = [
  { to: "/transactions", label: "Transactions" },
  { to: "/budget", label: "Budget" },
  { to: "/debts", label: "Debts" },
  { to: "/recurring", label: "Recurring" },
];

const linkClass = ({ isActive }: { isActive: boolean }) => `nav-link ${isActive ? "is-active" : ""}`;

function NavBar() {
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const { user, signOut } = useUser();
  const navigate = useNavigate();

  // close the account menu on outside click or Escape
  useEffect(() => {
    if (!menuOpen) return;
    const onClick = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setMenuOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onClick);
      document.removeEventListener("keydown", onKey);
    };
  }, [menuOpen]);

  async function handleSignOut() {
    setMenuOpen(false);
    await signOut();
    navigate("/", { replace: true });
  }

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
      <div className="nav-account" ref={menuRef}>
        <button
          type="button"
          className="nav-avatar"
          aria-haspopup="menu"
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((o) => !o)}
        >
          <img src={avatar} alt="Account menu" />
        </button>
        {menuOpen && (
          <div className="nav-menu" role="menu">
            {user && (
              <div className="nav-menu-who">
                <b>
                  {user.firstName} {user.lastName}
                </b>
                <span>{user.email}</span>
              </div>
            )}
            <button
              type="button"
              role="menuitem"
              className="nav-menu-item"
              onClick={() => {
                setMenuOpen(false);
                navigate("/settings");
              }}
            >
              <img src={settingsIcon} alt="" />
              Settings
            </button>
            <button type="button" role="menuitem" className="nav-menu-item" onClick={handleSignOut}>
              <img src={exitIcon} alt="" />
              Sign out
            </button>
          </div>
        )}
      </div>
    </nav>
  );
}

export default NavBar;
