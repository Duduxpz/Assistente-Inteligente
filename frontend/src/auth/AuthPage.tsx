import { useState, type FormEvent } from 'react'
import { ArrowRight, Bot, Eye, EyeOff, ShieldCheck, Sparkles, UsersRound } from 'lucide-react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from './useAuth'

export function AuthPage({ mode }: { mode: 'login' | 'register' | 'admin' }) {
  const { token, user, loading, login, register } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const isRegister = mode === 'register'
  const isAdmin = mode === 'admin'

  if (loading) return <AuthLoading />
  if (token && user) {
    const destination = isAdmin && user.roles.includes('ROLE_ADMIN') ? '/admin' : '/'
    return <Navigate to={destination} replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      if (isRegister) await register(email, password, displayName)
      else await login(email, password)
      navigate(isAdmin ? '/admin' : '/', { replace: true })
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível autenticar.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className={`auth-page ${isAdmin ? 'auth-page-admin' : ''}`}>
      <div className={`auth-card ${isAdmin ? 'auth-card-admin' : ''}`}>
        {isAdmin && (
          <aside className="auth-admin-aside">
            <Link className="auth-brand" to="/">
              <span className="brand-mark"><Sparkles size={19} /></span>
              <span className="brand-name">AETHER<span>AI</span></span>
            </Link>
            <span className="auth-admin-label"><ShieldCheck size={14} /> PORTAL ADMINISTRATIVO</span>
            <h1>Gestão centralizada. <span>Acesso controlado.</span></h1>
            <p className="auth-admin-description">
              Um espaço seguro para acompanhar e administrar os recursos da plataforma.
            </p>
            <div className="auth-admin-features">
              <div><UsersRound size={17} /><span>Usuários e permissões</span></div>
              <div><Bot size={17} /><span>Assistente e consumo</span></div>
              <div><ShieldCheck size={17} /><span>Operações protegidas</span></div>
            </div>
            <div className="auth-admin-footer">
              <span className="auth-admin-status" />
              AETHER AI <span>·</span> Console de gestão
            </div>
          </aside>
        )}
        <section className={isAdmin ? 'auth-content-admin' : ''}>
          {!isAdmin && (
            <Link className="auth-brand" to="/">
              <span className="brand-mark"><Sparkles size={19} /></span>
              <span className="brand-name">AETHER<span>AI</span></span>
            </Link>
          )}
          <span className="auth-eyebrow">
            {isRegister ? 'COMECE POR AQUI' : isAdmin ? 'ACESSO ADMINISTRATIVO' : 'BEM-VINDO DE VOLTA'}
          </span>
          <h1>{isRegister ? 'Crie seu espaço.' : isAdmin ? 'Entrar no painel' : 'Entre no seu espaço.'}</h1>
          <p className="auth-description">
            {isRegister
              ? 'Sua conta pessoal para organizar ideias e conversas.'
              : isAdmin
                ? 'Entre com sua conta AETHER AI. Os recursos de administração dependem das permissões do perfil.'
                : 'Acesse suas conversas, tarefas e documentos.'}
          </p>

          <form className="auth-form" onSubmit={handleSubmit}>
            {isRegister && (
              <label>
                <span>Nome</span>
                <input
                  autoComplete="name"
                  maxLength={120}
                  onChange={(event) => setDisplayName(event.target.value)}
                  placeholder="Como podemos chamar você?"
                  required
                  value={displayName}
                />
              </label>
            )}
            <label>
              <span>E-mail</span>
              <input
                autoComplete="email"
                onChange={(event) => setEmail(event.target.value)}
                placeholder="voce@exemplo.com"
                required
                type="email"
                value={email}
              />
            </label>
            <label>
              <span>Senha</span>
              <span className="password-input">
                <input
                  autoComplete={isRegister ? 'new-password' : 'current-password'}
                  maxLength={128}
                  minLength={isRegister ? 12 : undefined}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder={isRegister ? 'Mínimo de 12 caracteres' : 'Sua senha'}
                  required
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                />
                <button
                  aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
                  className="password-visibility"
                  onClick={() => setShowPassword(!showPassword)}
                  type="button"
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </span>
            </label>
            {error && <p className="auth-error" role="alert">{error}</p>}
            <button className="auth-submit" disabled={submitting} type="submit">
              {submitting ? 'Aguarde...' : isRegister ? 'Criar conta' : isAdmin ? 'Acessar painel' : 'Entrar'}
              {!submitting && <ArrowRight size={16} />}
            </button>
          </form>

          <p className="auth-switch">
            {isRegister ? 'Já tem uma conta?' : isAdmin ? 'Acesso para usuários?' : 'Ainda não tem uma conta?'}
            {' '}
            <Link to={isRegister ? '/login' : isAdmin ? '/login' : '/register'}>
              {isRegister ? 'Entrar' : isAdmin ? 'Login padrão' : 'Criar conta'}
            </Link>
          </p>
          <p className="auth-security">Suas credenciais são enviadas somente à API local configurada para este projeto.</p>
        </section>
      </div>
    </main>
  )
}

function AuthLoading() {
  return (
    <main className="auth-page">
      <p className="auth-loading" role="status">Conectando à sua conta...</p>
    </main>
  )
}
