import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import App from './App'
import { AuthPage } from './auth/AuthPage'

const mocks = vi.hoisted(() => ({
  useAuth: vi.fn(),
  apiRequest: vi.fn(),
  streamChatMessage: vi.fn(),
  login: vi.fn(),
}))

vi.mock('./auth/useAuth', () => ({ useAuth: mocks.useAuth }))
vi.mock('./lib/api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./lib/api')>()
  return {
    ...actual,
    apiRequest: mocks.apiRequest,
    streamChatMessage: mocks.streamChatMessage,
    apiDownload: vi.fn(),
  }
})

const user = {
  id: 'user-1',
  email: 'person@example.com',
  displayName: 'Test User',
  roles: ['ROLE_USER'],
}

function renderApp(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('AETHER AI core flows', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    let conversationMessages: {
      id: string
      role: 'user' | 'assistant'
      content: string
      createdAt: string
    }[] = []
    mocks.useAuth.mockReturnValue({
      token: 'test-token',
      user,
      loading: false,
      error: null,
      login: mocks.login,
      register: vi.fn(),
      updateProfile: vi.fn(),
      logout: vi.fn(),
      reloadUser: vi.fn(),
    })
    mocks.apiRequest.mockImplementation(async (path: string, options: RequestInit = {}) => {
      if (path === '/users/me/settings') return { theme: 'dark', locale: 'pt-BR' }
      if (path === '/ai/status') return { configured: true }
      if (path === '/plans') return { currentPlanKey: 'free', plans: [] }
      if (path === '/billing') return {
        planKey: 'free',
        subscriptionStatus: null,
        currentPeriodEnd: null,
        cancellationScheduled: false,
        testModeEnabled: false,
        testSubscription: false,
      }
      if (path === '/tasks' && !options.method) return []
      if (path === '/conversations' && !options.method) return []
      if (path === '/conversations' && options.method === 'POST') {
        return {
          id: 'conversation-1',
          title: 'Nova conversa',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        }
      }
      if (path === '/conversations/conversation-1/messages') return conversationMessages
      if (path === '/tasks' && options.method === 'POST') {
        return {
          id: 'task-1',
          title: JSON.parse(String(options.body)).title,
          description: null,
          priority: 'medium',
          completed: false,
          dueAt: null,
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        }
      }
      return []
    })
    mocks.streamChatMessage.mockImplementation(async (
      _path: string,
      content: string,
      _token: string,
      _signal: AbortSignal,
      onToken: (token: string) => void,
    ) => {
      conversationMessages = [
        {
          id: 'message-1',
          role: 'user',
          content,
          createdAt: '2026-01-01T00:00:00Z',
        },
        {
          id: 'message-2',
          role: 'assistant',
          content: 'Olá! Como posso ajudar?',
          createdAt: '2026-01-01T00:00:01Z',
        },
      ]
      onToken('Olá! ')
      onToken('Como posso ajudar?')
      return { messages: conversationMessages }
    })
  })

  it('submits the login form', async () => {
    const testUser = userEvent.setup()
    mocks.useAuth.mockReturnValue({
      token: null,
      user: null,
      loading: false,
      login: mocks.login,
      register: vi.fn(),
    })
    mocks.login.mockResolvedValue(undefined)

    render(
      <MemoryRouter>
        <AuthPage mode="login" />
      </MemoryRouter>,
    )

    await testUser.type(screen.getByLabelText('E-mail'), 'person@example.com')
    await testUser.type(screen.getByLabelText('Senha'), 'correct-horse-battery')
    await testUser.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => expect(mocks.login).toHaveBeenCalledWith(
      'person@example.com',
      'correct-horse-battery',
    ))
  })

  it('creates a task and renders it in the task list', async () => {
    const testUser = userEvent.setup()
    renderApp('/tasks')

    await testUser.type(await screen.findByPlaceholderText('O que você precisa fazer?'), 'Revisar proposta')
    await testUser.click(screen.getByRole('button', { name: 'Adicionar' }))

    expect(await screen.findByText('Revisar proposta')).toBeInTheDocument()
    expect(mocks.apiRequest).toHaveBeenCalledWith(
      '/tasks',
      expect.objectContaining({ method: 'POST' }),
      'test-token',
    )
  })

  it('creates a conversation and sends a message', async () => {
    const testUser = userEvent.setup()
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText },
    })
    renderApp('/chat')

    await testUser.type(await screen.findByLabelText('Escreva sua mensagem'), 'Olá, assistente')
    await testUser.click(screen.getByRole('button', { name: 'Enviar mensagem' }))

    await waitFor(() => expect(mocks.streamChatMessage).toHaveBeenCalledWith(
      '/conversations/conversation-1/messages/stream',
      'Olá, assistente',
      'test-token',
      expect.any(AbortSignal),
      expect.any(Function),
    ))
    expect(await screen.findByText('Olá, assistente')).toBeInTheDocument()
    expect(await screen.findByText(/Olá! Como posso ajudar/, {}, { timeout: 5000 })).toBeInTheDocument()
    await testUser.click(screen.getByRole('button', { name: 'Copiar resposta' }))
    expect(writeText).toHaveBeenCalledWith('Olá! Como posso ajudar?')

    await testUser.click(screen.getByRole('button', { name: 'Regenerar resposta' }))
    await waitFor(() => expect(mocks.streamChatMessage).toHaveBeenLastCalledWith(
      '/conversations/conversation-1/messages/message-2/regenerate/stream',
      '',
      'test-token',
      expect.any(AbortSignal),
      expect.any(Function),
    ))
  }, 15000)
})
