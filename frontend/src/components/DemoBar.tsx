import { useUser } from "../context/UserContext";

function DemoBar() {
  const { user, signOut } = useUser();
  if (!user?.demo) return null;

  async function makeAccount() {
    await signOut();
    // full reload so nothing from the demo session hangs around
    window.location.assign("/?mode=signup");
  }

  return (
    <div className="demo-bar" role="status">
      <p className="demo-bar-text">
        <strong>You're exploring a demo.</strong> Everything here is made up and gets deleted after 24 hours.
      </p>
      <button type="button" className="demo-bar-btn" onClick={makeAccount}>
        Make my own account
      </button>
    </div>
  );
}

export default DemoBar;
