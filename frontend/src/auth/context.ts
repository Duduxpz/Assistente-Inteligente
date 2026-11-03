import { createContext } from 'react'
import type { AuthUser } from '../lib/api'

export interface AuthContextValue {
  token: string | null
  user: AuthUser | null
  loading: boolean
  error: string | null
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, displayName: string) => Promise<void>
  updateProfile: (displayName: string) => Promise<void>
  logout: () => Promise<void>
  reloadUser: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
