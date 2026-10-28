import './App.css'
import { Routes, Route } from 'react-router-dom'
import AuthPage from './pages/AuthPage'
import Dashboard from './pages/Dashboard'
import DebtsPage from './pages/DebtsPage'
import RecurringPage from './pages/RecurringPage'
import BudgetPage from './pages/BudgetPage'
import SetupPage from './pages/SetupPage'
import SettingsPage from './pages/SettingsPage'
import NotFound from './pages/NotFound'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import ResetPasswordPage from './pages/ResetPasswordPage'
import TransactionsPage from './pages/TransactionsPage'
import AppLayout from './components/AppLayout'
import UserProvider from './components/UserProvider'

function App() {
  return (
    <UserProvider>
      <Routes>
        <Route path="/" element={<AuthPage />} />
        {/* forgot password, no sign in needed */}
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        {/* every signed-in page goes inside AppLayout (padding + nav), page shows up in its <Outlet /> */}
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/debts" element={<DebtsPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/budget" element={<BudgetPage />} />
          <Route path="/setup" element={<SetupPage />} />
          <Route path="/recurring" element={<RecurringPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>

        {/* anything else */}
        <Route path="*" element={<NotFound />} />
      </Routes>
    </UserProvider>
  )
}

export default App
