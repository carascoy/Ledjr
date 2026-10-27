import { useEffect } from "react";
import { Outlet, useLocation } from "react-router-dom";
import NavBar from "./NavBar";
import "../styles/Layout.css";

// The shell every signed-in page shares: page padding + the nav on top.
// <Outlet /> is where React Router puts the current page (Dashboard, Debts, ...).
function AppLayout() {
  const { pathname } = useLocation();

  // React Router keeps the scroll position between pages; start each page at the top instead.
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return (
    <div className="app-page">
      <NavBar />
      <Outlet />
    </div>
  );
}

export default AppLayout;
