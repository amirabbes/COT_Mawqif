import { useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'
import { connectLive } from '../api/live'
import type { LiveStatus } from '../api/live'
import type { Alert, Command, LiveMessage, Parking, Slot, Thing } from '../api/types'

export interface AdminData {
  parkings: Parking[]
  slots: Record<string, Slot[]>
  things: Thing[]
  alerts: Alert[]
  /** Commandes de barrière vues pendant cette session (la plus récente d'abord). */
  commands: Command[]
  /** Derniers messages WebSocket reçus (le plus récent d'abord). */
  log: LiveMessage[]
  live: LiveStatus
  error: string | null
  setAlert: (alert: Alert) => void
  addCommand: (command: Command) => void
}

const upsert = <T extends { id: string }>(list: T[], item: T): T[] =>
  list.some((x) => x.id === item.id) ? list.map((x) => (x.id === item.id ? item : x)) : [item, ...list]

/** Charge les données de l'exploitant (REST) puis les tient à jour avec le WebSocket "admin" (jeton obligatoire). */
export function useAdminData(enabled: boolean): AdminData {
  const [parkings, setParkings] = useState<Parking[]>([])
  const [slots, setSlots] = useState<Record<string, Slot[]>>({})
  const [things, setThings] = useState<Thing[]>([])
  const [alerts, setAlerts] = useState<Alert[]>([])
  const [commands, setCommands] = useState<Command[]>([])
  const [log, setLog] = useState<LiveMessage[]>([])
  const [live, setLive] = useState<LiveStatus>('closed')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!enabled) return
    let cancelled = false
    const fail = (e: unknown) => {
      if (!cancelled) setError(e instanceof ApiError ? `${e.status} — ${e.message}` : 'Erreur inattendue')
    }

    api.listParkings().then(async (list) => {
      if (cancelled) return
      setParkings(list)
      const entries = await Promise.all(list.map(async (p) => [p.id, await api.listSlots(p.id)] as const))
      if (!cancelled) setSlots(Object.fromEntries(entries))
    }).catch(fail)
    api.listThings().then((l) => { if (!cancelled) setThings(l) }).catch(fail)
    api.listAlerts({ limit: 50 }).then((l) => { if (!cancelled) setAlerts(l) }).catch(fail)

    const stop = connectLive(
      'admin',
      (message) => {
        setLog((current) => [message, ...current].slice(0, 100))
        switch (message.type) {
          case 'parking_updated':
            setParkings((c) => upsert(c, message.data))
            break
          case 'slot_updated': {
            const slot = message.data
            setSlots((c) => {
              const list = c[slot.parkingId] ?? []
              const next = list.some((s) => s.id === slot.id) ? list.map((s) => (s.id === slot.id ? slot : s)) : [...list, slot]
              return { ...c, [slot.parkingId]: next.sort((a, b) => a.number - b.number) }
            })
            break
          }
          case 'thing_status':
            setThings((c) => upsert(c, message.data))
            break
          case 'alert_created':
            setAlerts((c) => upsert(c, message.data))
            break
          case 'command_ack':
            setCommands((c) => upsert(c, message.data))
            break
        }
      },
      setLive,
      true,
    )

    return () => {
      cancelled = true
      stop()
    }
  }, [enabled])

  return {
    parkings, slots, things, alerts, commands, log, live, error,
    setAlert: (alert) => setAlerts((c) => upsert(c, alert)),
    addCommand: (command) => setCommands((c) => upsert(c, command)),
  }
}
