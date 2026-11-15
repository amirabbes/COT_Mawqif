import { useEffect, useState, useSyncExternalStore } from 'react'
import { api, getSession, subscribeSession } from '../api/client'
import type { Session } from '../api/client'
import type { Me } from '../api/types'

/** Session courante (jetons) + identité renvoyée par GET /auth/me. */
export function useSession(): { session: Session | null; me: Me | null } {
  const session = useSyncExternalStore(subscribeSession, getSession)
  const [me, setMe] = useState<Me | null>(null)
  const token = session?.refreshToken ?? null

  useEffect(() => {
    let cancelled = false
    if (!token) {
      // Déconnexion : on vide l'identité de façon asynchrone (pas de setState synchrone dans l'effet).
      void Promise.resolve().then(() => {
        if (!cancelled) setMe(null)
      })
      return () => {
        cancelled = true
      }
    }
    api
      .me()
      .then((m) => {
        if (!cancelled) setMe(m)
      })
      .catch(() => {
        if (!cancelled) setMe(null)
      })
    return () => {
      cancelled = true
    }
  }, [token])

  return { session, me }
}
