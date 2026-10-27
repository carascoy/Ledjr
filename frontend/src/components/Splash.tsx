import logo from "../assets/ledjr.svg";
import "../styles/Splash.css";

// shown while we ask the backend "am I logged in?" on page load.
// the logo waits a moment before showing up, so a fast check doesn't blink it on and off
function Splash() {
  return (
    <div className="splash" role="status" aria-label="Loading">
      <img className="splash-logo" src={logo} alt="" />
    </div>
  );
}

export default Splash;
