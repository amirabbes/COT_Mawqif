// Types de l'API REST — miroir de docs/contracts/openapi.yaml.
// Si le contrat change, mettre à jour ce fichier (ou le générer :
//   npx openapi-typescript ../../docs/contracts/openapi.yaml -o src/api/schema.d.ts).

export type SlotState = 'free' | 'occupied' | 'reserved'
export type ThingState = 'online' | 'offline'
export type ThingType = 'camera' | 'distance_sensor' | 'barrier' | 'gateway'
export type AlertType = 'parking_full' | 'wrong_parking' | 'plate_refused' | 'thing_offline'
export type AlertStatus = 'open' | 'acknowledged'
export type BarrierState = 'open' | 'closed' | 'unknown'
export type CommandStatus = 'sent' | 'executed' | 'rejected' | 'expired'

export interface Parking {
  id: string
  name: string
  lat: number | null
  lon: number | null
  totalSlots: number
  freeSlots: number
  barrierState: BarrierState
  updatedAt: string | null
}

export interface Slot {
  id: string
  parkingId: string
  number: number
  state: SlotState
  plate: string | null
  updatedAt: string | null
}

export interface Thing {
  id: string
  type: ThingType
  parkingId: string | null
  state: ThingState
  lastSeen: string | null
  lastDatum: string | null
}

export interface Alert {
  id: string
  parkingId: string
  type: AlertType
  status: AlertStatus
  slot: number | null
  plate: string | null
  message: string | null
  createdAt: string
  acknowledgedAt: string | null
}

export interface Command {
  id: string
  thingId: string
  command: 'open_barrier'
  status: CommandStatus
  issuedAt: string
  expiresAt: string
}

export interface Reservation {
  id: string
  parkingId: string
  plate: string
  from: string
  to: string
  status: 'active' | 'used' | 'cancelled' | 'expired'
}

export interface CreateReservationRequest {
  parkingId: string
  plate: string
  from: string
  to: string
}

/** Erreur application/problem+json (RFC 9457). */
export interface Problem {
  type?: string
  title: string
  status: number
  detail?: string
  instance?: string
}

// ---------- WebSocket (voir docs/contracts/README.md) ----------

export type LiveView = 'driver' | 'admin'

export type LiveMessage =
  | { type: 'parking_updated'; data: Parking; ts: string }
  | { type: 'slot_updated'; data: Slot; ts: string }
  | { type: 'thing_status'; data: Thing; ts: string }
  | { type: 'alert_created'; data: Alert; ts: string }
  | { type: 'command_ack'; data: Command; ts: string }
