import { useEffect } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useUser } from "../context/UserContext";
import NavBar from "./NavBar";
import MonthProvider from "./MonthProvider";
import Splash from "./Splash";
import "../styles/Layout.css";

// wrapper for every signed-in page: padding + nav on top
function AppLayout() {
  const { pathname } = useLocation();
  const { user, checking } = useUser();

  // router keeps your scroll position between pages, reset to the top instead
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  // still asking the backend if the session is good, logo until we know
  if (checking) return <Splash />;
  // every page in here needs you logged in
  if (!user) return <Navigate to="/" replace />;

  return (
    <MonthProvider>
      <div className="app-page">
        <NavBar />
        <Outlet />
      </div>
    </MonthProvider>
  );
}

export default AppLayout;
