import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { api, ApiError } from '../api/client'
import type { Parking, Reservation } from '../api/types'

interface Props {
  parkings: Parking[]
  loggedIn: boolean
}

type Result = { kind: 'ok' | 'error'; text: string }

const STATUS_LABEL = { active: 'active', used: 'utilisée', cancelled: 'annulée', expired: 'expirée' } as const
const STATUS_BADGE = { active: 'ok', used: '', cancelled: 'bad', expired: 'warn' } as const

/** Valeur "datetime-local" (heure locale) pour maintenant + `plusHours`. */
function localInput(plusHours: number): string {
  const d = new Date(Date.now() + plusHours * 3_600_000)
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 16)
}

const errorText = (err: unknown) => (err instanceof ApiError ? `${err.status} — ${err.message}` : 'Erreur inattendue')

/** Réserver une place et gérer mes réservations. */
export function ReservationForm({ parkings, loggedIn }: Props) {
  const [parkingId, setParkingId] = useState('')
  const [plate, setPlate] = useState('')
  const [from, setFrom] = useState(() => localInput(1))
  const [to, setTo] = useState(() => localInput(3))
  const [result, setResult] = useState<Result | null>(null)
  const [mine, setMine] = useState<Reservation[]>([])
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let cancelled = false
    const load = loggedIn ? api.listReservations() : Promise.resolve([] as Reservation[])
    load
      .then((list) => { if (!cancelled) setMine(list) })
      .catch((e: unknown) => { if (!cancelled) setResult({ kind: 'error', text: errorText(e) }) })
    return () => { cancelled = true }
  }, [loggedIn, reload])

  async function submit(e: FormEvent) {
    e.preventDefault()
    setResult(null)
    try {
      const created = await api.createReservation({
        parkingId: parkingId || parkings[0]?.id || '',
        plate,
        from: new Date(from).toISOString(),
        to: new Date(to).toISOString(),
      })
      setResult({ kind: 'ok', text: `Réservation confirmée pour ${created.plate}.` })
      setReload((n) => n + 1)
    } catch (err) {
      setResult({ kind: 'error', text: errorText(err) })
    }
  }

  async function cancel(id: string) {
    setResult(null)
    try {
      await api.cancelReservation(id)
      setReload((n) => n + 1)
    } catch (err) {
      setResult({ kind: 'error', text: errorText(err) })
    }
  }

  return (
    <section className="panel">
      <h2>Réserver une place</h2>
      {!loggedIn && <div className="msg info">Connectez-vous pour réserver (sans compte, l'API répond 401).</div>}
      <form onSubmit={submit} className="row">
        <label>
          Parking
          <select value={parkingId} onChange={(e) => setParkingId(e.target.value)}>
            {parkings.map((p) => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
          </select>
        </label>
        <label className="grow">
          Plaque
          <input value={plate} onChange={(e) => setPlate(e.target.value)} placeholder="123 TUN 4567" required />
        </label>
        <label>
          Début
          <input type="datetime-local" value={from} onChange={(e) => setFrom(e.target.value)} required />
        </label>
        <label>
          Fin
          <input type="datetime-local" value={to} onChange={(e) => setTo(e.target.value)} required />
        </label>
        <button>Réserver</button>
      </form>
      {result && <div className={`msg ${result.kind}`}>{result.text}</div>}

      {loggedIn && (
        <>
          <h3>Mes réservations ({mine.length})</h3>
          {mine.length === 0 && <div className="muted">Aucune réservation.</div>}
          {mine.length > 0 && (
            <div className="tablewrap">
              <table>
                <thead><tr><th>Parking</th><th>Plaque</th><th>Période</th><th>Statut</th><th /></tr></thead>
                <tbody>
                  {mine.map((r) => (
                    <tr key={r.id}>
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
        </>
      )}
    </section>
  )
}
