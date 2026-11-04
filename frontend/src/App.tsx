import {
  lazy,
  Suspense,
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from 'react'
import {
  ArrowDown,
  ArrowRight,
  AlertCircle,
  Bot,
  CalendarDays,
  Check,
  CheckCircle2,
  ChevronDown,
  Circle,
  CreditCard,
  Copy,
  Database,
  Download,
  FileText,
  Globe2,
  FolderOpen,
  LayoutDashboard,
  LoaderCircle,
  LogOut,
  Menu,
  MessageSquareText,
  Plus,
  Pencil,
  RefreshCw,
  Save,
  Send,
  Square,
  Shield,
  Settings2,
  ShieldAlert,
  Sparkles,
  Trash2,
  Upload,
  UsersRound,
  X,
} from 'lucide-react'
import { Navigate, NavLink, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { AuthPage } from './auth/AuthPage'
import { PasswordRecoveryPage } from './auth/PasswordRecoveryPage'
import { useAuth } from './auth/useAuth'
import {
  apiDownload,
  apiRequest,
  ApiError,
  streamChatMessage,
  type AdminUser,
  type AdminMetrics,
  type AdminUserPage,
  type BillingStatus,
  type ChatMessage,
  type CheckoutResponse,
  type Conversation,
  type DocumentItem,
  type PlanCatalog,
  type Task,
  type UserSettings,
} from './lib/api'
import './App.css'

const MarkdownContent = lazy(() => import('./chat/MarkdownContent')
  .then((module) => ({ default: module.MarkdownContent })))

const navigation = [
  { label: 'Visão geral', path: '/', icon: LayoutDashboard, end: true },
  { label: 'Conversas', path: '/chat', icon: MessageSquareText },
  { label: 'Documentos', path: '/documents', icon: FolderOpen },
  { label: 'Tarefas', path: '/tasks', icon: Check },
  { label: 'Planos', path: '/plans', icon: CreditCard },
]

const pageTitles: Record<string, string> = {
  '/': 'Visão geral',
  '/chat': 'Conversas',
  '/documents': 'Documentos',
  '/tasks': 'Tarefas',
  '/plans': 'Planos',
  '/admin': 'Administração',
  '/settings': 'Configurações',
}

let systemThemeMedia: MediaQueryList | null = null
let systemThemeListener: (() => void) | null = null

function applyTheme(theme: UserSettings['theme']) {
  clearSystemThemeListener()

  const media = window.matchMedia('(prefers-color-scheme: dark)')
  const update = () => {
    const isDark = theme === 'dark' || (theme === 'system' && media.matches)
    document.documentElement.dataset.theme = isDark ? 'dark' : 'light'
  }
  update()
  if (theme === 'system') {
    systemThemeMedia = media
    systemThemeListener = update
    media.addEventListener('change', update)
  }
}

function clearSystemThemeListener() {
  if (systemThemeMedia && systemThemeListener) {
    systemThemeMedia.removeEventListener('change', systemThemeListener)
  }
  systemThemeMedia = null
  systemThemeListener = null
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage mode="login" />} />
      <Route path="/admin/login" element={<AuthPage mode="admin" />} />
      <Route path="/register" element={<AuthPage mode="register" />} />
      <Route path="/forgot-password" element={<PasswordRecoveryPage mode="request" />} />
      <Route path="/reset-password" element={<PasswordRecoveryPage mode="reset" />} />
      <Route path="/*" element={<RequireAuthentication><ApplicationShell /></RequireAuthentication>} />
    </Routes>
  )
}

function RequireAuthentication({ children }: { children: ReactNode }) {
  const { token, user, loading, error, reloadUser, logout } = useAuth()
  const location = useLocation()

  if (loading) {
    return <main className="auth-page"><p className="auth-loading">Conectando à sua conta...</p></main>
  }
  if (!token) {
    const loginPath = location.pathname.startsWith('/admin') ? '/admin/login' : '/login'
    return <Navigate to={loginPath} replace state={{ from: location.pathname }} />
  }
  if (!user) {
    return (
      <main className="auth-page">
        <div className="auth-card session-problem">
          <span className="notice-icon"><AlertCircle size={18} /></span>
          <h1>Não foi possível carregar sua conta.</h1>
          <p>{error ?? 'Verifique se o backend está em execução e tente novamente.'}</p>
          <button className="auth-submit" onClick={() => void reloadUser()} type="button">Tentar novamente</button>
          <button className="auth-secondary" onClick={() => void logout().catch(() => undefined)} type="button">
            Sair da conta
          </button>
        </div>
      </main>
    )
  }
  return children
}

