import { useEffect } from "react";
import { Link } from "react-router-dom";
import { useUser } from "../context/UserContext";
import logo from "../assets/ledjr.svg";
import "../styles/Layout.css";

// any URL that isn't a page
function NotFound() {
  const { user } = useUser();

  useEffect(() => {
    document.title = "Page not found · Ledjr";
  }, []);

  return (
    <main className="not-found">
      <img src={logo} alt="" className="not-found-logo" />
      <h1 className="page-title">Page not found</h1>
      <p className="not-found-text">That page doesn't exist. Maybe a typo in the link?</p>
      <Link to={user ? "/dashboard" : "/"} className="btn-primary not-found-link">
        {user ? "Back to dashboard" : "Go to sign in"}
      </Link>
    </main>
  );
}

export default NotFound;
