import { createContext, useContext } from 'react'
import type { AppUser } from '../types/AppUser'

// who's logged in. provider lives in components/UserProvider.tsx (split so fast refresh doesn't complain)

export type UserContextType = {
  user: AppUser | null
  setUser: (user: AppUser | null) => void
  // true while we ask the backend "am I logged in?" on page load
  checking: boolean
  signOut: () => Promise<void>
}

export const UserContext = createContext<UserContextType | undefined>(undefined)

export function useUser() {
  const context = useContext(UserContext)
  if (context === undefined) {
    throw new Error('useUser must be used inside a UserProvider')
  }
  return context
}
