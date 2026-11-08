import type {
  Alert, Command, CreateReservationRequest, Parking, Problem, Reservation, Slot, SlotState, Thing,
} from './types'

// VITE_API_URL = adresse du backend SANS /api/v1 (ex. http://localhost:8081, voir .env.example)
const BASE = (import.meta.env.VITE_API_URL ?? 'http://localhost:8081').replace(/\/$/, '')
export const API_URL = `${BASE}/api/v1`
export const WS_URL = `${BASE.replace(/^http/, 'ws')}/api/ws`

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

let accessToken: string | null = null

/** À appeler après la connexion Keycloak (semaine 6). Sans jeton, aucun en-tête Authorization n'est envoyé. */
export function setAccessToken(token: string | null): void {
  accessToken = token
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body) headers.set('Content-Type', 'application/json')
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)

  let response: Response
  try {
    response = await fetch(`${API_URL}${path}`, { ...init, headers })
  } catch {
    throw new ApiError(0, 'Serveur injoignable. Le backend est-il démarré ?')
  }

  if (!response.ok) {
    let problem: Problem | undefined
    try {
      problem = (await response.json()) as Problem
    } catch {
      /* corps vide ou non JSON */
    }
    throw new ApiError(response.status, problem?.detail ?? problem?.title ?? response.statusText, problem)
  }
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

  // Réservations : l'API répond 501 tant que le backend ne les gère pas (voir ReservationResource.java)
  listReservations: () => request<Reservation[]>('/reservations'),
  createReservation: (body: CreateReservationRequest) =>
    request<Reservation>('/reservations', { method: 'POST', body: JSON.stringify(body) }),
  cancelReservation: (id: string) => request<void>(`/reservations/${enc(id)}`, { method: 'DELETE' }),
}
