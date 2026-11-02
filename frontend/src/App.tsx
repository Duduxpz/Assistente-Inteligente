import { useState, type FormEvent, type ReactNode } from 'react'
import {
  ArrowDown,
  ArrowRight,
  Bot,
  Check,
  ChevronDown,
  FileText,
  FolderOpen,
  LayoutDashboard,
  Menu,
  MessageSquareText,
  Plus,
  Send,
  Settings2,
  ShieldAlert,
  Sparkles,
  X,
} from 'lucide-react'
import { NavLink, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import './App.css'

const navigation = [
  { label: 'Visão geral', path: '/', icon: LayoutDashboard, end: true },
  { label: 'Conversas', path: '/chat', icon: MessageSquareText },
  { label: 'Documentos', path: '/documents', icon: FolderOpen },
  { label: 'Tarefas', path: '/tasks', icon: Check },
]

const pageTitles: Record<string, string> = {
  '/': 'Visão geral',
  '/chat': 'Conversas',
  '/documents': 'Documentos',
  '/tasks': 'Tarefas',
  '/settings': 'Configurações',
}

function App() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const pageTitle = pageTitles[location.pathname] ?? 'AETHER AI'

  function startConversation() {
    setMobileMenuOpen(false)
    navigate('/chat')
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

        <div className="workspace-switcher" aria-label="Espaço pessoal">
          <span className="workspace-avatar">D</span>
          <span className="workspace-copy">
            <span>Meu espaço</span>
            <span>Plano pessoal</span>
          </span>
          <ChevronDown size={15} />
        </div>

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
              <strong>Seu espaço está pronto</strong>
              <span>Conecte a IA para começar</span>
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
          <div className="profile-card">
            <span className="profile-avatar">D</span>
            <span className="profile-copy">
              <strong>Seu perfil</strong>
              <span>Conta pessoal</span>
            </span>
            <ChevronDown size={15} />
          </div>
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

        <div className="page-content">
          <Routes>
            <Route path="/" element={<Dashboard onStartConversation={startConversation} />} />
            <Route path="/chat" element={<ChatPage />} />
            <Route path="/documents" element={<FeaturePage title="Documentos" icon="documents" />} />
            <Route path="/tasks" element={<FeaturePage title="Tarefas" icon="tasks" />} />
            <Route path="/settings" element={<FeaturePage title="Configurações" icon="settings" />} />
            <Route path="*" element={<Dashboard onStartConversation={startConversation} />} />
          </Routes>
        </div>
      </main>
    </div>
  )
}

function Dashboard({ onStartConversation }: { onStartConversation: () => void }) {
  const navigate = useNavigate()

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

      <section className="connection-notice" aria-label="Status da integração">
        <span className="notice-icon"><ShieldAlert size={18} /></span>
        <div className="notice-copy">
          <strong>A integração com IA ainda não está configurada</strong>
          <span>Nenhuma resposta será simulada. Quando a API estiver pronta, você poderá conversar por aqui.</span>
        </div>
        <span className="notice-status">EM CONFIGURAÇÃO</span>
      </section>

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
          description="Abra uma conversa quando a integração com IA estiver disponível."
          action="Ir para conversas"
          onClick={onStartConversation}
          accent="violet"
        />
        <FeatureCard
          icon={<FileText size={19} />}
          label="DOCUMENTOS"
          title="Conhecimento organizado"
          description="O espaço para seus arquivos está sendo preparado."
          action="Explorar documentos"
          onClick={() => navigate('/documents')}
          accent="cyan"
        />
        <FeatureCard
          icon={<Check size={19} />}
          label="TAREFAS"
          title="Planos que saem do papel"
          description="Em breve, você poderá acompanhar suas tarefas por aqui."
          action="Ver tarefas"
          onClick={() => navigate('/tasks')}
          accent="blue"
        />
      </section>

      <section className="bottom-note">
        <span className="bottom-note-icon"><Sparkles size={15} /></span>
        <p>Estamos preparando seu espaço. <strong>Suas conversas e arquivos ainda não estão conectados.</strong></p>
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

function ChatPage() {
  const [draft, setDraft] = useState('')
  const [notice, setNotice] = useState('')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!draft.trim()) return
    setNotice('Mensagem não enviada: a integração com IA ainda não está configurada.')
  }

  return (
    <section className="chat-page">
      <div className="chat-intro">
        <span className="chat-icon"><Sparkles size={20} /></span>
        <span className="section-kicker">UM NOVO COMEÇO</span>
        <h1>O que está na sua mente?</h1>
        <p>Um bom pensamento pode começar com uma simples pergunta.</p>
      </div>
      <div className="chat-unavailable">
        <span className="notice-icon"><ShieldAlert size={18} /></span>
        <div className="notice-copy">
          <strong>A conversa ainda não pode ser enviada</strong>
          <span>A integração com o provedor de IA não está configurada. Não exibiremos respostas simuladas.</span>
        </div>
      </div>
      <form className="composer" onSubmit={handleSubmit}>
        <label className="sr-only" htmlFor="message-draft">Escreva sua mensagem</label>
        <textarea
          id="message-draft"
          value={draft}
          onChange={(event) => {
            setDraft(event.target.value)
            setNotice('')
          }}
          placeholder="Escreva uma mensagem..."
          rows={3}
        />
        <div className="composer-footer">
          <span className="composer-hint">Sua mensagem não será enviada enquanto a integração não estiver pronta.</span>
          <button className="send-button" type="submit" disabled={!draft.trim()} aria-label="Tentar enviar mensagem">
            <Send size={16} />
          </button>
        </div>
      </form>
      {notice && <p className="composer-notice" role="status">{notice}</p>}
      <p className="chat-disclaimer">AETHER AI ainda está em configuração. Nenhuma resposta de IA é gerada nesta etapa.</p>
    </section>
  )
}

function FeaturePage({ title, icon }: { title: string; icon: 'documents' | 'tasks' | 'settings' }) {
  const Icon = icon === 'documents' ? FileText : icon === 'tasks' ? Check : Settings2

  return (
    <section className="feature-empty-page">
      <span className="feature-empty-icon"><Icon size={21} /></span>
      <span className="section-kicker">AETHER AI · EM CONFIGURAÇÃO</span>
      <h1>{title}</h1>
      <p>Esta área faz parte do projeto e será ativada em uma próxima etapa.</p>
      <div className="feature-empty-notice">
        <ShieldAlert size={17} />
        <span>Nenhum dado foi criado ou armazenado aqui ainda.</span>
      </div>
    </section>
  )
}

export default App
