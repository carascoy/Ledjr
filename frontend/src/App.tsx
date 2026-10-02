import './App.css'
import { Routes, Route } from 'react-router-dom'
import AuthPage from './pages/AuthPage'
import Dashboard from './pages/Dashboard'
import { UserProvider } from './context/UserContext'

function App() {
  return (
    <UserProvider>
      <Routes>
        <Route path="/" element={<AuthPage />} />
        <Route path="/dashboard" element={<Dashboard />} />
      </Routes>
    </UserProvider>
  )
}

export default App
