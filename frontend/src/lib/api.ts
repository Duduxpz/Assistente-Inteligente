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

  let response: Response
  try {
    response = await fetch(`${apiBaseUrl}${path}`, { ...options, headers })
  } catch (error) {
    throw new ApiError(
      error instanceof Error
        ? `Não foi possível conectar à API. Verifique se o backend está em execução. (${error.message})`
        : 'Não foi possível conectar à API. Verifique se o backend está em execução.',
      0,
    )
  }

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

  let response: Response
  try {
    response = await fetch(`${apiBaseUrl}${path}`, { headers })
  } catch (error) {
    throw new ApiError(
      error instanceof Error
        ? `Não foi possível conectar à API. Verifique se o backend está em execução. (${error.message})`
        : 'Não foi possível conectar à API. Verifique se o backend está em execução.',
      0,
    )
  }

  if (!response.ok) {
    const body = await response.text()
    let message = `A requisição falhou (HTTP ${response.status}).`
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