function ApplicationShell() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [themeError, setThemeError] = useState('')
  const [logoutError, setLogoutError] = useState('')
  const [aiConfigured, setAIConfigured] = useState<boolean | null>(null)
  const [aiStatusError, setAiStatusError] = useState('')
  const location = useLocation()
  const navigate = useNavigate()
  const { token, user, logout } = useAuth()
  const pageTitle = pageTitles[location.pathname] ?? 'AETHER AI'

  useEffect(() => {
    let active = true
    void apiRequest<UserSettings>('/users/me/settings', {}, token)
      .then(({ theme }) => {
        if (!active) return
        applyTheme(theme)
        setThemeError('')
      })
      .catch(() => {
        if (active) setThemeError('Não foi possível carregar o tema salvo da sua conta.')
      })
    return () => {
      active = false
      clearSystemThemeListener()
    }
  }, [token])

  useEffect(() => {
    let active = true
    void apiRequest<{ configured: boolean }>('/ai/status', {}, token)
      .then(({ configured }) => {
        if (!active) return
        setAIConfigured(configured)
        setAiStatusError('')
      })
      .catch(() => {
        if (active) {
          setAIConfigured(null)
          setAiStatusError('Não foi possível verificar o status da integração de IA.')
        }
      })
    return () => { active = false }
  }, [token])

  function startConversation() {
    setMobileMenuOpen(false)
    navigate('/chat')
  }

  async function handleLogout() {
    setLogoutError('')
    try {
      await logout()
    } catch (requestError) {
      setLogoutError(requestError instanceof Error
        ? requestError.message
        : 'Não foi possível encerrar a sessão.')
    }
  }

  return (
    <div className="app-shell">
      <aside className={`sidebar ${mobileMenuOpen ? 'sidebar-open' : ''}`}>
        <NavLink
          className="brand"
          to="/"
          aria-label="AETHER AI, página inicial"
          onClick={() => setMobileMenuOpen(false)}
        >
          <span className="brand-mark">
            <Sparkles size={19} strokeWidth={2.2} />
          </span>
          <span className="brand-name">AETHER<span>AI</span></span>
        </NavLink>

        <WorkspacePlanMenu token={token} displayName={user?.displayName ?? 'AETHER'} />

        <button className="new-chat-button" type="button" onClick={startConversation}>
          <Plus size={17} />
          <span>Nova conversa</span>
        </button>

        <div className="nav-section-label">ESPAÇO DE TRABALHO</div>
        <nav className="main-navigation" aria-label="Navegação principal">
          {navigation.map(({ label, path, icon: Icon, end }) => (
            <NavLink
              key={path}
              to={path}
              end={end}
              className={({ isActive }) => `nav-link ${isActive ? 'nav-link-active' : ''}`}
              onClick={() => setMobileMenuOpen(false)}
            >
              <Icon size={17} strokeWidth={1.8} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="usage-card">
            <span className="usage-icon"><Bot size={16} /></span>
            <div className="usage-copy">
              <strong>{aiConfigured ? 'IA conectada' : 'Seu espaço está pronto'}</strong>
              <span>{aiConfigured ? 'OpenRouter configurado' : 'Conecte a IA para começar'}</span>
            </div>
            <ArrowRight size={14} />
          </div>
          <NavLink
            className={({ isActive }) => `nav-link settings-link ${isActive ? 'nav-link-active' : ''}`}
            to="/settings"
            onClick={() => setMobileMenuOpen(false)}
          >
            <Settings2 size={17} strokeWidth={1.8} />
            <span>Configurações</span>
          </NavLink>
          {user?.roles.includes('ROLE_ADMIN') && (
            <NavLink
              className={({ isActive }) => `nav-link settings-link ${isActive ? 'nav-link-active' : ''}`}
              to="/admin"
              onClick={() => setMobileMenuOpen(false)}
            >
              <Shield size={17} strokeWidth={1.8} />
              <span>Administração</span>
            </NavLink>
          )}
          <div className="profile-card">
            <span className="profile-avatar">{user?.displayName?.charAt(0).toUpperCase() ?? 'A'}</span>
            <span className="profile-copy">
              <strong>{user?.displayName}</strong>
              <span>{user?.email}</span>
            </span>
            <button className="logout-button" onClick={() => void handleLogout()} type="button" aria-label="Sair da conta" title="Sair">
              <LogOut size={15} />
            </button>
          </div>
          {logoutError && <p className="workspace-plan-error" role="alert">{logoutError}</p>}
        </div>
      </aside>

      {mobileMenuOpen && (
        <button
          className="mobile-backdrop"
          type="button"
          aria-label="Fechar menu"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}

      <main className="main-panel">
        <header className="topbar">
          <button
            className="mobile-menu-button icon-button"
            type="button"
            aria-label={mobileMenuOpen ? 'Fechar menu' : 'Abrir menu'}
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          >
            {mobileMenuOpen ? <X size={19} /> : <Menu size={19} />}
          </button>
          <div className="breadcrumb">
            <span>Workspace</span>
            <span className="breadcrumb-divider">/</span>
            <strong>{pageTitle}</strong>
          </div>
          <div className="topbar-actions">
            <span className="environment-badge">
              <span className="status-dot" />
              Ambiente local
            </span>
            <button
              className="topbar-new-button"
              type="button"
              onClick={startConversation}
            >
              <Plus size={16} />
              <span>Nova conversa</span>
            </button>
          </div>
        </header>
        {themeError && <p className="theme-load-error" role="status">{themeError}</p>}
        <div className="page-content">
          <Routes>
            <Route
              path="/"
              element={(
                <Dashboard
                  onStartConversation={startConversation}
                />
              )}
            />
            <Route
              path="/chat"
              element={<ChatPage aiConfigured={aiConfigured} aiStatusError={aiStatusError} />}
            />
            <Route path="/documents" element={<DocumentsPage />} />
            <Route path="/tasks" element={<TasksPage />} />
            <Route path="/plans" element={<PlansPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route
              path="/admin"
              element={(
                <RequireAdmin>
                  <AdminPage aiConfigured={aiConfigured} />
                </RequireAdmin>
              )}
            />
            <Route
              path="*"
              element={(
                <Dashboard
                  onStartConversation={startConversation}
                />
              )}
            />
          </Routes>
        </div>
      </main>
    </div>
  )
}

function WorkspacePlanMenu({ token, displayName }: { token: string | null; displayName: string }) {
  const [open, setOpen] = useState(false)
  const [catalog, setCatalog] = useState<PlanCatalog | null>(null)
  const [billing, setBilling] = useState<BillingStatus | null>(null)
  const [catalogError, setCatalogError] = useState('')
  const [billingError, setBillingError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const menuRef = useRef<HTMLDivElement>(null)
  const location = useLocation()
  const navigate = useNavigate()
  const currentPlan = catalog?.plans.find((plan) => plan.key === catalog.currentPlanKey)
  const planLabel = currentPlan?.name ?? (catalog ? 'Plano indisponível' : 'Carregando plano...')
  const userInitial = displayName.trim().charAt(0).toUpperCase() || 'A'

  useEffect(() => {
    let active = true
    void apiRequest<PlanCatalog>('/plans', {}, token)
      .then((response) => {
        if (active) {
          setCatalog(response)
          setCatalogError('')
        }
      })
      .catch((requestError: unknown) => {
        if (active) {
          setCatalog(null)
          setCatalogError(requestError instanceof Error
            ? requestError.message
            : 'Não foi possível consultar seu plano.')
        }
      })
    void apiRequest<BillingStatus>('/billing', {}, token)
      .then((response) => {
        if (active) {
          setBilling(response)
          setBillingError('')
        }
      })
      .catch((requestError: unknown) => {
        if (active) {
          setBilling(null)
          setBillingError(requestError instanceof Error
            ? requestError.message
            : 'Não foi possível consultar sua assinatura.')
        }
      })
    return () => { active = false }
  }, [token, location.pathname, refreshKey])

  useEffect(() => {
    if (!open) return
    function closeOnOutsideClick(event: PointerEvent) {
      if (event.target instanceof Node && !menuRef.current?.contains(event.target)) {
        setOpen(false)
      }
    }
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', closeOnOutsideClick)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsideClick)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [open])

  function openPlanManagement() {
    setOpen(false)
    navigate('/plans')
  }

  return (
    <div className="workspace-menu" ref={menuRef}>
      <button
        className="workspace-switcher"
        type="button"
        aria-label={`Meu espaço, plano ${planLabel}`}
        aria-expanded={open}
        aria-controls="workspace-plan-popover"
        onClick={() => {
          setOpen((value) => !value)
          setRefreshKey((value) => value + 1)
        }}
      >
        <span className="workspace-avatar">{userInitial}</span>
        <span className="workspace-copy">
          <span>Meu espaço</span>
          <span>{planLabel}</span>
        </span>
        <ChevronDown className={open ? 'workspace-chevron-open' : ''} size={15} />
      </button>
      {open && (
        <section className="workspace-plan-popover" id="workspace-plan-popover" aria-label="Detalhes do plano atual">
          <div className="workspace-plan-heading">
            <div>
              <span className="workspace-plan-eyebrow">SEU PLANO ATUAL</span>
              <h2>{currentPlan?.name ?? (catalogError ? 'Plano indisponível' : 'Carregando...')}</h2>
            </div>
            {currentPlan && <span className="workspace-plan-badge">ATIVO</span>}
          </div>
          {currentPlan && (
            <>
              <p className="workspace-plan-description">{currentPlan.description}</p>
              <p className="workspace-plan-price">
                <strong>
                  {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
                    .format(currentPlan.monthlyPriceCents / 100)}
                </strong>
                <span>/mês</span>
              </p>
              <ul className="workspace-plan-features">
                <li><span>Mensagens de IA</span><strong>{currentPlan.monthlyMessages}/mês</strong></li>
                <li>
                  <span>Tarefas ativas</span>
                  <strong>{currentPlan.activeTaskLimit ?? 'Ilimitadas'}</strong>
                </li>
                <li>
                  <span>Conversas</span>
                  <strong>{currentPlan.conversationLimit ?? 'Ilimitadas'}</strong>
                </li>
                <li><span>Documentos</span><strong>{formatBytes(currentPlan.documentStorageBytes)}</strong></li>
              </ul>
            </>
          )}
          {billing?.testModeEnabled && (
            <p className="workspace-plan-status">
              {billing.testSubscription
                ? 'Plano de teste ativo; nenhuma cobrança foi realizada.'
                : 'Modo de testes: as assinaturas não geram cobrança.'}
            </p>
          )}
          {billing?.cancellationScheduled && billing.currentPeriodEnd && (
            <p className="workspace-plan-status">
              Acesso disponível até {new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium' })
                .format(new Date(billing.currentPeriodEnd))}.
            </p>
          )}
          {(catalogError || billingError) && (
            <p className="workspace-plan-error" role="status">{catalogError || billingError}</p>
          )}
          <button className="workspace-plan-manage" onClick={openPlanManagement} type="button">
            Gerenciar plano <ArrowRight size={14} />
          </button>
        </section>
      )}
    </div>
  )
}

function RequireAdmin({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  return user?.roles.includes('ROLE_ADMIN') ? children : <Navigate to="/" replace />
}

function Dashboard({
  onStartConversation,
}: {
  onStartConversation: () => void
}) {
  const { token } = useAuth()
  const navigate = useNavigate()
  const [stats, setStats] = useState<{ conversations: number; documents: number; tasks: number } | null>(null)
  const [statsError, setStatsError] = useState('')

  useEffect(() => {
    let active = true
    void Promise.all([
      apiRequest<Conversation[]>('/conversations', {}, token),
      apiRequest<DocumentItem[]>('/documents', {}, token),
      apiRequest<Task[]>('/tasks', {}, token),
    ])
      .then(([conversations, documents, tasks]) => {
        if (active) {
          setStats({
            conversations: conversations.length,
            documents: documents.length,
            tasks: tasks.filter((task) => !task.completed).length,
          })
          setStatsError('')
        }
      })
      .catch((error: unknown) => {
        if (active) setStatsError(error instanceof Error ? error.message : 'Não foi possível carregar o resumo da conta.')
      })
    return () => { active = false }
  }, [token])

  return (
    <div className="dashboard-page">
      <section className="welcome-section">
        <div className="welcome-copy">
          <div className="eyebrow"><span className="eyebrow-line" /> SEU ASSISTENTE, NO SEU RITMO</div>
          <h1>Um espaço para<br /><span>pensar com clareza.</span></h1>
          <p>Converse, organize suas ideias e encontre novas possibilidades — tudo em um só lugar.</p>
          <button className="primary-button" type="button" onClick={onStartConversation}>
            <Sparkles size={16} />
            Começar uma conversa
            <ArrowRight size={16} />
          </button>
        </div>
        <div className="welcome-art" aria-hidden="true">
          <div className="orbit orbit-outer" />
          <div className="orbit orbit-inner" />
          <div className="art-glow" />
          <div className="art-core"><Sparkles size={30} strokeWidth={1.3} /></div>
          <span className="orbit-dot orbit-dot-one" />
          <span className="orbit-dot orbit-dot-two" />
          <span className="orbit-dot orbit-dot-three" />
        </div>
      </section>

      {statsError && <p className="dashboard-data-error" role="status">{statsError}</p>}
      {stats && (
        <section className="dashboard-stats" aria-label="Resumo da conta">
          <div><MessageSquareText size={15} /><strong>{stats.conversations}</strong><span>Conversas</span></div>
          <div><FileText size={15} /><strong>{stats.documents}</strong><span>Documentos</span></div>
          <div><Check size={15} /><strong>{stats.tasks}</strong><span>Tarefas em aberto</span></div>
        </section>
      )}

      <section className="section-heading">
        <div>
          <span className="section-kicker">SEU WORKSPACE</span>
          <h2>O que você quer fazer?</h2>
        </div>
        <span className="section-caption">Suas ferramentas, em um só lugar</span>
      </section>

      <section className="feature-grid" aria-label="Ferramentas">
        <FeatureCard
          icon={<MessageSquareText size={19} />}
          label="CONVERSAS"
          title="Suas ideias começam aqui"
          description="Crie conversas e conecte seu provedor OpenRouter."
          action="Ir para conversas"
          onClick={onStartConversation}
          accent="violet"
        />
        <FeatureCard
          icon={<FileText size={19} />}
          label="DOCUMENTOS"
          title="Conhecimento organizado"
          description="Envie, organize e baixe seus arquivos privados."
          action="Explorar documentos"
          onClick={() => navigate('/documents')}
          accent="cyan"
        />
        <FeatureCard
          icon={<Check size={19} />}
          label="TAREFAS"
          title="Planos que saem do papel"
          description="Crie tarefas, defina prioridades e acompanhe prazos."
          action="Ver tarefas"
          onClick={() => navigate('/tasks')}
          accent="blue"
        />
      </section>

      <section className="bottom-note">
        <span className="bottom-note-icon"><Sparkles size={15} /></span>
        <p>Seu workspace está conectado. <strong>Conversas, tarefas e documentos ficam associados à sua conta.</strong></p>
        <span className="bottom-note-decoration"><ArrowDown size={15} /></span>
      </section>
    </div>
  )
}

function FeatureCard({
  icon,
  label,
  title,
  description,
  action,
  onClick,
  accent,
}: {
  icon: ReactNode
  label: string
  title: string
  description: string
  action: string
  onClick: () => void
  accent: string
}) {
  return (
    <button className={`feature-card accent-${accent}`} type="button" onClick={onClick}>
      <span className="feature-card-icon">{icon}</span>
      <span className="feature-card-label">{label}</span>
      <span className="feature-card-title">{title}</span>
      <span className="feature-card-description">{description}</span>
      <span className="feature-card-action">{action}<ArrowRight size={14} /></span>
    </button>
  )
}

function ChatPage({
  aiConfigured,
  aiStatusError,
}: {
  aiConfigured: boolean | null
  aiStatusError: string
}) {
  const { token } = useAuth()
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [selectedConversation, setSelectedConversation] = useState<Conversation | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [messagesConversationId, setMessagesConversationId] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [streamingContent, setStreamingContent] = useState('')
  const [regeneratingMessageId, setRegeneratingMessageId] = useState<string | null>(null)
  const messageGeneration = useRef(0)
  const streamController = useRef<AbortController | null>(null)

  useEffect(() => {
    let active = true
    void apiRequest<Conversation[]>('/conversations', {}, token)
      .then((items) => {
        if (active) {
          setConversations(items)
          setSelectedConversation(items[0] ?? null)
        }
      })
      .catch((error: unknown) => {
        if (active) setNotice(error instanceof Error ? error.message : 'Não foi possível carregar as conversas.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [token])

  useEffect(() => {
    if (!selectedConversation) return

    let active = true
    const generation = messageGeneration.current
    void apiRequest<ChatMessage[]>(`/conversations/${selectedConversation.id}/messages`, {}, token)
      .then((items) => {
        if (active && generation === messageGeneration.current) {
          setMessages(items)
          setMessagesConversationId(selectedConversation.id)
        }
      })
      .catch((error: unknown) => {
        if (active) setNotice(error instanceof Error ? error.message : 'Não foi possível carregar as mensagens.')
      })
    return () => {
      active = false
      streamController.current?.abort()
    }
  }, [selectedConversation, token])

  async function createConversation(): Promise<Conversation | null> {
    setNotice('')
    try {
      const conversation = await apiRequest<Conversation>('/conversations', {
        method: 'POST',
        body: JSON.stringify({ title: 'Nova conversa' }),
      }, token)
      setConversations((items) => [conversation, ...items])
      setSelectedConversation(conversation)
      setMessages([])
      setMessagesConversationId(conversation.id)
      return conversation
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Não foi possível criar a conversa.')
      return null
    }
  }

  async function renameConversation(conversation: Conversation) {
    const title = window.prompt('Novo nome para a conversa:', conversation.title)?.trim()
    if (!title || title === conversation.title) return
    try {
      const updated = await apiRequest<Conversation>(`/conversations/${conversation.id}`, {
        method: 'PUT',
        body: JSON.stringify({ title }),
      }, token)
      setConversations((items) => items.map((item) => item.id === updated.id ? updated : item))
      setSelectedConversation(updated)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Não foi possível renomear a conversa.')
    }
  }

  async function deleteConversation(conversation: Conversation) {
    if (!window.confirm(`Excluir a conversa "${conversation.title}"?`)) return
    try {
      await apiRequest<void>(`/conversations/${conversation.id}`, { method: 'DELETE' }, token)
      setConversations((items) => items.filter((item) => item.id !== conversation.id))
      if (selectedConversation?.id === conversation.id) setSelectedConversation(null)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Não foi possível excluir a conversa.')
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!draft.trim() || sending) return
    const submittedContent = draft.trim()
    const controller = new AbortController()
    streamController.current = controller
    messageGeneration.current++
    setSending(true)
    setStreamingContent('')
    setRegeneratingMessageId(null)
    setNotice('')
    try {
      const conversation = selectedConversation ?? await createConversation()
      if (!conversation) return
      const result = await streamChatMessage(
        `/conversations/${conversation.id}/messages/stream`,
        submittedContent,
        token,
        controller.signal,
        (tokenChunk) => setStreamingContent((current) => current + tokenChunk),
      )
      messageGeneration.current++
      setMessages(result.messages)
      setMessagesConversationId(conversation.id)
      setDraft('')
    } catch (error) {
      messageGeneration.current++
      if (error instanceof DOMException && error.name === 'AbortError') {
        setNotice('Geração interrompida.')
      } else {
        setNotice(error instanceof Error ? error.message : 'Não foi possível enviar a mensagem.')
      }
      if (selectedConversation) {
        const generation = messageGeneration.current
        void apiRequest<ChatMessage[]>(
          `/conversations/${selectedConversation.id}/messages`,
          {},
          token,
        )
          .then((items) => {
            if (generation === messageGeneration.current) {
              setMessages(items)
              setMessagesConversationId(selectedConversation.id)
            }
          })
          .catch((loadError: unknown) => {
            setNotice(loadError instanceof Error
              ? loadError.message
              : 'Não foi possível carregar as mensagens.')
          })
      }
    } finally {
      if (streamController.current === controller) streamController.current = null
      setSending(false)
      setStreamingContent('')
      setRegeneratingMessageId(null)
    }
  }

  async function regenerateResponse(message: ChatMessage) {
    if (!selectedConversation || sending) return
    const conversation = selectedConversation
    const controller = new AbortController()
    streamController.current = controller
    messageGeneration.current++
    setSending(true)
    setStreamingContent('')
    setRegeneratingMessageId(message.id)
    setNotice('')
    try {
      const result = await streamChatMessage(
        `/conversations/${conversation.id}/messages/${message.id}/regenerate/stream`,
        '',
        token,
        controller.signal,
        (tokenChunk) => setStreamingContent((current) => current + tokenChunk),
      )
      messageGeneration.current++
      setMessages(result.messages)
      setMessagesConversationId(conversation.id)
    } catch (error) {
      messageGeneration.current++
      setNotice(error instanceof DOMException && error.name === 'AbortError'
        ? 'Geração interrompida.'
        : error instanceof Error ? error.message : 'Não foi possível regenerar a resposta.')
      void apiRequest<ChatMessage[]>(`/conversations/${conversation.id}/messages`, {}, token)
        .then((items) => {
          setMessages(items)
          setMessagesConversationId(conversation.id)
        })
        .catch((loadError: unknown) => {
          setNotice(loadError instanceof Error
            ? loadError.message
            : 'Não foi possível carregar as mensagens.')
        })
    } finally {
      if (streamController.current === controller) streamController.current = null
      setSending(false)
      setStreamingContent('')
      setRegeneratingMessageId(null)
    }
  }

  async function copyResponse(content: string) {
    try {
      await navigator.clipboard.writeText(content)
      setNotice('Resposta copiada.')
    } catch {
      setNotice('Não foi possível copiar a resposta para a área de transferência.')
    }
  }

  return (
    <section className="chat-page">
      <div className="chat-heading-row">
        <div className="chat-intro">
          <span className="chat-icon"><Sparkles size={20} /></span>
          <span className="section-kicker">UM NOVO COMEÇO</span>
          <h1>{selectedConversation?.title ?? 'O que está na sua mente?'}</h1>
          <p>Um bom pensamento pode começar com uma simples pergunta.</p>
        </div>
        <button className="primary-button" onClick={() => void createConversation()} type="button">
          <Plus size={16} /> Nova conversa
        </button>
      </div>
      <div className="conversation-layout">
        <aside className="conversation-list" aria-label="Histórico de conversas">
          <div className="conversation-list-heading">
            <span>SEU HISTÓRICO</span>
            <button type="button" onClick={() => void createConversation()} aria-label="Criar conversa">
              <Plus size={15} />
            </button>
          </div>
          {loading && <p className="list-empty">Carregando conversas...</p>}
          {!loading && conversations.length === 0 && <p className="list-empty">Suas conversas aparecerão aqui.</p>}
          {conversations.map((conversation) => (
            <div
              className={`conversation-item ${selectedConversation?.id === conversation.id ? 'conversation-item-active' : ''}`}
              key={conversation.id}
            >
              <button
                className="conversation-select"
                onClick={() => {
                  setSelectedConversation(conversation)
                  setNotice('')
                }}
                type="button"
              >
                <MessageSquareText size={14} />
                <span>{conversation.title}</span>
              </button>
              <button
                className="conversation-action"
                onClick={() => void renameConversation(conversation)}
                type="button"
                aria-label={`Renomear ${conversation.title}`}
              >
                <Pencil size={13} />
              </button>
              <button
                className="conversation-action"
                onClick={() => void deleteConversation(conversation)}
                type="button"
                aria-label={`Excluir ${conversation.title}`}
              >
                <Trash2 size={13} />
              </button>
            </div>
          ))}
        </aside>

        <div className="chat-workspace">
          <div className="message-history" aria-live="polite">
            {!selectedConversation && (
              <div className="chat-empty-state">
                <MessageSquareText size={21} />
                <strong>Escolha uma conversa ou comece uma nova</strong>
                <span>O histórico fica associado à sua conta.</span>
              </div>
            )}
            {selectedConversation && messagesConversationId === selectedConversation.id && messages.length === 0 && (
              <div className="chat-empty-state">
                <Sparkles size={21} />
                <strong>Esta conversa está pronta</strong>
                <span>Envie uma mensagem para conversar com o provedor conectado.</span>
              </div>
            )}
            {messagesConversationId === selectedConversation?.id && messages.map((message) => (
              <article className={`message-bubble message-${message.role}`} key={message.id}>
                <span>{message.role === 'user' ? 'Você' : 'AETHER AI'}</span>
                {message.role === 'assistant'
                  ? (
                    <div className="markdown-content">
                      <Suspense fallback={<p>Formatando resposta...</p>}>
                        <MarkdownContent
                          content={sending && regeneratingMessageId === message.id
                            ? streamingContent || 'Preparando resposta...'
                            : message.content}
                        />
                      </Suspense>
                    </div>
                  )
                  : <p>{message.content}</p>}
                {message.role === 'assistant' && (
                  <div className="message-actions">
                    <button
                      type="button"
                      aria-label="Copiar resposta"
                      onClick={() => void copyResponse(message.content)}
                    >
                      <Copy size={13} /> Copiar
                    </button>
                    {message.id === messages[messages.length - 1]?.id && (
                      <button
                        type="button"
                        aria-label="Regenerar resposta"
                        disabled={sending}
                        onClick={() => void regenerateResponse(message)}
                      >
                        <RefreshCw size={13} /> Regenerar
                      </button>
                    )}
                  </div>
                )}
              </article>
            ))}
            {sending && selectedConversation && (
              <article className="message-bubble message-assistant" aria-live="polite">
                <span>AETHER AI</span>
                <div className="markdown-content">
                  {!regeneratingMessageId && (
                    streamingContent
                      ? <Suspense fallback={<p>Formatando resposta...</p>}><MarkdownContent content={streamingContent} /></Suspense>
                      : <p>Preparando resposta...</p>
                  )}
                </div>
              </article>
            )}
          </div>
          <form className="composer" onSubmit={(event) => void handleSubmit(event)}>
            <label className="sr-only" htmlFor="message-draft">Escreva sua mensagem</label>
            <textarea
              id="message-draft"
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              placeholder={selectedConversation
                ? 'Escreva uma mensagem...'
                : 'Escreva sua mensagem; criaremos uma conversa ao enviar...'}
              rows={3}
            />
            <div className="composer-footer">
              <span className="composer-hint">
                {aiConfigured
                  ? 'Sua mensagem será enviada ao provedor conectado.'
                  : aiConfigured === false
                    ? 'Você pode escrever; conecte um provedor para receber respostas.'
                    : aiStatusError || 'Verificando a conexão do provedor...'}
              </span>
              <button
                className="send-button"
                type={sending ? 'button' : 'submit'}
                disabled={!sending && !draft.trim()}
                onClick={sending ? () => streamController.current?.abort() : undefined}
                aria-label={sending ? 'Parar geração' : 'Enviar mensagem'}
              >
                {sending ? <Square size={14} /> : <Send size={16} />}
              </button>
            </div>
          </form>
        </div>
      </div>
      {notice && <p className="composer-notice" role="status">{notice}</p>}
      <p className="chat-disclaimer">O histórico pertence à sua conta. As respostas são geradas pelo provedor conectado.</p>
    </section>
  )
}

function TasksPage() {
  const { token } = useAuth()
  const [tasks, setTasks] = useState<Task[]>([])
  const [loading, setLoading] = useState(true)
  const [loadAttempt, setLoadAttempt] = useState(0)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [filter, setFilter] = useState<'all' | 'open' | 'completed'>('all')
  const [editingTask, setEditingTask] = useState<Task | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState<Task['priority']>('medium')
  const [dueDate, setDueDate] = useState('')

  useEffect(() => {
    let active = true
    void apiRequest<Task[]>('/tasks', {}, token)
      .then((items) => {
        if (!active) return
        setTasks(items)
        setError('')
      })
      .catch((requestError: unknown) => {
        if (active) setError(requestError instanceof Error ? requestError.message : 'Não foi possível carregar suas tarefas.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [token, loadAttempt])

  function resetForm() {
    setEditingTask(null)
    setTitle('')
    setDescription('')
    setPriority('medium')
    setDueDate('')
  }

  function editTask(task: Task) {
    setEditingTask(task)
    setTitle(task.title)
    setDescription(task.description ?? '')
    setPriority(task.priority)
    setDueDate(task.dueAt ? task.dueAt.slice(0, 10) : '')
  }

  async function saveTask(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError('')
    const request = {
      title: title.trim(),
      description: description.trim() || null,
      priority,
      completed: editingTask?.completed ?? false,
      dueAt: dueDate ? new Date(`${dueDate}T23:59:00`).toISOString() : null,
    }
    try {
      const saved = await apiRequest<Task>(
        editingTask ? `/tasks/${editingTask.id}` : '/tasks',
        { method: editingTask ? 'PUT' : 'POST', body: JSON.stringify(request) },
        token,
      )
      setTasks((items) => editingTask
        ? items.map((item) => item.id === saved.id ? saved : item)
        : [saved, ...items])
      resetForm()
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível salvar a tarefa.')
    } finally {
      setSaving(false)
    }
  }

  async function toggleTask(task: Task) {
    try {
      const updated = await apiRequest<Task>(`/tasks/${task.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          title: task.title,
          description: task.description,
          priority: task.priority,
          completed: !task.completed,
          dueAt: task.dueAt,
        }),
      }, token)
      setTasks((items) => items.map((item) => item.id === updated.id ? updated : item))
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível atualizar a tarefa.')
    }
  }

  async function deleteTask(task: Task) {
    if (!window.confirm(`Excluir a tarefa "${task.title}"?`)) return
    try {
      await apiRequest<void>(`/tasks/${task.id}`, { method: 'DELETE' }, token)
      setTasks((items) => items.filter((item) => item.id !== task.id))
      if (editingTask?.id === task.id) resetForm()
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível excluir a tarefa.')
    }
  }

  const filteredTasks = tasks.filter((task) => filter === 'all'
    || (filter === 'open' && !task.completed)
    || (filter === 'completed' && task.completed))

  return (
    <section className="tasks-page">
      <div className="tasks-heading">
        <div>
          <span className="section-kicker">ORGANIZE SEU DIA</span>
          <h1>Suas tarefas</h1>
          <p>Pequenos passos também levam longe.</p>
        </div>
        <span className="task-count">{tasks.filter((task) => !task.completed).length} em aberto</span>
      </div>

      <form className="task-form" onSubmit={(event) => void saveTask(event)}>
        <div className="task-form-heading">
          <strong>{editingTask ? 'Editar tarefa' : 'Adicionar uma tarefa'}</strong>
          {editingTask && <button className="text-button" onClick={resetForm} type="button">Cancelar</button>}
        </div>
        <label className="sr-only" htmlFor="task-title">Título da tarefa</label>
        <input
          id="task-title"
          maxLength={200}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="O que você precisa fazer?"
          required
          value={title}
        />
        <label className="sr-only" htmlFor="task-description">Descrição opcional</label>
        <textarea
          id="task-description"
          maxLength={10000}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="Adicionar detalhes (opcional)"
          rows={2}
          value={description}
        />
        <div className="task-form-footer">
          <label className="task-select-label">
            Prioridade
            <select onChange={(event) => setPriority(event.target.value as Task['priority'])} value={priority}>
              <option value="low">Baixa</option>
              <option value="medium">Média</option>
              <option value="high">Alta</option>
            </select>
          </label>
          <label className="task-date-label">
            <CalendarDays size={14} />
            <span>Prazo</span>
            <input onChange={(event) => setDueDate(event.target.value)} type="date" value={dueDate} />
          </label>
          <button className="primary-button task-submit" disabled={saving || !title.trim()} type="submit">
            {saving ? <LoaderCircle size={15} className="spin-icon" /> : <Plus size={15} />}
            {editingTask ? 'Salvar alterações' : 'Adicionar'}
          </button>
        </div>
      </form>

      {error && (
        <div className="tasks-error" role="alert">
          <AlertCircle size={15} />
          <span>{error}</span>
          <button className="text-button" onClick={() => setLoadAttempt((attempt) => attempt + 1)} type="button">
            Tentar novamente
          </button>
        </div>
      )}

      <div className="task-list-heading">
        <h2>Lista de tarefas</h2>
        <div className="task-filters" aria-label="Filtrar tarefas">
          {(['all', 'open', 'completed'] as const).map((value) => (
            <button
              className={filter === value ? 'task-filter-active' : ''}
              key={value}
              onClick={() => setFilter(value)}
              type="button"
            >
              {value === 'all' ? 'Todas' : value === 'open' ? 'Em aberto' : 'Concluídas'}
            </button>
          ))}
        </div>
      </div>

      <div className="task-list">
        {loading && <p className="task-list-empty"><LoaderCircle size={16} className="spin-icon" /> Carregando tarefas...</p>}
        {!loading && filteredTasks.length === 0 && (
          <div className="task-list-empty">
            <CheckCircle2 size={20} />
            <span>{tasks.length === 0 ? 'Nenhuma tarefa ainda. Adicione a primeira acima.' : 'Nenhuma tarefa neste filtro.'}</span>
          </div>
        )}
        {filteredTasks.map((task) => (
          <article className={`task-item ${task.completed ? 'task-item-completed' : ''}`} key={task.id}>
            <button
              aria-label={task.completed ? `Reabrir ${task.title}` : `Concluir ${task.title}`}
              className="task-check"
              onClick={() => void toggleTask(task)}
              type="button"
            >
              {task.completed ? <CheckCircle2 size={19} /> : <Circle size={19} />}
            </button>
            <div className="task-item-copy">
              <strong>{task.title}</strong>
              {task.description && <span>{task.description}</span>}
              <div className="task-item-meta">
                <span className={`priority-chip priority-${task.priority}`}>
                  {task.priority === 'high' ? 'Alta' : task.priority === 'low' ? 'Baixa' : 'Média'}
                </span>
                {task.dueAt && <span><CalendarDays size={12} />{new Date(task.dueAt).toLocaleDateString('pt-BR')}</span>}
              </div>
            </div>
            <button className="task-action" onClick={() => editTask(task)} type="button" aria-label={`Editar ${task.title}`}>
              <Pencil size={14} />
            </button>
            <button className="task-action task-delete" onClick={() => void deleteTask(task)} type="button" aria-label={`Excluir ${task.title}`}>
              <Trash2 size={14} />
            </button>
          </article>
        ))}
      </div>
    </section>
  )
}

function DocumentsPage() {
  const { token } = useAuth()
  const [documents, setDocuments] = useState<DocumentItem[]>([])
  const [file, setFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loadAttempt, setLoadAttempt] = useState(0)

  useEffect(() => {
    let active = true
    void apiRequest<DocumentItem[]>('/documents', {}, token)
      .then((items) => {
        if (active) {
          setDocuments(items)
          setError('')
        }
      })
      .catch((requestError: unknown) => {
        if (active) setError(requestError instanceof Error ? requestError.message : 'Não foi possível carregar os documentos.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [token, loadAttempt])

  async function uploadDocument(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file || busy) return
    const form = event.currentTarget
    setBusy(true)
    setError('')
    setNotice('')
    const body = new FormData()
    body.append('file', file)
    try {
      const document = await apiRequest<DocumentItem>('/documents', { method: 'POST', body }, token)
      setDocuments((items) => [document, ...items])
      setFile(null)
      form.reset()
      setNotice('Documento enviado com sucesso.')
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível enviar o documento.')
    } finally {
      setBusy(false)
    }
  }

  async function downloadDocument(document: DocumentItem) {
    setError('')
    try {
      const blob = await apiDownload(`/documents/${document.id}/download`, token)
      const url = URL.createObjectURL(blob)
      const link = window.document.createElement('a')
      link.href = url
      link.download = document.fileName
      link.click()
      URL.revokeObjectURL(url)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível baixar o documento.')
    }
  }

  async function deleteDocument(document: DocumentItem) {
    if (!window.confirm(`Excluir o documento "${document.fileName}"?`)) return
    try {
      await apiRequest<void>(`/documents/${document.id}`, { method: 'DELETE' }, token)
      setDocuments((items) => items.filter((item) => item.id !== document.id))
      setNotice('Documento excluído.')
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível excluir o documento.')
    }
  }

  return (
    <section className="documents-page">
      <div className="tasks-heading">
        <div>
          <span className="section-kicker">SEUS ARQUIVOS</span>
          <h1>Documentos</h1>
          <p>Arquivos privados da sua conta, armazenados localmente pelo backend.</p>
        </div>
        <span className="task-count">{documents.length} arquivos</span>
      </div>

      <form className="document-upload" onSubmit={(event) => void uploadDocument(event)}>
        <span className="document-upload-icon"><Upload size={19} /></span>
        <div className="document-upload-copy">
          <strong>Adicione um documento</strong>
          <span>PDF, TXT ou Markdown · até 10 MB</span>
        </div>
        <label className="document-file-picker">
          <span>{file?.name ?? 'Escolher arquivo'}</span>
          <input
            accept=".pdf,.txt,.md,application/pdf,text/plain,text/markdown"
            onChange={(event) => {
              setFile(event.target.files?.[0] ?? null)
              setNotice('')
              setError('')
            }}
            type="file"
          />
        </label>
        <button className="primary-button task-submit" disabled={!file || busy} type="submit">
          {busy ? <LoaderCircle size={15} className="spin-icon" /> : <Upload size={15} />}
          Enviar
        </button>
      </form>

      {error && (
        <div className="tasks-error" role="alert">
          <AlertCircle size={15} />
          <span>{error}</span>
          <button className="text-button" onClick={() => setLoadAttempt((attempt) => attempt + 1)} type="button">
            Tentar novamente
          </button>
        </div>
      )}
      {notice && <p className="success-notice" role="status">{notice}</p>}

      <div className="task-list-heading">
        <h2>Seus documentos</h2>
        <span className="section-caption">Acesso exclusivo à sua conta</span>
      </div>
      <div className="document-list">
        {loading && <p className="task-list-empty"><LoaderCircle size={16} className="spin-icon" /> Carregando documentos...</p>}
        {!loading && documents.length === 0 && (
          <div className="task-list-empty">
            <FolderOpen size={20} />
            <span>Nenhum documento ainda. Envie o primeiro arquivo acima.</span>
          </div>
        )}
        {documents.map((document) => (
          <article className="document-item" key={document.id}>
            <span className="document-file-icon"><FileText size={17} /></span>
            <div className="document-item-copy">
              <strong>{document.fileName}</strong>
              <span>{formatBytes(document.sizeBytes)} · {new Date(document.createdAt).toLocaleDateString('pt-BR')}</span>
            </div>
            <button
              className="task-action"
              onClick={() => void downloadDocument(document)}
              type="button"
              aria-label={`Baixar ${document.fileName}`}
            >
              <Download size={15} />
            </button>
            <button
              className="task-action task-delete"
              onClick={() => void deleteDocument(document)}
              type="button"
              aria-label={`Excluir ${document.fileName}`}
            >
              <Trash2 size={15} />
            </button>
          </article>
        ))}
      </div>
    </section>
  )
}

function SettingsPage() {
  const { token, user, updateProfile } = useAuth()
  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [preferences, setPreferences] = useState<UserSettings | null>(null)
  const [busy, setBusy] = useState(false)
  const [savingTheme, setSavingTheme] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    void apiRequest<UserSettings>('/users/me/settings', {}, token)
      .then((settings) => {
        if (active) {
          setPreferences(settings)
          applyTheme(settings.theme)
        }
      })
      .catch((requestError: unknown) => {
        if (active) setError(requestError instanceof Error ? requestError.message : 'Não foi possível carregar as preferências.')
      })
    return () => { active = false }
  }, [token])

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await updateProfile(displayName.trim())
      setNotice('Perfil atualizado.')
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível atualizar o perfil.')
    } finally {
      setBusy(false)
    }
  }

  async function saveTheme(theme: UserSettings['theme']) {
    if (!preferences || savingTheme) return
    const previousTheme = preferences.theme
    setSavingTheme(true)
    setError('')
    setNotice('')
    setPreferences({ ...preferences, theme })
    applyTheme(theme)
    try {
      const updated = await apiRequest<UserSettings>('/users/me/settings', {
        method: 'PUT',
        body: JSON.stringify({ theme }),
      }, token)
      setPreferences(updated)
      setNotice('Tema atualizado.')
    } catch (requestError) {
      setPreferences({ ...preferences, theme: previousTheme })
      applyTheme(previousTheme)
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível atualizar o tema.')
    } finally {
      setSavingTheme(false)
    }
  }

  async function downloadUsageReport() {
    try {
      const report = await apiRequest<{
        totals: { inputTokens: number; outputTokens: number; requests: number }
        recentActivity: Array<{ operation: string; inputTokens: number; outputTokens: number; createdAt: string }>
      }>('/usage', {}, token)
      const blob = new Blob([JSON.stringify(report, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = window.document.createElement('a')
      link.href = url
      link.download = 'aether-usage.json'
      link.click()
      URL.revokeObjectURL(url)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível exportar o uso.')
    }
  }

  return (
    <section className="settings-page">
      <div className="tasks-heading">
        <div>
          <span className="section-kicker">SUA CONTA</span>
          <h1>Configurações</h1>
          <p>Gerencie seus dados e preferências pessoais.</p>
        </div>
      </div>
      {error && <p className="tasks-error" role="alert"><AlertCircle size={15} />{error}</p>}
      {notice && <p className="success-notice" role="status">{notice}</p>}
      <form className="settings-card" onSubmit={(event) => void saveProfile(event)}>
        <span className="settings-card-icon"><UsersRound size={17} /></span>
        <div className="settings-card-heading">
          <strong>Perfil</strong>
          <span>Essas informações identificam sua conta.</span>
        </div>
        <label className="settings-field">
          <span>Nome de exibição</span>
          <input maxLength={120} onChange={(event) => setDisplayName(event.target.value)} required value={displayName} />
        </label>
        <label className="settings-field">
          <span>E-mail</span>
          <input readOnly value={user?.email ?? ''} />
        </label>
        <button className="primary-button settings-save" disabled={busy || !displayName.trim()} type="submit">
          {busy ? <LoaderCircle size={15} className="spin-icon" /> : <Save size={15} />}
          Salvar perfil
        </button>
      </form>
      <section className="settings-card preference-card">
        <span className="settings-card-icon"><Globe2 size={17} /></span>
        <div className="settings-card-heading">
          <strong>Preferências</strong>
          <span>Configurações disponíveis para seu workspace.</span>
        </div>
        <div className="settings-preference-row">
          <div><strong>Idioma</strong><span>Idioma atual da interface</span></div>
          <span className="settings-value">{preferences?.locale ?? 'Carregando...'}</span>
        </div>
        <div className="settings-preference-row">
          <div><strong>Tema</strong><span>Escolha a aparência deste site</span></div>
          <label className="settings-theme-control">
            <span className="sr-only">Tema do site</span>
            <select
              aria-label="Tema do site"
              disabled={!preferences || savingTheme}
              onChange={(event) => void saveTheme(event.target.value as UserSettings['theme'])}
              value={preferences?.theme ?? 'dark'}
            >
              <option value="dark">Escuro</option>
              <option value="light">Claro</option>
              <option value="system">Do sistema</option>
            </select>
          </label>
        </div>
      </section>
      <section className="settings-card usage-settings-card">
        <span className="settings-card-icon"><Database size={17} /></span>
        <div className="settings-card-heading">
          <strong>Dados de utilização</strong>
          <span>Exporte o resumo de uso registrado na sua conta.</span>
        </div>
        <button className="auth-secondary" onClick={() => void downloadUsageReport()} type="button">
          <Download size={15} /> Exportar resumo de uso
        </button>
      </section>
    </section>
  )
}

function PlansPage() {
  const { token } = useAuth()
  const [catalog, setCatalog] = useState<PlanCatalog | null>(null)
  const [billing, setBilling] = useState<BillingStatus | null>(null)
  const [error, setError] = useState('')
  const [billingError, setBillingError] = useState('')
  const [billingUnavailable, setBillingUnavailable] = useState(false)
  const [actionError, setActionError] = useState('')
  const [notice, setNotice] = useState(() => new URLSearchParams(window.location.search).get('billing') === 'return'
    ? 'Pagamento iniciado. O plano será atualizado após a confirmação segura do Mercado Pago.'
    : '')
  const [busy, setBusy] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    let active = true
    void apiRequest<PlanCatalog>('/plans', {}, token)
      .then((response) => {
        if (active) {
          setCatalog(response)
          setError('')
        }
      })
      .catch((requestError: unknown) => {
        if (active) setError(requestError instanceof Error ? requestError.message : 'Não foi possível carregar os planos.')
      })
    void apiRequest<BillingStatus>('/billing', {}, token)
      .then((response) => {
        if (active) {
          setBilling(response)
          setBillingError('')
          setBillingUnavailable(false)
        }
      })
      .catch((requestError: unknown) => {
        if (active) {
          setBilling(null)
          if (requestError instanceof ApiError && requestError.status === 404) {
            setBillingUnavailable(true)
            setBillingError('')
          } else {
            setBillingUnavailable(false)
            setBillingError(requestError instanceof Error
              ? requestError.message
              : 'Não foi possível consultar o status da assinatura.')
          }
        }
      })
    return () => { active = false }
  }, [token, refreshKey])

  async function beginCheckout(planKey: PlanCatalog['plans'][number]['key']) {
    setBusy(true)
    setActionError('')
    setNotice('')
    try {
      if (billing?.testModeEnabled) {
        const response = await apiRequest<BillingStatus>('/billing/test/activate', {
          method: 'POST',
          body: JSON.stringify({ planKey }),
        }, token)
        setBilling(response)
        setRefreshKey((value) => value + 1)
        const planName = catalog?.plans.find((plan) => plan.key === planKey)?.name ?? planKey
        setNotice(`${planName} ativado para testes por 30 dias. Nenhuma cobrança foi realizada.`)
        setBusy(false)
        return
      }

      const response = await apiRequest<CheckoutResponse>('/billing/checkout', {
        method: 'POST',
        body: JSON.stringify({ planKey }),
      }, token)
      const checkoutUrl = new URL(response.checkoutUrl)
      if (checkoutUrl.protocol !== 'https:'
        || !(checkoutUrl.hostname === 'mercadopago.com.br' || checkoutUrl.hostname.endsWith('.mercadopago.com.br'))) {
        throw new Error('O Mercado Pago retornou um endereço de pagamento inválido.')
      }
      window.location.assign(checkoutUrl.toString())
    } catch (requestError) {
      setActionError(requestError instanceof Error ? requestError.message : 'Não foi possível iniciar o pagamento.')
      setBusy(false)
    }
  }

  async function cancelRenewal() {
    if (!window.confirm('Deseja cancelar a renovação? Seu plano pago continuará ativo até o fim do período já pago.')) return
    setBusy(true)
    setActionError('')
    setNotice('')
    try {
      const endpoint = billing?.testSubscription ? '/billing/test/cancel' : '/billing/cancel'
      const response = await apiRequest<BillingStatus>(endpoint, { method: 'POST' }, token)
      setBilling(response)
      setRefreshKey((value) => value + 1)
      setNotice(response.cancellationScheduled
        ? response.testSubscription
          ? 'Plano de teste cancelado. Seu acesso continua até o fim do período de teste.'
          : 'Renovação cancelada. Seu plano continuará ativo até o fim do período pago.'
        : response.testSubscription
        ? 'Plano de teste cancelado. Nenhuma cobrança foi realizada.'
        : 'Assinatura pendente cancelada. Nenhum plano pago foi ativado.')
    } catch (requestError) {
      setActionError(requestError instanceof Error ? requestError.message : 'Não foi possível cancelar a renovação.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="plans-page">
      <div className="tasks-heading">
        <div>
          <span className="section-kicker">PLANOS AETHER</span>
          <h1>Escolha o espaço ideal para você.</h1>
          <p>Compare limites e recursos de cada plano.</p>
        </div>
      </div>
      {error && <p className="tasks-error" role="alert"><AlertCircle size={15} />{error}</p>}
      {actionError && <p className="tasks-error" role="alert"><AlertCircle size={15} />{actionError}</p>}
      {billingError && <p className="tasks-error" role="alert"><AlertCircle size={15} />Status de cobrança indisponível: {billingError}</p>}
      {billingUnavailable && (
        <p className="plan-billing-alert" role="status">
          Os planos estão disponíveis, mas a cobrança requer reiniciar o backend com a versão mais recente.
        </p>
      )}
      {notice && <p className="plan-billing-alert" role="status">{notice}</p>}
      {billing?.testModeEnabled && (
        <p className="plan-billing-alert" role="status">
          Modo de testes: ativar um plano não gera cobrança e libera o acesso por 30 dias.
        </p>
      )}
      {billing?.cancellationScheduled && billing.currentPeriodEnd && (
        <p className="plan-billing-alert" role="status">
          Renovação cancelada. Acesso ao plano até {new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium' }).format(new Date(billing.currentPeriodEnd))}.
        </p>
      )}
      {(billing?.subscriptionStatus === 'authorized'
        || billing?.subscriptionStatus === 'pending'
        || billing?.subscriptionStatus === 'paused') && (
        <button className="auth-secondary plan-cancel-button" disabled={busy} onClick={() => void cancelRenewal()} type="button">
          {busy
            ? 'Processando...'
            : billing.testSubscription
            ? 'Cancelar plano de teste'
            : billing.subscriptionStatus === 'pending'
            ? 'Cancelar pagamento pendente'
            : 'Cancelar renovação'}
        </button>
      )}
      {!catalog && !error && <p className="task-list-empty"><LoaderCircle size={16} className="spin-icon" /> Carregando planos...</p>}
      {catalog && (
        <div className="plan-grid">
          {catalog.plans.map((plan) => {
            const isCurrent = plan.key === catalog.currentPlanKey
            const price = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
              .format(plan.monthlyPriceCents / 100)
            const taskLimit = plan.activeTaskLimit === null
              ? 'Tarefas ativas ilimitadas'
              : `${plan.activeTaskLimit} tarefas ativas`
            const conversationLimit = plan.conversationLimit === null
              ? 'Conversas ilimitadas'
              : `${plan.conversationLimit} ${plan.conversationLimit === 1 ? 'conversa' : 'conversas'}`
            const subscriptionInProgress = billing?.subscriptionStatus === 'pending'
            const hasPaidSubscription = !billing?.testSubscription && (billing?.subscriptionStatus === 'authorized'
              || billing?.subscriptionStatus === 'paused'
              || billing?.cancellationScheduled === true)

            return (
              <article className={`plan-card ${isCurrent ? 'plan-card-current' : ''}`} key={plan.key}>
                {isCurrent && <span className="plan-current-badge">SEU PLANO</span>}
                <h2>{plan.name}</h2>
                <p className="plan-description">{plan.description}</p>
                <p className="plan-price">
                  <strong>{price}</strong>
                  <span>/mês</span>
                </p>
                <ul className="plan-features">
                  <li><Check size={15} />{plan.monthlyMessages} mensagens de IA por mês</li>
                  <li><Check size={15} />{taskLimit}</li>
                  <li><Check size={15} />{conversationLimit}</li>
                  <li><Check size={15} />{formatBytes(plan.documentStorageBytes)} de documentos</li>
                </ul>
                <button
                  className="plan-action"
                  disabled={isCurrent || busy || billing === null || billingUnavailable || Boolean(billingError)
                    || hasPaidSubscription || (subscriptionInProgress && billing?.planKey !== plan.key)}
                  onClick={() => void beginCheckout(plan.key)}
                  type="button"
                >
                  {isCurrent
                    ? 'Plano atual'
                    : busy
                    ? 'Processando...'
                    : subscriptionInProgress && billing?.planKey === plan.key
                    ? 'Continuar pagamento'
                    : billing?.testModeEnabled
                    ? 'Ativar plano de teste'
                    : 'Assinar plano'}
                </button>
              </article>
            )
          })}
        </div>
      )}
      <p className="plan-billing-note">
        {billing?.testModeEnabled
          ? 'Assinaturas de teste locais não geram cobrança e não estão disponíveis em produção.'
          : 'Pagamentos recorrentes via Mercado Pago. O plano só muda após confirmação da assinatura.'}
      </p>
    </section>
  )
}

function AdminPage({ aiConfigured }: { aiConfigured: boolean | null }) {
  const { token } = useAuth()
  const [users, setUsers] = useState<AdminUser[]>([])
  const [metrics, setMetrics] = useState<AdminMetrics | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    void Promise.all([
      apiRequest<AdminUserPage>('/admin/users?page=0&size=50', {}, token),
      apiRequest<AdminMetrics>('/admin/metrics', {}, token),
    ])
      .then(([userPage, summary]) => {
        if (!active) return
        setUsers(userPage.content)
        setMetrics(summary)
      })
      .catch((requestError: unknown) => {
        if (active) setError(requestError instanceof Error ? requestError.message : 'Não foi possível carregar a área administrativa.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [token])

  async function toggleUser(user: AdminUser) {
    try {
      const updated = await apiRequest<AdminUser>(`/admin/users/${user.id}/status`, {
        method: 'PUT',
        body: JSON.stringify({ enabled: !user.enabled }),
      }, token)
      setUsers((items) => items.map((item) => item.id === updated.id ? updated : item))
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível atualizar o usuário.')
    }
  }

  return (
    <section className="admin-page">
      <div className="tasks-heading">
        <div>
          <span className="section-kicker">ACESSO RESTRITO</span>
          <h1>Administração</h1>
          <p>Gerenciamento de contas e utilização da plataforma.</p>
        </div>
      </div>
      <section className="connection-notice" aria-label="Status da integração">
        <span className="notice-icon"><ShieldAlert size={18} /></span>
        <div className="notice-copy">
          <strong>{aiConfigured ? 'A integração com IA está pronta' : 'A integração com IA ainda não está configurada'}</strong>
          <span>
            {aiConfigured
              ? 'As respostas serão geradas pelo provedor configurado.'
              : 'Nenhuma resposta será simulada. Configure sua chave OpenRouter para conversar por aqui.'}
          </span>
        </div>
        <span className="notice-status">{aiConfigured ? 'CONECTADA' : 'EM CONFIGURAÇÃO'}</span>
      </section>
      {error && <p className="tasks-error" role="alert"><AlertCircle size={15} />{error}</p>}
      {metrics && (
        <div className="admin-usage-card">
          <Database size={18} />
          <span><strong>{metrics.messages}</strong> mensagens nos últimos 30 dias</span>
          <span><strong>{metrics.usage.inputTokens + metrics.usage.outputTokens}</strong> tokens no período</span>
          {Object.entries(metrics.usersByPlan).map(([plan, count]) => (
            <span key={plan}><strong>{count}</strong> usuários {plan}</span>
          ))}
        </div>
      )}
      <div className="task-list-heading"><h2>Usuários</h2></div>
      <div className="admin-user-list">
        {loading && <p className="task-list-empty"><LoaderCircle size={16} className="spin-icon" /> Carregando usuários...</p>}
        {!loading && users.map((adminUser) => (
          <article className="admin-user-item" key={adminUser.id}>
            <span className="profile-avatar">{adminUser.displayName.charAt(0).toUpperCase()}</span>
            <div><strong>{adminUser.displayName}</strong><span>{adminUser.email}</span></div>
            <span className={adminUser.enabled ? 'account-status-enabled' : 'account-status-disabled'}>
              {adminUser.enabled ? 'Ativa' : 'Desativada'}
            </span>
            <button
              className="text-button"
              onClick={() => void toggleUser(adminUser)}
              type="button"
            >
              {adminUser.enabled ? 'Desativar' : 'Ativar'}
            </button>
          </article>
        ))}
      </div>
    </section>
  )
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`
}

export default App
