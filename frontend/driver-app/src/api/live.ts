import { WS_URL } from './client'
import type { LiveMessage, LiveView } from './types'

export type LiveStatus = 'connecting' | 'open' | 'closed'

/**
 * Ouvre le WebSocket temps réel et le rouvre automatiquement s'il tombe.
 * Retourne une fonction pour tout arrêter (à appeler dans le cleanup d'un useEffect).
 */
export function connectLive(
  view: LiveView,
  onMessage: (message: LiveMessage) => void,
  onStatus?: (status: LiveStatus) => void,
): () => void {
  let socket: WebSocket | null = null
  let stopped = false
  let retry = 0
  let timer: ReturnType<typeof setTimeout> | undefined

  const open = () => {
    onStatus?.('connecting')
    socket = new WebSocket(`${WS_URL}/${view}`)
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
      timer = setTimeout(open, Math.min(1000 * 2 ** retry, 15000)) // 2 s, 4 s, 8 s ... max 15 s
    }
  }

  open()
  return () => {
    stopped = true
    if (timer) clearTimeout(timer)
    socket?.close()
  }
}
