import { useState, type FormEvent } from 'react'
import { ArrowRight, Eye, EyeOff, Sparkles } from 'lucide-react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from './useAuth'

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { token, user, loading, login, register } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  if (loading) return <AuthLoading />
  if (token && user) return <Navigate to="/" replace />

  const isRegister = mode === 'register'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      if (isRegister) await register(email, password, displayName)
      else await login(email, password)
      navigate('/', { replace: true })
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível autenticar.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-card">
        <Link className="auth-brand" to="/">
          <span className="brand-mark"><Sparkles size={19} /></span>
          <span className="brand-name">AETHER<span>AI</span></span>
        </Link>
        <span className="auth-eyebrow">{isRegister ? 'COMECE POR AQUI' : 'BEM-VINDO DE VOLTA'}</span>
        <h1>{isRegister ? 'Crie seu espaço.' : 'Entre no seu espaço.'}</h1>
        <p className="auth-description">
          {isRegister
            ? 'Sua conta pessoal para organizar ideias e conversas.'
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
            {submitting ? 'Aguarde...' : isRegister ? 'Criar conta' : 'Entrar'}
            {!submitting && <ArrowRight size={16} />}
          </button>
        </form>

        <p className="auth-switch">
          {isRegister ? 'Já tem uma conta?' : 'Ainda não tem uma conta?'}
          {' '}
          <Link to={isRegister ? '/login' : '/register'}>
            {isRegister ? 'Entrar' : 'Criar conta'}
          </Link>
        </p>
        <p className="auth-security">Suas credenciais são enviadas somente à API local configurada para este projeto.</p>
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
