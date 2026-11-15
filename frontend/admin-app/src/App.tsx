import { useState } from 'react'
import { logout } from './api/client'
import { Accounts } from './components/Accounts'
import { Alerts } from './components/Alerts'
import { Dashboard } from './components/Dashboard'
import { EventLog } from './components/EventLog'
import { Login } from './components/Login'
import { Reservations } from './components/Reservations'
import { Things } from './components/Things'
import { useAdminData } from './hooks/useAdminData'
import { useSession } from './hooks/useSession'

type Tab = 'dashboard' | 'alerts' | 'reservations' | 'things' | 'accounts' | 'log'

const LIVE_LABEL = { open: 'temps réel actif', connecting: 'connexion…', closed: 'temps réel coupé' } as const

export default function App() {
  const { session, me } = useSession()
  const data = useAdminData(session !== null)
  const [tab, setTab] = useState<Tab>('dashboard')

  const isAdmin = me?.roles.includes('ADMIN') ?? false
  const openAlerts = data.alerts.filter((a) => a.status === 'open').length
  const tabs: [Tab, string][] = [
    ['dashboard', 'Tableau de bord'],
    ['alerts', `Alertes${openAlerts ? ` (${openAlerts})` : ''}`],
    ['reservations', 'Réservations'],
    ['things', 'Objets'],
    ['log', 'Flux temps réel'],
    ...(isAdmin ? ([['accounts', 'Comptes']] as [Tab, string][]) : []),
  ]

  return (
    <>
      <header className="topbar">
        <h1>Mawqif · Exploitant</h1>
        {session && (
          <>
            <span>
              <span className={`dot ${data.live === 'open' ? 'open' : data.live === 'closed' ? 'closed' : ''}`} />
              {LIVE_LABEL[data.live]}
            </span>
            <span>{me ? `${me.username} (${me.roles.join(', ')})` : '…'}</span>
            <button className="secondary" onClick={logout}>Déconnexion</button>
          </>
        )}
      </header>

      <main className="container">
        {!session && <Login />}
        {session && (
          <>
            {data.error && <div className="msg error">{data.error}</div>}
            <nav className="tabs">
              {tabs.map(([id, label]) => (
                <button key={id} className={tab === id ? 'active' : ''} onClick={() => setTab(id)}>{label}</button>
              ))}
            </nav>
            {tab === 'dashboard' && <Dashboard data={data} />}
            {tab === 'alerts' && <Alerts data={data} />}
            {tab === 'reservations' && <Reservations />}
            {tab === 'things' && <Things data={data} />}
            {tab === 'log' && <EventLog data={data} />}
            {tab === 'accounts' && isAdmin && <Accounts />}
          </>
        )}
      </main>
    </>
  )
}
