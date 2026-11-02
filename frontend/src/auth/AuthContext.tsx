import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError, apiRequest, login as loginRequest, register as registerRequest, type AuthUser } from '../lib/api'
import { AuthContext, type AuthContextValue } from './context'

const accessTokenKey = 'aether.accessToken'

function getStoredAccessToken() {
  const sessionToken = sessionStorage.getItem(accessTokenKey)
  if (sessionToken) {
    localStorage.setItem(accessTokenKey, sessionToken)
    sessionStorage.removeItem(accessTokenKey)
    return sessionToken
  }
  return localStorage.getItem(accessTokenKey)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(getStoredAccessToken)
  const [user, setUser] = useState<AuthContextValue['user']>(null)
  const [resolvedToken, setResolvedToken] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [retryCount, setRetryCount] = useState(0)
  const loading = Boolean(token && resolvedToken !== token)

  const logout = useCallback(() => {
    localStorage.removeItem(accessTokenKey)
    sessionStorage.removeItem(accessTokenKey)
    setToken(null)
    setUser(null)
    setError(null)
    setResolvedToken(null)
  }, [])

  useEffect(() => {
    function handleUnauthorized(event: Event) {
      if (event instanceof CustomEvent && event.detail === token) logout()
    }

    window.addEventListener('aether:unauthorized', handleUnauthorized)
    return () => window.removeEventListener('aether:unauthorized', handleUnauthorized)
  }, [logout, token])

  const reloadUser = useCallback(() => {
    setError(null)
    setResolvedToken(null)
    setRetryCount((count) => count + 1)
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
          localStorage.removeItem(accessTokenKey)
          sessionStorage.removeItem(accessTokenKey)
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
  }, [token, retryCount])

  const authenticate = useCallback(async (result: ReturnType<typeof loginRequest>) => {
    const auth = await result
    localStorage.setItem(accessTokenKey, auth.accessToken)
    sessionStorage.removeItem(accessTokenKey)
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
