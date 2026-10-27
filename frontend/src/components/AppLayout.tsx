import { useEffect } from "react";
import { Outlet, useLocation } from "react-router-dom";
import NavBar from "./NavBar";
import "../styles/Layout.css";

// wrapper for every signed-in page: padding + nav on top
function AppLayout() {
  const { pathname } = useLocation();

  // router keeps your scroll position between pages, reset to the top instead
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
