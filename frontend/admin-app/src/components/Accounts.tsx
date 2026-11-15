import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { api, ApiError } from '../api/client'
import type { Identity, Role } from '../api/types'

const ROLES: Role[] = ['DRIVER', 'OPERATOR', 'ADMIN']

/** Gestion des comptes (rôle ADMIN, scope iam:manage). Un opérateur obtient 403 s'il appelle cette API. */
export function Accounts() {
  const [identities, setIdentities] = useState<Identity[]>([])
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [roles, setRoles] = useState<Role[]>(['OPERATOR'])
  const [message, setMessage] = useState<{ kind: 'ok' | 'error'; text: string } | null>(null)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let cancelled = false
    api.listIdentities()
      .then((l) => { if (!cancelled) setIdentities(l) })
      .catch((e: unknown) => {
        if (!cancelled) setMessage({ kind: 'error', text: e instanceof ApiError ? `${e.status} — ${e.message}` : 'Erreur inattendue' })
      })
    return () => { cancelled = true }
  }, [reload])

  async function create(e: FormEvent) {
    e.preventDefault()
    setMessage(null)
    try {
      const created = await api.createIdentity({ username, password, roles })
      setMessage({ kind: 'ok', text: `Compte « ${created.username} » créé.` })
      setUsername('')
      setPassword('')
      setReload((n) => n + 1)
    } catch (err) {
      setMessage({ kind: 'error', text: err instanceof ApiError ? `${err.status} — ${err.message}` : 'Erreur inattendue' })
    }
  }

  function toggle(role: Role) {
    setRoles((current) => (current.includes(role) ? current.filter((r) => r !== role) : [...current, role]))
  }

  return (
    <>
      <section className="panel">
        <h2>Créer un compte</h2>
        <form onSubmit={create} className="row">
          <label className="grow">
            Nom d'utilisateur
            <input value={username} onChange={(e) => setUsername(e.target.value)} required />
          </label>
          <label className="grow">
            Mot de passe (8 caractères min.)
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={8} required />
          </label>
          <fieldset className="row" style={{ border: 0, padding: 0 }}>
            {ROLES.map((r) => (
              <label key={r} className="row">
                <input type="checkbox" checked={roles.includes(r)} onChange={() => toggle(r)} /> {r}
              </label>
            ))}
          </fieldset>
          <button disabled={roles.length === 0}>Créer</button>
        </form>
        {message && <div className={`msg ${message.kind}`}>{message.text}</div>}
      </section>

      <section className="panel">
        <h2>Comptes ({identities.length})</h2>
        <div className="tablewrap">
          <table>
            <thead><tr><th>Utilisateur</th><th>Rôles</th><th>Scopes</th><th>Créé</th></tr></thead>
            <tbody>
              {identities.map((i) => (
                <tr key={i.id}>
                  <td>{i.username}</td>
                  <td>{i.roles.join(', ')}</td>
                  <td className="muted">{i.scope}</td>
                  <td>{i.createdAt ? new Date(i.createdAt).toLocaleDateString() : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </>
  )
}
