import { useState } from 'react'
import type { FormEvent } from 'react'
import { api, ApiError, login, logout } from '../api/client'
import type { Me } from '../api/types'

interface Props {
  me: Me | null
  loggedIn: boolean
}

/** Connexion / inscription / déconnexion. Les parkings restent consultables sans compte. */
export function AuthPanel({ me, loggedIn }: Props) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (mode === 'login') await login(username, password)
      else await api.register(username, password)
      setPassword('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Erreur inattendue')
    } finally {
      setBusy(false)
    }
  }

  if (loggedIn) {
    return (
      <section className="panel">
        <h2>Mon compte</h2>
        <div>
          Connecté en tant que <strong>{me?.username ?? '…'}</strong> {me && <span className="badge ok">{me.roles.join(', ')}</span>}
        </div>
        {me && <div className="muted">Scopes : {me.scope} · jeton valide jusqu'à {new Date(me.expires_at).toLocaleTimeString()}</div>}
        <div className="row">
          <button className="secondary" onClick={logout}>Se déconnecter</button>
        </div>
      </section>
    )
  }

  return (
    <section className="panel">
      <h2>{mode === 'login' ? 'Connexion' : 'Créer un compte'}</h2>
      <form onSubmit={submit} className="row">
        <label className="grow">
          Nom d'utilisateur
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
        </label>
        <label className="grow">
          Mot de passe
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            minLength={mode === 'register' ? 8 : undefined}
            required
          />
        </label>
        <button disabled={busy}>{mode === 'login' ? 'Se connecter' : "S'inscrire"}</button>
      </form>
      {error && <div className="msg error">{error}</div>}
      <div className="row">
        <button
          type="button"
          className="secondary"
          onClick={() => {
            setMode(mode === 'login' ? 'register' : 'login')
            setError(null)
          }}
        >
          {mode === 'login' ? 'Pas de compte ? Inscription' : 'Déjà un compte ? Connexion'}
        </button>
        <span className="muted">Compte de test : driver / driver12345</span>
      </div>
    </section>
  )
}
