import { useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'
import type { Parking, Slot } from '../api/types'

interface Props {
  parking: Parking
  /** Places reçues en direct (plus récentes que la liste chargée en REST). */
  slotEvents: Record<string, Slot>
}

const BARRIER_LABEL = { open: 'ouverte', closed: 'fermée', unknown: 'inconnue' } as const

export function ParkingCard({ parking, slotEvents }: Props) {
  const [expanded, setExpanded] = useState(false)
  const [loaded, setLoaded] = useState<Slot[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!expanded) return
    let cancelled = false
    api
      .listSlots(parking.id)
      .then((list) => {
        if (!cancelled) setLoaded(list)
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof ApiError ? e.message : 'Erreur inattendue')
      })
    return () => {
      cancelled = true
    }
  }, [expanded, parking.id])

  // Pour chaque place, on garde la version la plus récente entre le REST et le direct.
  const slots = (loaded ?? []).map((s) => {
    const live = slotEvents[s.id]
    return live && (live.updatedAt ?? '') >= (s.updatedAt ?? '') ? live : s
  })

  const full = parking.freeSlots === 0
  return (
    <article className="card">
      <div className="row">
        <h3 className="grow">{parking.name}</h3>
        <span className={`badge ${full ? 'bad' : 'ok'}`}>{full ? 'Complet' : 'Disponible'}</span>
      </div>
      <div>
        <span className="big">{parking.freeSlots}</span>
        <span className="muted"> / {parking.totalSlots} places libres</span>
      </div>
      <div className="muted">Barrière : {BARRIER_LABEL[parking.barrierState]}</div>
      <button className="secondary" onClick={() => setExpanded(!expanded)}>
        {expanded ? 'Masquer les places' : 'Voir les places'}
      </button>
      {expanded && error && <div className="msg error">{error}</div>}
      {expanded && loaded === null && !error && <div className="muted">Chargement…</div>}
      {expanded && loaded && (
        <div className="slots">
          {slots.map((s) => (
            <div key={s.id} className={`slot ${s.state}`} title={s.plate ?? s.state}>
              <b>{s.number}</b>
              {s.state === 'free' ? 'libre' : s.state === 'occupied' ? 'occupée' : 'réservée'}
            </div>
          ))}
        </div>
      )}
    </article>
  )
}
