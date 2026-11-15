import { AuthPanel } from './components/AuthPanel'
import { ParkingCard } from './components/ParkingCard'
import { ReservationForm } from './components/ReservationForm'
import { useParkings } from './hooks/useParkings'
import { useSession } from './hooks/useSession'

const LIVE_LABEL = { open: 'temps réel actif', connecting: 'connexion…', closed: 'temps réel coupé' } as const

export default function App() {
  const { parkings, slotEvents, loading, error, live } = useParkings()
  const { session, me } = useSession()

  return (
    <>
      <header className="topbar">
        <h1>Mawqif · Automobiliste</h1>
        <span>
          <span className={`dot ${live === 'open' ? 'open' : live === 'closed' ? 'closed' : ''}`} />
          {LIVE_LABEL[live]}
        </span>
      </header>

      <main className="container">
        <AuthPanel me={me} loggedIn={session !== null} />

        <section className="panel">
          <h2>Parkings</h2>
          {loading && <div className="muted">Chargement des parkings…</div>}
          {error && <div className="msg error">{error}</div>}
          {!loading && !error && parkings.length === 0 && <div className="muted">Aucun parking pour le moment.</div>}
          <div className="grid">
            {parkings.map((p) => (
              <ParkingCard key={p.id} parking={p} slotEvents={slotEvents} />
            ))}
          </div>
        </section>

        <ReservationForm parkings={parkings} loggedIn={session !== null} />
      </main>
    </>
  )
}
