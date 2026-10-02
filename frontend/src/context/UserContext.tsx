import { createContext, useContext, useState } from 'react'
import type { ReactNode } from 'react'
import type { AppUser } from '../types/AppUser'

type UserContextType = {
  user: AppUser | null
  setUser: (user: AppUser | null) => void
}

const UserContext = createContext<UserContextType | undefined>(undefined)

export function UserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AppUser | null>(null)

  return (
    <UserContext.Provider value={{ user, setUser }}>
      {children}
    </UserContext.Provider>
  )
}

export function useUser() {
  const context = useContext(UserContext)
  if (context === undefined) {
    throw new Error('useUser must be used inside a UserProvider')
  }
  return context
}