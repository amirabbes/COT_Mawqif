import type { AdminData } from '../hooks/useAdminData'

export function Things({ data }: { data: AdminData }) {
  return (
    <section className="panel">
      <h2>Objets connectés</h2>
      {data.things.length === 0 && <div className="muted">Aucun objet. Lancez le simulateur : il publie les états en MQTT.</div>}
      {data.things.length > 0 && (
        <div className="tablewrap">
          <table>
            <thead><tr><th>Objet</th><th>Type</th><th>Parking</th><th>État</th><th>Vu pour la dernière fois</th></tr></thead>
            <tbody>
              {data.things.map((t) => (
                <tr key={t.id}>
                  <td>{t.id}</td>
                  <td>{t.type}</td>
                  <td>{t.parkingId ?? '—'}</td>
                  <td><span className={`badge ${t.state === 'online' ? 'ok' : 'bad'}`}>{t.state}</span></td>
                  <td>{t.lastSeen ? new Date(t.lastSeen).toLocaleString() : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
