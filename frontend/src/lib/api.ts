const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export class ApiError extends Error {
  readonly status: number

  constructor(
    message: string,
    status: number,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

function notifyUnauthorized(token: string | null | undefined) {
  if (token && typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('aether:unauthorized', { detail: token }))
  }
}

let pendingRefresh: Promise<AuthResponse | null> | null = null

function refreshAccessToken(): Promise<AuthResponse | null> {
  if (pendingRefresh) return pendingRefresh

  pendingRefresh = fetch(`${apiBaseUrl}/auth/refresh`, {
    method: 'POST',
    credentials: 'include',
  })
    .then(async (response) => {
      if (response.status === 401) return null
      const body = await response.text()
      let data: unknown
      if (body) {
        try {
          data = JSON.parse(body)
        } catch {
          throw new ApiError(`A API retornou uma resposta inválida (HTTP ${response.status}).`, response.status)
        }
      }
      if (!response.ok) {
        const message = typeof data === 'object' && data !== null && 'message' in data
          ? String(data.message)
          : `A requisição falhou (HTTP ${response.status}).`
        throw new ApiError(message, response.status)
      }
      const auth = data as AuthResponse
      window.dispatchEvent(new CustomEvent('aether:token-refreshed', { detail: auth }))
      return auth
    })
    .catch((error: unknown) => {
      if (error instanceof ApiError) throw error
      throw new ApiError(
        error instanceof Error
          ? `Não foi possível renovar a sessão. Verifique a conexão com a API. (${error.message})`
          : 'Não foi possível renovar a sessão. Verifique a conexão com a API.',
        0,
      )
    })
    .finally(() => {
      pendingRefresh = null
    })

  return pendingRefresh
}

async function fetchApi(
  path: string,
  options: RequestInit,
  headers: Headers,
  token?: string | null,
): Promise<Response> {
  const request = () => fetch(`${apiBaseUrl}${path}`, {
    ...options,
    headers,
    credentials: options.credentials ?? 'include',
  })

  let response: Response
  try {
    response = await request()
  } catch (error) {
    throw new ApiError(
      error instanceof Error
        ? `Não foi possível conectar à API. Verifique se o backend está em execução. (${error.message})`
        : 'Não foi possível conectar à API. Verifique se o backend está em execução.',
      0,
    )
  }

  if (response.status === 401
    && token
    && !['/auth/login', '/auth/register', '/auth/refresh'].includes(path)) {
    const refreshed = await refreshAccessToken()
    if (refreshed) {
      headers.set('Authorization', `Bearer ${refreshed.accessToken}`)
      try {
        response = await request()
      } catch (error) {
        throw new ApiError(
          error instanceof Error
            ? `Não foi possível conectar à API. Verifique se o backend está em execução. (${error.message})`
            : 'Não foi possível conectar à API. Verifique se o backend está em execução.',
          0,
        )
      }
    }
  }

  if (response.status === 401) notifyUnauthorized(token)
  return response
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null,
): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetchApi(path, options, headers, token)

  const body = await response.text()
  let data: unknown
  if (body) {
    try {
      data = JSON.parse(body)
    } catch {
      throw new ApiError(`A API retornou uma resposta inválida (HTTP ${response.status}).`, response.status)
    }
  }

  if (!response.ok) {
    const message = token && response.status === 401
      ? 'Sua sessão expirou. Entre novamente para continuar.'
      : typeof data === 'object' && data !== null && 'message' in data
      ? String(data.message)
      : `A requisição falhou (HTTP ${response.status}).`
    throw new ApiError(message, response.status)
  }

  return data as T
}

export interface AuthUser {
  id: string
  email: string
  displayName: string
  roles: string[]
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresAt: string
  user: AuthUser
}

export interface Task {
  id: string
  title: string
  description: string | null
  priority: 'low' | 'medium' | 'high'
  completed: boolean
  dueAt: string | null
  createdAt: string
  updatedAt: string
}

export interface Conversation {
  id: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  createdAt: string
}

export interface DocumentItem {
  id: string
  fileName: string
  contentType: string
  sizeBytes: number
  createdAt: string
}

export interface UserSettings {
  theme: 'dark' | 'light' | 'system'
  locale: 'pt-BR' | 'en-US' | 'es-ES'
}

export interface PlanDetails {
  key: 'free' | 'plus' | 'ultra'
  name: string
  description: string
  monthlyPriceCents: number
  monthlyMessages: number
  activeTaskLimit: number | null
  conversationLimit: number | null
  documentStorageBytes: number
}

export interface PlanCatalog {
  currentPlanKey: PlanDetails['key']
  plans: PlanDetails[]
}

export interface BillingStatus {
  planKey: PlanDetails['key']
  subscriptionStatus: string | null
  currentPeriodEnd: string | null
  cancellationScheduled: boolean
  testModeEnabled: boolean
  testSubscription: boolean
}

export interface CheckoutResponse {
  checkoutUrl: string
}

export interface UsageSummary {
  totals: {
    inputTokens: number
    outputTokens: number
    requests: number
  }
  recentActivity: Array<{
    operation: string
    inputTokens: number
    outputTokens: number
    createdAt: string
  }>
}

export interface AdminUser {
  id: string
  email: string
  displayName: string
  enabled: boolean
}

export interface AdminUserPage {
  content: AdminUser[]
  number: number
  totalPages: number
  totalElements: number
}

export async function apiDownload(path: string, token: string | null): Promise<Blob> {
  const headers = new Headers()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetchApi(path, {}, headers, token)

  if (!response.ok) {
    const body = await response.text()
    let message = token && response.status === 401
      ? 'Sua sessão expirou. Entre novamente para continuar.'
      : `A requisição falhou (HTTP ${response.status}).`
    if (body) {
      try {
        const data: unknown = JSON.parse(body)
        if (typeof data === 'object' && data !== null && 'message' in data) {
          message = String(data.message)
        }
      } catch {
        throw new ApiError(`A API retornou uma resposta inválida (HTTP ${response.status}).`, response.status)
      }
    }
    throw new ApiError(message, response.status)
  }
  return response.blob()
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  return apiRequest<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

export async function register(
  email: string,
  password: string,
  displayName: string,
): Promise<AuthResponse> {
  return apiRequest<AuthResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password, displayName }),
  })
}
