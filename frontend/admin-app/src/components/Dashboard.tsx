import { useState } from 'react'
import type { FormEvent } from 'react'
import { api, ApiError } from '../api/client'
import type { AdminData } from '../hooks/useAdminData'
import type { Parking } from '../api/types'

const STATUS_BADGE = { sent: 'warn', executed: 'ok', rejected: 'bad', expired: 'bad' } as const

function ParkingPanel({ parking, data }: { parking: Parking; data: AdminData }) {
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<{ kind: 'ok' | 'error'; text: string } | null>(null)
  const slots = data.slots[parking.id] ?? []

  async function open(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setMessage(null)
    try {
      const command = await api.openBarrier(parking.id, reason)
      data.addCommand(command)
      setMessage({ kind: 'ok', text: 'Commande signée envoyée (202). Le résultat arrive par temps réel ci-dessous.' })
      setReason('')
    } catch (err) {
      setMessage({ kind: 'error', text: err instanceof ApiError ? `${err.status} — ${err.message}` : 'Erreur inattendue' })
    } finally {
      setBusy(false)
    }
  }

  return (
    <article className="panel">
      <div className="row">
        <h2 className="grow">{parking.name}</h2>
        <span className={`badge ${parking.barrierState === 'open' ? 'ok' : parking.barrierState === 'closed' ? '' : 'warn'}`}>
          barrière : {parking.barrierState}
        </span>
      </div>
      <div>
        <span className="big">{parking.freeSlots}</span>
        <span className="muted"> / {parking.totalSlots} places libres</span>
      </div>
      <div className="slots">
        {slots.map((s) => (
          <div key={s.id} className={`slot ${s.state}`} title={s.plate ?? s.state}>
            <b>{s.number}</b>
            {s.plate ?? (s.state === 'free' ? 'libre' : s.state === 'occupied' ? 'occupée' : 'réservée')}
          </div>
        ))}
      </div>
      <form onSubmit={open} className="row">
        <label className="grow">
          Motif de l'ouverture manuelle (3 à 200 caractères)
          <input value={reason} onChange={(e) => setReason(e.target.value)} minLength={3} maxLength={200} required />
        </label>
        <button className="danger" disabled={busy}>Ouvrir la barrière</button>
      </form>
      {message && <div className={`msg ${message.kind}`}>{message.text}</div>}
    </article>
  )
}

export function Dashboard({ data }: { data: AdminData }) {
  return (
    <>
      {data.parkings.length === 0 && <div className="panel muted">Aucun parking (le backend a-t-il créé les données de départ ?).</div>}
      {data.parkings.map((p) => (
        <ParkingPanel key={p.id} parking={p} data={data} />
      ))}
      <section className="panel">
        <h2>Commandes de barrière</h2>
        {data.commands.length === 0 && <div className="muted">Aucune commande pendant cette session.</div>}
        {data.commands.length > 0 && (
          <div className="tablewrap">
            <table>
              <thead><tr><th>Statut</th><th>Objet</th><th>Émise</th><th>Expire</th></tr></thead>
              <tbody>
                {data.commands.map((c) => (
                  <tr key={c.id}>
                    <td><span className={`badge ${STATUS_BADGE[c.status]}`}>{c.status}</span></td>
                    <td>{c.thingId}</td>
                    <td>{new Date(c.issuedAt).toLocaleTimeString()}</td>
                    <td>{new Date(c.expiresAt).toLocaleTimeString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  )
}
