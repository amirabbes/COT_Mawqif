import { getFreshAccessToken, WS_URL } from './client'
import type { LiveMessage, LiveView } from './types'

export type LiveStatus = 'connecting' | 'open' | 'closed'

/**
 * Ouvre le WebSocket temps réel et le rouvre automatiquement s'il tombe.
 * `authenticated` : joint le jeton d'accès (?access_token=), obligatoire pour la vue "admin".
 * Retourne une fonction pour tout arrêter (à appeler dans le cleanup d'un useEffect).
 */
export function connectLive(
  view: LiveView,
  onMessage: (message: LiveMessage) => void,
  onStatus?: (status: LiveStatus) => void,
  authenticated = false,
): () => void {
  let socket: WebSocket | null = null
  let stopped = false
  let retry = 0
  let timer: ReturnType<typeof setTimeout> | undefined

  const open = async () => {
    onStatus?.('connecting')
    let url = `${WS_URL}/${view}`
    if (authenticated) {
      const token = await getFreshAccessToken()
      if (!token) {
        onStatus?.('closed') // pas connecté : on ne réessaie pas
        return
      }
      url += `?access_token=${encodeURIComponent(token)}`
    }
    if (stopped) return
    socket = new WebSocket(url)
    socket.onopen = () => {
      retry = 0
      onStatus?.('open')
    }
    socket.onmessage = (event: MessageEvent<string>) => {
      try {
        onMessage(JSON.parse(event.data) as LiveMessage)
      } catch {
        /* message illisible : on l'ignore */
      }
    }
    socket.onclose = () => {
      onStatus?.('closed')
      if (stopped) return
      retry += 1
      timer = setTimeout(() => void open(), Math.min(1000 * 2 ** retry, 15000)) // 2 s, 4 s, 8 s ... max 15 s
    }
  }

  void open()
  return () => {
    stopped = true
    if (timer) clearTimeout(timer)
    socket?.close()
  }
}
