import { Link } from "react-router-dom";
import LegalLayout, { CONTACT_EMAIL } from "../components/LegalLayout";

function PrivacyPage() {
  const email = <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>;

  return (
    <LegalLayout title="Privacy Policy">
      <p className="legal-lead">
        Ledjr is a personal finance app built and run by one person, Yan Carasco ("I", "me"). You're trusting it with
        information about your money, so this page explains in plain English exactly what Ledjr keeps, why, who else
        touches it, and how to get rid of it.
      </p>

      <div className="legal-summary">
        <h2>The short version</h2>
        <ul>
          <li>Ledjr only knows what you type into it. It never connects to your bank.</li>
          <li>Your data is used to run Ledjr for you, and nothing else. It's never sold, rented or used for ads.</li>
          <li>There are no ads, no tracking cookies and no third-party analytics scripts.</li>
          <li>You can delete your account and everything in it at any time from Settings.</li>
        </ul>
      </div>

      <h2>1. What Ledjr collects</h2>
      <h3>Information you give it</h3>
      <ul>
        <li><strong>Account details:</strong> your first name, last name, email address and password. Your password is
          stored as a one-way hash (BCrypt), never as the actual text, so nobody, including me, can read it.</li>
        <li><strong>Your financial entries:</strong> the transactions, categories, budgets, recurring bills and
          paychecks, and debts you add, including amounts, dates, descriptions, balances, interest rates, credit
          limits and due dates.</li>
      </ul>
      <p>Ledjr doesn't ask for and doesn't want bank logins, card numbers, account numbers or Social Security numbers.
        Please don't put them in descriptions or names.</p>

      <h3>Information collected automatically</h3>
      <ul>
        <li><strong>Account activity times:</strong> when your account was created and roughly when you last used it
          (updated at most once an hour).</li>
        <li><strong>A login session:</strong> while you're signed in, the server keeps a session record linked to your
          account. It ends when you sign out, or after a day without using Ledjr.</li>
        <li><strong>IP addresses, briefly:</strong> to stop people guessing passwords, Ledjr counts failed sign-ins per
          email and per IP address for 15 minutes, and counts demo starts per IP address for an hour. These counts are
          kept in memory only and are never written to the database.</li>
        <li><strong>Server logs:</strong> the server records events like "transaction added" or "category deleted"
          along with your account number, plus standard request information from the hosting provider (such as IP
          address and time). Logs are kept for a short time to fix bugs and spot abuse.</li>
      </ul>

      <h3>Demo accounts</h3>
      <p>"Try the demo" creates a temporary account filled with made-up data. It asks for nothing about you and is
        deleted automatically after 24 hours. Ledjr keeps only the time each demo was started, to count how many
        demos have been used.</p>

      <h2>2. How your information is used</h2>
      <ul>
        <li>To run Ledjr: show your dashboard, budgets, calendar and debt plans, and do the math behind them.</li>
        <li>To keep your account secure: sign-in, sessions, password resets and rate limiting.</li>
        <li>To send emails you ask for, like a password reset link. Ledjr doesn't send marketing email.</li>
        <li>To understand overall use, like how many people signed up this week or how many demos were tried. I look
          at totals and first names, not at what you've entered.</li>
        <li>To fix bugs and prevent abuse.</li>
      </ul>
      <p>I don't look at the details of your transactions, budgets or debts unless you ask me to help with your account,
        or it's necessary to investigate a security problem or comply with the law.</p>

      <h2>3. Who else handles your information</h2>
      <p>Ledjr uses a few services to run. They process data only to provide their service to Ledjr:</p>
      <ul>
        <li><strong>Heroku</strong> (Salesforce) runs Ledjr's server and stores its database, in the United States.</li>
        <li><strong>Cloudflare</strong> delivers the website and manages the ledjr.dev domain. Every visit passes
          through its network.</li>
        <li><strong>Resend</strong> sends Ledjr's emails, like password reset links. It receives your email address and
          the message.</li>
      </ul>
      <p>Your information is never sold or shared with advertisers or data brokers. I would only disclose it if required
        by law, to protect someone's safety, or if Ledjr were ever transferred to someone else, in which case this
        policy would still apply to your data and you'd be told first.</p>

      <h2>4. Cookies</h2>
      <p>Ledjr uses one cookie: the session cookie that keeps you signed in. It's marked HttpOnly (page scripts can't
        read it) and SameSite (other websites can't send it along), and it only travels over HTTPS. There are no
        advertising, tracking or analytics cookies, which is why there's no cookie banner.</p>

      <h2>5. How long it's kept</h2>
      <ul>
        <li>Your account and entries are kept until you delete them or your account.</li>
        <li>Password reset links expire after 30 minutes and only work once.</li>
        <li>Demo accounts are deleted after 24 hours.</li>
        <li>Server logs are kept for a short time and then discarded.</li>
        <li>If database backups are kept, deleted data can remain in them for up to 30 days before they're replaced.</li>
      </ul>

      <h2>6. Security</h2>
      <p>Everything travels over HTTPS. Passwords are hashed, sessions are stored on the server, every change requires a
        security token that other websites can't get, and repeated wrong passwords are slowed down. No system is
        perfectly secure, though. If a breach ever affected your information, I'd tell you by email as soon as
        reasonably possible and explain what happened.</p>

      <h2>7. Your choices and rights</h2>
      <ul>
        <li><strong>See and change:</strong> everything Ledjr stores about you is visible in the app, and you can edit
          your name, email, password and every entry.</li>
        <li><strong>Delete:</strong> Settings → Delete account removes your account and everything in it right away.</li>
        <li><strong>Get a copy:</strong> email {email} and I'll send you your data in a common format.</li>
        <li><strong>Ask a question or object:</strong> email {email}.</li>
      </ul>
      <p>These apply to everyone, wherever you live. Depending on your location, privacy laws (like the CCPA in
        California or the GDPR in Europe) may give you additional rights, and I'll honor them. I'll respond to requests
        within 30 days.</p>

      <h2>8. Children</h2>
      <p>Ledjr isn't for children under 13, and I don't knowingly collect information from them. If you believe a child
        under 13 has made an account, email {email} and it'll be deleted.</p>

      <h2>9. Changes to this policy</h2>
      <p>If this policy changes, the date at the top will change. If it's a significant change to how your information
        is used, I'll let you know by email or in the app before it takes effect.</p>

      <h2>10. Contact</h2>
      <p>Questions about privacy or your data: {email}. See also the <Link to="/terms">Terms of Service</Link>.</p>
    </LegalLayout>
  );
}

export default PrivacyPage;
