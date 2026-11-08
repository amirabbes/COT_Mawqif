# Contrats d'interface — Mawqif

| Interface | Entre | Fichier |
|---|---|---|
| REST `/api/v1` | PWA ↔ backend | [`openapi.yaml`](openapi.yaml) (OpenAPI 3.1) |
| MQTT | passerelle/objets ↔ backend | [`mqtt-contract.md`](mqtt-contract.md) + [`mqtt-messages.schema.json`](mqtt-messages.schema.json) |
| WebSocket | backend → PWA (temps réel) | section ci-dessous |

Visualiser l'API REST : coller `openapi.yaml` dans <https://editor.swagger.io>, ou
`npx @redocly/cli preview-docs docs/contracts/openapi.yaml`.

## WebSocket (temps réel vers les PWA)

URL : `ws://localhost:8081/api/ws/{view}` avec `view` = `driver` ou `admin`.
Le serveur envoie uniquement ; le client n'envoie rien (sauf ping).
Chaque message est une enveloppe JSON :

```json
{ "type": "slot_updated", "data": { "...": "..." }, "ts": "2026-11-10T08:30:12Z" }
```

| `type` | `data` (schéma REST) | Vue |
|---|---|---|
| `parking_updated` | `Parking` (compteurs) | `driver`, `admin` |
| `slot_updated` | `Slot` | `driver`, `admin` |
| `thing_status` | `Thing` | `admin` |
| `alert_created` | `Alert` | `admin` |
| `command_ack` | `Command` | `admin` |

L'authentification du WebSocket (jeton Keycloak) sera ajoutée avec la sécurité (semaine 6).

## Correspondance MQTT → REST/WebSocket

| Message MQTT reçu | Mise à jour en base | Diffusion WebSocket |
|---|---|---|
| `things/<id>/status` | `Thing.state`, `Thing.lastSeen` | `thing_status` |
| `datum` `slot_state` | `ParkingSlot.state`, compteurs du `Parking` | `slot_updated`, `parking_updated` |
| `datum` `barrier_state` | `Parking.barrierState` | `parking_updated` |
| `datum` `vehicle_entered` / `vehicle_exited` | journal `Datum` (décision d'accès : semaine suivante) | — |
| `alerts/<parking>/<type>` | nouvelle `Alert` | `alert_created` |
| `things/<barrière>/ack` | `BarrierCommand.status` | `command_ack` |
