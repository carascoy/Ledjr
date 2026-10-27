import './App.css'
import { Routes, Route } from 'react-router-dom'
import AuthPage from './pages/AuthPage'
import Dashboard from './pages/Dashboard'
import DebtsPage from './pages/DebtsPage'
import TransactionsPage from './pages/TransactionsPage'
import ComingSoon from './pages/ComingSoon'
import AppLayout from './components/AppLayout'
import { UserProvider } from './context/UserContext'

function App() {
  return (
    <UserProvider>
      <Routes>
        <Route path="/" element={<AuthPage />} />

        {/* every signed-in page goes inside AppLayout (padding + nav), page shows up in its <Outlet /> */}
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/debts" element={<DebtsPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route
            path="/budget"
            element={<ComingSoon title="Budget" description="Category envelopes and monthly budgets live here soon. Your categories already work in quick-add." />}
          />
          <Route
            path="/recurring"
            element={<ComingSoon title="Recurring" description="Recurring bills and paychecks, with what's due next, are coming here." />}
          />
        </Route>
      </Routes>
    </UserProvider>
  )
}

export default App
