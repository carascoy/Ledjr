import '../styles/Dashboard.css'
import logo from '../assets/ledjr.svg'
import avatar from '../assets/profile-icon.svg'
import { Link } from 'react-router-dom'

function Dashboard() {

  return (
    <div className="dash-page">

      <nav className="nav-card">
        <Link to="/dashboard" className="nav-logo">
          <img src={logo} alt="Ledjr home" />
        </Link>
        <Link to="/transactions" className="nav-link">Transactions</Link>
        <Link to="/budget" className="nav-link">Budget</Link>
        <Link to="/debts" className="nav-link">Debts</Link>
        <Link to="/recurring" className="nav-link">Recurring</Link>
        <button type="button" className="nav-avatar">
          <img src={avatar} alt="Account menu" />
        </button>
      </nav>

      <main className="dash-grid">
        <section className="pill pill-income">Income vs Expenses</section>
        <section className="pill pill-donut">Spending by Category</section>
        <section className="pill pill-debts">Debts</section>
        <section className="pill pill-recent">Recent Transactions</section>
        <section className="pill pill-upcoming">Upcoming</section>
        <section className="pill pill-add">+ Add</section>
      </main>

    </div>
  )
}

export default Dashboard