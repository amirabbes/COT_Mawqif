# Contrat MQTT — Mawqif

Version 1.0 · Broker Mosquitto · `mqtts://mosquitto:1882` (TLS 1.3, backend) et `mqtt://localhost:1883` (local : Node-RED, simulateur).
Les schémas JSON de chaque message sont dans [`mqtt-messages.schema.json`](mqtt-messages.schema.json).
Vérification automatique : `python simulator/check_contract.py`.

## Principes

- Même convention de topics que le backend du prof : `things/<objet>/status`, `things/<objet>/datum`, `alerts/<parking>/<type>`.
- Charge utile : JSON UTF-8. Horodatage `ts` en ISO-8601 UTC (`2026-11-10T08:30:00Z`).
- Les noms d'objets (`<thingId>`) sont stables : `camera-entree`, `camera-interieure`, `capteur-distance`, `barriere`, `passerelle`, `place-01` … `place-NN`.
- **Le backend est le seul à décider** (ouvrir la barrière, refuser une plaque). La passerelle et l'IA ne font que publier des observations.

## Topics

| Topic | Sens | Qui publie | QoS | Retain | Schéma |
|---|---|---|---|---|---|
| `things/<thingId>/status` | objet → backend | l'objet (et son *Last Will*) | 1 | oui | `status` |
| `things/<thingId>/datum` | objet → backend | caméra, capteur, place, barrière | 1 | non | `datum` |
| `alerts/<parkingId>/<type>` | passerelle/backend → tous | passerelle (règles en périphérie) ou backend | 1 | non | `alert_<type>` |
| `things/<thingId>/cmd` | backend → objet | backend uniquement | 1 | non | `cmd` |
| `things/<thingId>/ack` | objet → backend | la barrière | 1 | non | `ack` |

Types d'alerte : `parking_full`, `wrong_parking`, `plate_refused`, `thing_offline`.

Abonnements du backend : `things/+/status`, `things/+/datum`, `things/+/ack`, `alerts/+/+`.
Abonnements de la barrière : `things/barriere/cmd`.

## Messages `datum`

Le champ `event` choisit la variante.

| `event` | Publié par | Champs obligatoires | Sens métier |
|---|---|---|---|
| `vehicle_entered` | `camera-entree` | `parking`, `plate`, `ts` | plaque lue à l'entrée : le backend décide |
| `vehicle_exited` | `camera-entree` | `parking`, `plate`, `ts` | plaque lue à la sortie : durée calculée |
| `slot_state` | `place-NN` | `parking`, `slot`, `state` (`free`/`occupied`), `ts` | état d'une place |
| `distance_reading` | `capteur-distance` | `parking`, `distance_cm`, `ts` | mesure brute (optionnel) |
| `barrier_state` | `barriere` | `parking`, `state` (`open`/`closed`), `ts` | position réelle de la barrière |

Exemples :

```json
{"event":"vehicle_entered","parking":"parking-1","plate":"123 TUN 4567","ts":"2026-11-10T08:30:00Z"}
{"event":"slot_state","parking":"parking-1","slot":7,"state":"occupied","plate":"123 TUN 4567","ts":"2026-11-10T08:30:12Z"}
```

## Statut et Last Will

Chaque objet publie `{"state":"online"}` (retenu) à la connexion et déclare un *Last Will* `{"state":"offline"}` sur son propre topic `status`. Le backend passe alors l'objet à `offline` et lève une alerte `thing_offline`.

## Commande de barrière (backend → objet)

```json
{
  "cmdId": "019a6c3e-7b1a-7c2e-8f10-2d4b9c1e5a77",
  "command": "open_barrier",
  "parking": "parking-1",
  "reason": "ouverture manuelle opérateur",
  "issuedAt": "2026-11-10T08:30:00Z",
  "expiresAt": "2026-11-10T08:30:10Z",
  "sig": "<base64url(HMAC-SHA256)>"
}
```

Règles de l'objet qui reçoit la commande :

1. Recalculer `sig = base64url(HMAC-SHA256(clé, "cmdId|command|parking|issuedAt|expiresAt"))` et comparer. Différent → ignorer et répondre `rejected`.
2. `expiresAt` dépassé → `rejected` (protection contre le rejeu).
3. `cmdId` déjà exécuté → ne pas rouvrir (idempotence).
4. Sinon ouvrir, publier `ack` puis un `datum` `barrier_state`.

```json
{"cmdId":"019a6c3e-7b1a-7c2e-8f10-2d4b9c1e5a77","result":"executed","ts":"2026-11-10T08:30:01Z"}
```

La clé de signature est partagée en développement (variable `COMMAND_SIGNING_KEY` du `.env`). Prévu plus tard : certificats X.509 (mTLS) et signature asymétrique.

## Évolution du contrat

- Ajouter un champ optionnel : compatible, pas de changement de version.
- Renommer ou supprimer un champ, changer un type : nouvelle version (`things/...` reste, on ajoute `"v": 2` dans le message) et revue d'équipe.
- Toute modification passe par une PR qui met à jour ce fichier **et** le schéma JSON ; `check_contract.py` doit rester vert.
