import type {
  Alert, Command, CreateReservationRequest, Identity, Me, Parking, Problem, Reservation, Slot, SlotState, Thing,
  TokenResponse,
} from './types'

// Deux services : l'API (api-1.0.war) et l'IAM (iam-1.0.war : connexion, comptes, /auth/me).
// VITE_API_URL / VITE_IAM_URL       = adresse du service SANS chemin (ex. http://192.168.1.14:8081 ou https://api.mon-domaine.tn).
//                                     Par défaut : même machine que la page, port 8081 -> marche en local ET depuis un autre appareil.
// VITE_API_PREFIX / VITE_IAM_PREFIX = chemin devant /v1 (et /ws pour l'API). Par défaut "/api" et "/iam" (mode développement :
//                                     WildFly sert les deux WAR sous ces contextes). Vides ("") quand chaque service a son propre
//                                     hôte virtuel (scripts .cli de l'enseignant : https://api.<domaine>/v1 et https://iam.<domaine>/v1).
const DEFAULT_BASE = `${location.protocol}//${location.hostname}:8081`
const trim = (v: string) => v.replace(/\/$/, '')
const BASE = trim(import.meta.env.VITE_API_URL ?? DEFAULT_BASE)
const PREFIX = trim(import.meta.env.VITE_API_PREFIX ?? '/api')
export const API_URL = `${BASE}${PREFIX}/v1`
export const WS_URL = `${BASE.replace(/^http/, 'ws')}${PREFIX}/ws`
export const IAM_URL = `${trim(import.meta.env.VITE_IAM_URL ?? DEFAULT_BASE)}${trim(import.meta.env.VITE_IAM_PREFIX ?? '/iam')}/v1`

/** Client OAuth de cette application (voir IamSeed côté backend). */
export const CLIENT_ID = 'driver-app'

/** Erreur renvoyée par l'API (corps problem+json si disponible). */
export class ApiError extends Error {
  status: number
  problem?: Problem

  constructor(status: number, message: string, problem?: Problem) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

// ---------- Session (jetons) ----------

export interface Session {
  accessToken: string
  refreshToken: string
  /** Date d'expiration du jeton d'accès (ms depuis 1970). */
  expiresAt: number
  scope: string
}

const STORAGE_KEY = `mawqif.${CLIENT_ID}.session`

function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as Session) : null
  } catch {
    return null
  }
}

let session: Session | null = loadSession()
const listeners = new Set<() => void>()

function setSession(next: Session | null): void {
  session = next
  try {
    if (next) localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    /* stockage indisponible : la session reste en mémoire */
  }
  listeners.forEach((l) => l())
}

export const getSession = (): Session | null => session

export function subscribeSession(listener: () => void): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

function toSession(t: TokenResponse): Session {
  return {
    accessToken: t.access_token,
    refreshToken: t.refresh_token,
    expiresAt: Date.now() + t.expires_in * 1000,
    scope: t.scope,
  }
}

async function toError(response: Response): Promise<ApiError> {
  let problem: Problem | undefined
  try {
    problem = (await response.json()) as Problem
  } catch {
    /* corps vide ou non JSON */
  }
  return new ApiError(response.status, problem?.detail ?? problem?.title ?? response.statusText, problem)
}

async function send(path: string, init: RequestInit, base: string = API_URL): Promise<Response> {
  try {
    return await fetch(`${base}${path}`, init)
  } catch {
    throw new ApiError(0, 'Serveur injoignable. Le backend est-il démarré ?')
  }
}

async function tokenRequest(body: Record<string, string>): Promise<TokenResponse> {
  const response = await send('/auth/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(body),
  }, IAM_URL)
  if (!response.ok) throw await toError(response)
  return (await response.json()) as TokenResponse
}

let refreshing: Promise<boolean> | null = null

