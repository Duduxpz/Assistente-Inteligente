import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError, apiRequest, login as loginRequest, register as registerRequest, type AuthUser } from '../lib/api'
import { AuthContext, type AuthContextValue } from './context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => sessionStorage.getItem('aether.accessToken'))
  const [user, setUser] = useState<AuthContextValue['user']>(null)
  const [resolvedToken, setResolvedToken] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const loading = Boolean(token && resolvedToken !== token)

  const logout = useCallback(() => {
    sessionStorage.removeItem('aether.accessToken')
    setToken(null)
    setUser(null)
    setError(null)
    setResolvedToken(null)
  }, [])

  const reloadUser = useCallback(() => {
    setError(null)
    setResolvedToken(null)
  }, [])

  const updateProfile = useCallback(async (displayName: string) => {
    if (!token) throw new Error('You must be signed in to update your profile.')
    const updatedUser = await apiRequest<AuthUser>('/users/me', {
      method: 'PUT',
      body: JSON.stringify({ displayName }),
    }, token)
    setUser(updatedUser)
  }, [token])

  useEffect(() => {
    if (!token) return

    let active = true
    void apiRequest<AuthUser>('/users/me', {}, token)
      .then((currentUser) => {
        if (!active) return
        setUser(currentUser)
        setError(null)
      })
      .catch((requestError: unknown) => {
        if (!active) return
        if (requestError instanceof ApiError && requestError.status === 401) {
          sessionStorage.removeItem('aether.accessToken')
          setToken(null)
          setUser(null)
        } else {
          setUser(null)
          setError(requestError instanceof Error ? requestError.message : 'Não foi possível carregar sua conta.')
        }
      })
      .finally(() => {
        if (active) setResolvedToken(token)
      })

    return () => { active = false }
  }, [token, resolvedToken])

  const authenticate = useCallback(async (result: ReturnType<typeof loginRequest>) => {
    const auth = await result
    sessionStorage.setItem('aether.accessToken', auth.accessToken)
    setToken(auth.accessToken)
    setResolvedToken(auth.accessToken)
    setUser(auth.user)
    setError(null)
  }, [])

  const value = useMemo(() => ({
    token,
    user,
    loading,
    error,
    login: async (email: string, password: string) => authenticate(loginRequest(email, password)),
    register: async (email: string, password: string, displayName: string) =>
      authenticate(registerRequest(email, password, displayName)),
    updateProfile,
    logout,
    reloadUser,
  }), [token, user, loading, error, authenticate, updateProfile, logout, reloadUser])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
