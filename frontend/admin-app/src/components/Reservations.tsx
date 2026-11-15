import { useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'
import type { Reservation } from '../api/types'

const STATUS_LABEL = { active: 'active', used: 'utilisée', cancelled: 'annulée', expired: 'expirée' } as const
const STATUS_BADGE = { active: 'ok', used: '', cancelled: 'bad', expired: 'warn' } as const

/** Toutes les réservations (l'exploitant peut aussi les annuler). */
export function Reservations() {
  const [list, setList] = useState<Reservation[]>([])
  const [error, setError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let cancelled = false
    api.listReservations()
      .then((l) => { if (!cancelled) setList(l) })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof ApiError ? `${e.status} — ${e.message}` : 'Erreur inattendue')
      })
    return () => { cancelled = true }
  }, [reload])

  async function cancel(id: string) {
    setError(null)
    try {
      await api.cancelReservation(id)
      setReload((n) => n + 1)
    } catch (err) {
      setError(err instanceof ApiError ? `${err.status} — ${err.message}` : 'Erreur inattendue')
    }
  }

  return (
    <section className="panel">
      <div className="row">
        <h2 className="grow">Réservations ({list.length})</h2>
        <button className="secondary" onClick={() => setReload((n) => n + 1)}>Actualiser</button>
      </div>
      {error && <div className="msg error">{error}</div>}
      {list.length === 0 && <div className="muted">Aucune réservation.</div>}
      {list.length > 0 && (
        <div className="tablewrap">
          <table>
            <thead><tr><th>Compte</th><th>Parking</th><th>Plaque</th><th>Période</th><th>Statut</th><th /></tr></thead>
            <tbody>
              {list.map((r) => (
                <tr key={r.id}>
                  <td>{r.owner ?? '—'}</td>
                  <td>{r.parkingId}</td>
                  <td>{r.plate}</td>
                  <td>{new Date(r.from).toLocaleString()} → {new Date(r.to).toLocaleString()}</td>
                  <td><span className={`badge ${STATUS_BADGE[r.status]}`}>{STATUS_LABEL[r.status]}</span></td>
                  <td>{r.status === 'active' && <button className="secondary" onClick={() => void cancel(r.id)}>Annuler</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
