import { useState } from 'react'
import { api, ApiError } from '../api/client'
import type { AdminData } from '../hooks/useAdminData'

const TYPE_LABEL = {
  parking_full: 'Parking complet',
  wrong_parking: 'Mauvais stationnement',
  plate_refused: 'Plaque refusée',
  thing_offline: 'Objet hors ligne',
} as const

export function Alerts({ data }: { data: AdminData }) {
  const [error, setError] = useState<string | null>(null)

  async function acknowledge(id: string) {
    setError(null)
    try {
      data.setAlert(await api.acknowledgeAlert(id))
    } catch (err) {
      setError(err instanceof ApiError ? `${err.status} — ${err.message}` : 'Erreur inattendue')
    }
  }

  const sorted = [...data.alerts].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  return (
    <section className="panel">
      <h2>Alertes ({data.alerts.filter((a) => a.status === 'open').length} ouvertes)</h2>
      {error && <div className="msg error">{error}</div>}
      {sorted.length === 0 && <div className="muted">Aucune alerte. Lancez le simulateur pour en générer.</div>}
      {sorted.length > 0 && (
        <div className="tablewrap">
          <table>
            <thead><tr><th>Date</th><th>Type</th><th>Détail</th><th>Statut</th><th /></tr></thead>
            <tbody>
              {sorted.map((a) => (
                <tr key={a.id}>
                  <td>{new Date(a.createdAt).toLocaleString()}</td>
                  <td>{TYPE_LABEL[a.type]}</td>
                  <td>
                    {a.parkingId}
                    {a.slot !== null && ` · place ${a.slot}`}
                    {a.plate && ` · ${a.plate}`}
                    {a.message && <div className="muted">{a.message}</div>}
                  </td>
                  <td><span className={`badge ${a.status === 'open' ? 'warn' : 'ok'}`}>{a.status === 'open' ? 'ouverte' : 'acquittée'}</span></td>
                  <td>{a.status === 'open' && <button className="secondary" onClick={() => void acknowledge(a.id)}>Acquitter</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
