import { useParkings } from './hooks/useParkings'

export default function App() {
  const { parkings, loading, error, live } = useParkings()

  return (
    <main style={{ fontFamily: 'sans-serif', padding: 24 }}>
      <h1>Mawqif · Automobiliste</h1>
      <p style={{ color: '#666' }}>
        Temps réel : {live === 'open' ? 'connecté' : live === 'connecting' ? 'connexion…' : 'déconnecté'}
      </p>

      {loading && <p>Chargement des parkings…</p>}
      {error && <p style={{ color: 'crimson' }}>Erreur : {error}</p>}
      {!loading && !error && parkings.length === 0 && <p>Aucun parking pour le moment.</p>}

      <ul style={{ listStyle: 'none', padding: 0, display: 'grid', gap: 12, maxWidth: 480 }}>
        {parkings.map((p) => (
          <li key={p.id} style={{ border: '1px solid #ddd', borderRadius: 8, padding: 12 }}>
            <strong>{p.name}</strong>
            <div>
              {p.freeSlots} place{p.freeSlots > 1 ? 's' : ''} libre{p.freeSlots > 1 ? 's' : ''} sur {p.totalSlots}
            </div>
            <small style={{ color: '#666' }}>Barrière : {p.barrierState}</small>
          </li>
        ))}
      </ul>
    </main>
  )
}
