import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, login } from '../api/client'

/** Connexion exploitant. Seuls les comptes OPERATOR / ADMIN sont acceptés par le client admin-app. */
export function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(username, password)
    } catch (err) {
      setError(err instanceof ApiError ? `${err.status} — ${err.message}` : 'Erreur inattendue')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel">
      <h2>Connexion exploitant</h2>
      <form onSubmit={submit} className="row">
        <label className="grow">
          Nom d'utilisateur
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
        </label>
        <label className="grow">
          Mot de passe
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
        </label>
        <button disabled={busy}>Se connecter</button>
      </form>
      {error && <div className="msg error">{error}</div>}
      <div className="muted">Comptes de test : admin / admin12345 · operator / operator12345 (un compte « driver » sera refusé : 403).</div>
    </section>
  )
}
