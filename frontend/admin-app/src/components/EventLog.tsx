import type { AdminData } from '../hooks/useAdminData'

/** Flux brut des messages WebSocket : pratique pour vérifier que le simulateur et le backend communiquent. */
export function EventLog({ data }: { data: AdminData }) {
  return (
    <section className="panel">
      <h2>Flux temps réel ({data.log.length})</h2>
      {data.log.length === 0 && <div className="muted">En attente d'événements… lancez <code>python simulator/simulator.py</code>.</div>}
      <div className="log">
        {data.log.map((m, i) => (
          <div key={`${m.ts}-${i}`}>
            <b>{new Date(m.ts).toLocaleTimeString()}</b> · {m.type} · {JSON.stringify(m.data)}
          </div>
        ))}
      </div>
    </section>
  )
}
