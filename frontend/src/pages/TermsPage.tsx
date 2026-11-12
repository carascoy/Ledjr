import { Link } from "react-router-dom";
import LegalLayout, { CONTACT_EMAIL } from "../components/LegalLayout";

function TermsPage() {
  const email = <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>;

  return (
    <LegalLayout title="Terms of Service">
      <p className="legal-lead">
        These terms are the agreement between you and Yan Carasco ("I", "me"), who builds and runs Ledjr. By creating
        an account or using Ledjr, including the demo, you agree to them. If you don't agree, please don't use Ledjr.
      </p>

      <div className="legal-summary">
        <h2>The short version</h2>
        <ul>
          <li>Ledjr is free, and it's a tool for organizing your own numbers. It's not financial advice.</li>
          <li>Your data is yours. You can delete it whenever you want.</li>
          <li>Don't misuse Ledjr or try to break it.</li>
          <li>It's provided as-is by one person, so double-check anything important.</li>
        </ul>
      </div>

      <h2>1. Who can use Ledjr</h2>
      <p>You must be at least 13 years old. If you're under 18, you need permission from a parent or guardian. You
        agree to give accurate sign-up information and to use Ledjr only for your own personal finances.</p>

      <h2>2. Your account</h2>
      <p>You're responsible for keeping your password safe and for what happens in your account. Use a password you don't
        use anywhere else. If you think someone else has gotten into your account, change your password (which signs
        out every device) and email {email}.</p>

      <h2>3. Not financial advice</h2>
      <p>Ledjr helps you track and organize information you enter. Its estimates, like how much you can budget, budget
        suggestions, payoff plans and interest totals, are calculations based on that information and on simplifying
        assumptions. They can be incomplete or wrong, and they aren't financial, tax, legal or investment advice. Ledjr
        doesn't connect to your bank, so it only knows what you tell it. Always check important numbers against your
        actual statements, and talk to a qualified professional before making big financial decisions. You're
        responsible for the decisions you make.</p>

      <h2>4. Your content</h2>
      <p>Everything you enter stays yours. You give me permission to store and process it only as needed to run Ledjr for
        you, as described in the <Link to="/privacy">Privacy Policy</Link>. You can delete individual entries or your
        whole account at any time.</p>

      <h2>5. The demo</h2>
      <p>"Try the demo" creates a temporary account with made-up data that's deleted after 24 hours. Some features, like
        changing the email or password, are turned off in demos. Don't enter real personal or financial information into
        a demo.</p>

      <h2>6. Acceptable use</h2>
      <p>You agree not to:</p>
      <ul>
        <li>break the law or use Ledjr to store information you don't have the right to;</li>
        <li>access or try to access other people's accounts or data;</li>
        <li>probe, scan, attack or overload Ledjr, or get around its security or rate limits;</li>
        <li>create accounts or demos automatically, in bulk, or to spam;</li>
        <li>resell Ledjr or offer it to others as your own service.</li>
      </ul>
      <p>If you find a security problem, please report it to {email} instead of exploiting it. I appreciate it.</p>

      <h2>7. Ending your account</h2>
      <p>You can delete your account anytime in Settings. I may suspend or delete accounts that break these terms or put
        Ledjr or other people at risk, and I may delete accounts that have been inactive for more than two years after
        emailing a warning first.</p>

      <h2>8. Changes to Ledjr and to these terms</h2>
      <p>Ledjr is a personal project, so features may change and it may not be available every minute. If I ever decide
        to shut it down, I'll give at least 30 days' notice by email so you can get a copy of your data. If these terms
        change in an important way, I'll let you know before the change takes effect. Continuing to use Ledjr after that
        means you accept the new terms.</p>

      <h2>9. No warranties</h2>
      <p>Ledjr is provided "as is" and "as available", without warranties of any kind, express or implied, including
        warranties of merchantability, fitness for a particular purpose, accuracy and non-infringement. I don't promise
        that it will be error-free, uninterrupted or that data will never be lost, so keep your own records of anything
        important.</p>

      <h2>10. Limitation of liability</h2>
      <p>To the fullest extent the law allows, I'm not liable for any indirect, incidental, special, consequential or
        punitive damages, or for lost money, profits, data or opportunities, arising from your use of Ledjr or reliance
        on its calculations. My total liability for any claim related to Ledjr is limited to $50. Some places don't
        allow these limits, so they may not all apply to you.</p>

      <h2>11. Indemnity</h2>
      <p>If you misuse Ledjr or break these terms and that leads to a claim against me, you agree to cover the reasonable
        costs of that claim.</p>

      <h2>12. Governing law</h2>
      <p>These terms are governed by the laws of the Commonwealth of Massachusetts, USA, without regard to conflict-of-law
        rules. Any dispute will be handled in the state or federal courts located in Massachusetts, unless the law where
        you live gives you the right to bring it there.</p>

      <h2>13. Everything else</h2>
      <p>These terms and the Privacy Policy are the whole agreement between us about Ledjr. If part of these terms can't be
        enforced, the rest still applies. Not enforcing a part right away doesn't mean giving it up.</p>

      <h2>14. Contact</h2>
      <p>Questions about these terms: {email}.</p>
    </LegalLayout>
  );
}

export default TermsPage;
