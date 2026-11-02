import { useEffect, useState, type FormEvent } from 'react'
import { ArrowLeft, ArrowRight, KeyRound, MailCheck } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { apiRequest } from '../lib/api'

export function PasswordRecoveryPage({ mode }: { mode: 'request' | 'reset' }) {
  const [searchParams] = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [configured, setConfigured] = useState<boolean | null>(null)
  const [checking, setChecking] = useState(mode === 'request')
  const [submitting, setSubmitting] = useState(false)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  const token = searchParams.get('token') ?? ''

  useEffect(() => {
    if (mode === 'reset' && token) {
      window.history.replaceState({}, '', '/reset-password')
    }
  }, [mode, token])

  useEffect(() => {
    if (mode !== 'request') return

    let active = true
    void apiRequest<{ configured: boolean }>('/auth/password-reset/status')
      .then(({ configured: emailConfigured }) => {
        if (active) setConfigured(emailConfigured)
      })
      .catch((requestError: unknown) => {
        if (active) setError(requestError instanceof Error
          ? requestError.message
          : 'Não foi possível verificar o serviço de e-mail.')
      })
      .finally(() => {
        if (active) setChecking(false)
      })
    return () => { active = false }
  }, [mode])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setNotice('')
    if (mode === 'reset' && password !== confirmPassword) {
      setError('As senhas não conferem.')
      return
    }

    setSubmitting(true)
    try {
      if (mode === 'request') {
        await apiRequest<void>('/auth/password-reset/request', {
          method: 'POST',
          body: JSON.stringify({ email }),
        })
        setNotice('Se este e-mail estiver cadastrado, você receberá instruções para redefinir a senha.')
      } else {
        await apiRequest<void>('/auth/password-reset/complete', {
          method: 'POST',
          body: JSON.stringify({ token, newPassword: password }),
        })
        setNotice('Senha redefinida. Entre novamente com a nova senha.')
        setPassword('')
        setConfirmPassword('')
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível concluir a solicitação.')
    } finally {
      setSubmitting(false)
    }
  }

  const isRequest = mode === 'request'
  const canSubmit = !submitting && (isRequest ? Boolean(email.trim()) : Boolean(token && password && confirmPassword))

  return (
    <main className="auth-page">
      <section className="auth-card">
        <Link className="auth-brand" to="/login">
          <span className="brand-mark"><KeyRound size={18} /></span>
          <span className="brand-name">AETHER<span>AI</span></span>
        </Link>
        <span className="auth-eyebrow">{isRequest ? 'RECUPERAÇÃO DE CONTA' : 'NOVA CREDENCIAL'}</span>
        <h1>{isRequest ? 'Esqueceu sua senha?' : 'Crie uma nova senha.'}</h1>
        <p className="auth-description">
          {isRequest
            ? 'Informe o e-mail da conta para receber um link seguro de redefinição.'
            : 'Escolha uma senha com pelo menos 12 caracteres. O link expira em 30 minutos.'}
        </p>

        {isRequest && !checking && configured === false && (
          <p className="recovery-unavailable" role="status">
            A recuperação por e-mail ainda não está configurada. Entre em contato com o suporte da plataforma.
          </p>
        )}
        {isRequest && checking && <p className="auth-loading" role="status">Verificando o serviço de e-mail...</p>}
        {mode === 'reset' && !token && (
          <p className="recovery-unavailable" role="alert">Este link não contém um token válido. Solicite outro link.</p>
        )}

        {(!isRequest || configured) && !checking && (isRequest || token) && (
          <form className="auth-form" onSubmit={(event) => void handleSubmit(event)}>
            {isRequest
              ? (
                <label>
                  <span>E-mail</span>
                  <input
                    autoComplete="email"
                    onChange={(event) => setEmail(event.target.value)}
                    required
                    type="email"
                    value={email}
                  />
                </label>
              )
              : (
                <>
                  <label>
                    <span>Nova senha</span>
                    <input
                      autoComplete="new-password"
                      minLength={12}
                      maxLength={128}
                      onChange={(event) => setPassword(event.target.value)}
                      required
                      type="password"
                      value={password}
                    />
                  </label>
                  <label>
                    <span>Confirme a nova senha</span>
                    <input
                      autoComplete="new-password"
                      minLength={12}
                      maxLength={128}
                      onChange={(event) => setConfirmPassword(event.target.value)}
                      required
                      type="password"
                      value={confirmPassword}
                    />
                  </label>
                </>
              )}
            <button className="auth-submit" disabled={!canSubmit} type="submit">
              {submitting ? 'Aguarde...' : isRequest ? 'Enviar instruções' : 'Redefinir senha'}
              {!submitting && <ArrowRight size={16} />}
            </button>
          </form>
        )}
        {error && <p className="auth-error" role="alert">{error}</p>}
        {notice && <p className="success-notice" role="status">{notice}</p>}
        <Link className="recovery-back-link" to="/login">
          <ArrowLeft size={14} /> Voltar para o login
        </Link>
        {isRequest && notice && <MailCheck className="recovery-success-icon" size={22} aria-hidden="true" />}
      </section>
    </main>
  )
}
