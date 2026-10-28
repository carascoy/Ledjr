import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import type { AppUser } from '../types/AppUser'
import { api, resetCsrf, SIGNED_OUT_EVENT } from '../api/client'
import { UserContext } from '../context/UserContext'

function UserProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<AppUser | null>(null)
  const [checking, setChecking] = useState(true)

  // any login/logout means a new session, so the old CSRF token is dead too
  const setUser = (next: AppUser | null) => {
    resetCsrf()
    setUserState(next)
  }

  // on page load, the session cookie might still be good -> GET /me says who we are.
  // this is what makes refreshing the page NOT log you out anymore
  useEffect(() => {
    let ignore = false
    api('/me')
      .then((response) => (response.ok ? response.json() : null))
      .then((data: AppUser | null) => {
        if (!ignore) setUserState(data)
      })
      .catch(() => {
        if (!ignore) setUserState(null)
      })
      .finally(() => {
        if (!ignore) setChecking(false)
      })
    return () => {
      ignore = true
    }
  }, [])

  // api() fires this on any 401 (session expired / logged out somewhere else)
  useEffect(() => {
    const onSignedOut = () => setUserState(null)
    window.addEventListener(SIGNED_OUT_EVENT, onSignedOut)
    return () => window.removeEventListener(SIGNED_OUT_EVENT, onSignedOut)
  }, [])

  async function signOut() {
    try {
      await api('/signout', { method: 'POST' })
    } finally {
      setUser(null)
    }
  }

  return (
    <UserContext.Provider value={{ user, setUser, checking, signOut }}>
      {children}
    </UserContext.Provider>
  )
}

export default UserProvider