/** Renouvelle les jetons ; une seule requête à la fois. Déconnecte si le renouvellement échoue. */
function refresh(): Promise<boolean> {
  if (!session) return Promise.resolve(false)
  const refreshToken = session.refreshToken
  refreshing ??= (async () => {
    try {
      setSession(toSession(await tokenRequest({ grant_type: 'refresh_token', client_id: CLIENT_ID, refresh_token: refreshToken })))
      return true
    } catch {
      setSession(null)
      return false
    } finally {
      refreshing = null
    }
  })()
  return refreshing
}

/** Jeton d'accès valide (renouvelé s'il expire dans moins de 30 s), ou null si non connecté. */
export async function getFreshAccessToken(): Promise<string | null> {
  if (session && session.expiresAt - Date.now() < 30_000) await refresh()
  return session?.accessToken ?? null
}

export async function login(username: string, password: string): Promise<void> {
  setSession(toSession(await tokenRequest({ grant_type: 'password', client_id: CLIENT_ID, username, password })))
}

export function logout(): void {
  setSession(null)
}

// ---------- Requêtes ----------

async function request<T>(path: string, init: RequestInit = {}, base: string = API_URL, canRetry = true): Promise<T> {
  const token = await getFreshAccessToken()
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await send(path, { ...init, headers }, base)
  if (response.status === 401 && token && canRetry && (await refresh())) {
    return request<T>(path, init, base, false)
  }
  if (!response.ok) throw await toError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

const enc = encodeURIComponent

function query(params: Record<string, string | number | undefined>): string {
  const entries = Object.entries(params).filter(([, v]) => v !== undefined)
  if (entries.length === 0) return ''
  return '?' + entries.map(([k, v]) => `${enc(k)}=${enc(String(v))}`).join('&')
}

export const api = {
  health: () => request<{ status: 'UP' }>('/health'),

  /** Inscription (rôle automobiliste), puis connexion automatique. */
  register: async (username: string, password: string) => {
    await request<Identity>('/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) }, IAM_URL)
    await login(username, password)
  },
  me: () => request<Me>('/auth/me', {}, IAM_URL),

  listParkings: () => request<Parking[]>('/parkings'),
  getParking: (id: string) => request<Parking>(`/parkings/${enc(id)}`),
  listSlots: (parkingId: string, state?: SlotState) =>
    request<Slot[]>(`/parkings/${enc(parkingId)}/slots${query({ state })}`),
  /** Opérateur uniquement. Réponse 202 : le résultat final arrive par WebSocket (command_ack). */
  openBarrier: (parkingId: string, reason: string) =>
    request<Command>(`/parkings/${enc(parkingId)}/barrier/open`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    }),

  listThings: (filter: { parkingId?: string; state?: 'online' | 'offline' } = {}) =>
    request<Thing[]>(`/things${query(filter)}`),

  listAlerts: (filter: { parkingId?: string; status?: 'open' | 'acknowledged'; limit?: number } = {}) =>
    request<Alert[]>(`/alerts${query(filter)}`),
  acknowledgeAlert: (alertId: string) =>
    request<Alert>(`/alerts/${enc(alertId)}/acknowledge`, { method: 'POST' }),

  // Réservations : un automobiliste voit les siennes, l'exploitant voit toutes (voir ReservationManager.java)
  listReservations: (parkingId?: string) => request<Reservation[]>(`/reservations${query({ parkingId })}`),
  createReservation: (body: CreateReservationRequest) =>
    request<Reservation>('/reservations', { method: 'POST', body: JSON.stringify(body) }),
  cancelReservation: (id: string) => request<void>(`/reservations/${enc(id)}`, { method: 'DELETE' }),

  // Administration des comptes (rôle ADMIN) : service IAM
  listIdentities: () => request<Identity[]>('/identities', {}, IAM_URL),
  createIdentity: (body: { username: string; password: string; roles: string[] }) =>
    request<Identity>('/identities', { method: 'POST', body: JSON.stringify(body) }, IAM_URL),
}
