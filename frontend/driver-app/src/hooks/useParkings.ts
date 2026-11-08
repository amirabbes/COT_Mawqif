import { useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'
import { connectLive } from '../api/live'
import type { LiveStatus } from '../api/live'
import type { Parking } from '../api/types'

interface ParkingsState {
  parkings: Parking[]
  loading: boolean
  error: string | null
  live: LiveStatus
}

/** Liste des parkings (REST) mise à jour en direct (WebSocket "parking_updated"). */
export function useParkings(): ParkingsState {
  const [parkings, setParkings] = useState<Parking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [live, setLive] = useState<LiveStatus>('connecting')

  useEffect(() => {
    let cancelled = false

    api
      .listParkings()
      .then((list) => {
        if (!cancelled) setParkings(list)
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof ApiError ? e.message : 'Erreur inattendue')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    const stop = connectLive(
      'driver',
      (message) => {
        if (message.type !== 'parking_updated') return
        const updated = message.data
        setParkings((current) =>
          current.some((p) => p.id === updated.id)
            ? current.map((p) => (p.id === updated.id ? updated : p))
            : [...current, updated],
        )
      },
      setLive,
    )

    return () => {
      cancelled = true
      stop()
    }
  }, [])

  return { parkings, loading, error, live }
}
