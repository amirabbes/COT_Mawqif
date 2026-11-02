#!/usr/bin/env python3
"""
Simulateur Mawqif : envoie de faux événements MQTT (entrée d'une voiture,
place occupée, place libérée, mauvais stationnement...) pour tester le backend
sans caméras ni Raspberry Pi.

Utilisation (depuis la racine du dépôt) :
    pip install -r simulator/requirements.txt
    python simulator/simulator.py                  # MQTT simple, port 1883
    python simulator/simulator.py --tls            # MQTT sécurisé, port 1882
    python simulator/simulator.py --count 20       # s'arrête après 20 événements

Le nom d'utilisateur et le mot de passe sont lus dans le fichier .env
(MQTT_USER et MQTT_PASSWORD).

Topics utilisés (même convention que le backend du prof) :
    things/<objet>/status            état d'un objet (online / offline)
    things/<objet>/datum             données : voiture détectée, état d'une place...
    alerts/<parking>/<type>          alertes : parking_full, wrong_parking
"""
import argparse
import json
import os
import random
import ssl
import sys
import threading
import time
from datetime import datetime, timezone
from pathlib import Path

import paho.mqtt.client as mqtt

ROOT = Path(__file__).resolve().parent.parent


def load_env(path: Path) -> None:
    """Lit un fichier .env simple (CLE=valeur) sans écraser les variables déjà définies."""
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip())


def now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def random_plate() -> str:
    return f"{random.randint(100, 999)} TUN {random.randint(1000, 9999)}"


class Simulator:
    DEVICES = ["camera-entree", "camera-interieure", "capteur-distance", "barriere"]

    def __init__(self, client: mqtt.Client, parking: str, slots: int):
        self.client = client
        self.parking = parking
        self.slots = slots
        self.occupied: dict[int, str] = {}  # numéro de place -> plaque

    def publish(self, topic: str, payload: dict) -> None:
        text = json.dumps(payload, ensure_ascii=False)
        self.client.publish(topic, text, qos=1).wait_for_publish(timeout=5)
        print(f"-> {topic}  {text}")

    def heartbeat(self) -> None:
        for device in self.DEVICES:
            self.publish(f"things/{device}/status", {"state": "online", "ts": now()})

    def free_slots(self) -> list[int]:
        return [n for n in range(1, self.slots + 1) if n not in self.occupied]

    def car_arrives(self) -> None:
        plate = random_plate()
        self.publish("things/camera-entree/datum",
                     {"event": "vehicle_entered", "parking": self.parking,
                      "plate": plate, "ts": now()})
        free = self.free_slots()
        if not free:
            self.publish(f"alerts/{self.parking}/parking_full",
                         {"plate": plate, "message": "Parking complet", "ts": now()})
            return
        time.sleep(0.5)  # le temps de rejoindre la place
        slot = random.choice(free)
        self.occupied[slot] = plate
        self.publish(f"things/place-{slot:02d}/datum",
                     {"event": "slot_state", "parking": self.parking, "slot": slot,
                      "state": "occupied", "plate": plate, "ts": now()})

    def car_leaves(self) -> None:
        slot = random.choice(list(self.occupied))
        plate = self.occupied.pop(slot)
        self.publish(f"things/place-{slot:02d}/datum",
                     {"event": "slot_state", "parking": self.parking, "slot": slot,
                      "state": "free", "plate": plate, "ts": now()})
        time.sleep(0.5)
        self.publish("things/camera-entree/datum",
                     {"event": "vehicle_exited", "parking": self.parking,
                      "plate": plate, "ts": now()})

    def wrong_parking(self) -> None:
        slot = random.choice(list(self.occupied))
        self.publish(f"alerts/{self.parking}/wrong_parking",
                     {"slot": slot, "plate": self.occupied[slot],
                      "reason": random.choice(["deux places occupées", "mal garé (angle)"]),
                      "ts": now()})

    def step(self) -> None:
        """Choisit au hasard un événement réaliste."""
        roll = random.random()
        if self.occupied and roll < 0.05:
            self.wrong_parking()
        elif self.occupied and roll < 0.40:
            self.car_leaves()
        else:
            self.car_arrives()


def connect(args) -> mqtt.Client:
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="mawqif-simulator")
    user, password = os.environ.get("MQTT_USER"), os.environ.get("MQTT_PASSWORD")
    if user:
        client.username_pw_set(user, password)
    if args.tls:
        client.tls_set(ca_certs=str(args.ca), cert_reqs=ssl.CERT_REQUIRED)
    client.will_set("things/passerelle/status", json.dumps({"state": "offline"}), qos=1)

    connected = threading.Event()
    result = {}

    def on_connect(_client, _userdata, _flags, reason_code, _properties):
        result["rc"] = reason_code
        connected.set()

    client.on_connect = on_connect
    try:
        client.connect(args.host, args.port)
    except Exception as e:  # noqa: BLE001
        sys.exit(f"Impossible de se connecter à {args.host}:{args.port} : {e}\n"
                 "Les conteneurs tournent-ils ?  (docker compose ps)")
    client.loop_start()
    if not connected.wait(timeout=10) or result["rc"].is_failure:
        client.loop_stop()
        sys.exit(f"Connexion refusée ou sans réponse ({result.get('rc')}).\n"
                 "Vérifiez MQTT_USER / MQTT_PASSWORD dans .env.")
    print(f"Connecté à {args.host}:{args.port} ({'TLS' if args.tls else 'sans TLS'})")
    return client


def main() -> None:
    load_env(ROOT / ".env")
    p = argparse.ArgumentParser(description="Simulateur d'événements MQTT pour Mawqif")
    p.add_argument("--host", default="localhost")
    p.add_argument("--port", type=int, help="1883 par défaut, 1882 avec --tls")
    p.add_argument("--tls", action="store_true", help="connexion sécurisée (TLS 1.3)")
    p.add_argument("--ca", type=Path, default=ROOT / "infra" / "certs" / "ca.crt")
    p.add_argument("--parking", default="parking-1")
    p.add_argument("--slots", type=int, default=10, help="nombre de places")
    p.add_argument("--interval", type=float, default=3.0, help="secondes entre deux événements")
    p.add_argument("--count", type=int, default=0, help="nombre d'événements (0 = sans fin)")
    p.add_argument("--seed", type=int, help="pour obtenir toujours la même séquence")
    args = p.parse_args()
    args.port = args.port or (1882 if args.tls else 1883)
    if args.seed is not None:
        random.seed(args.seed)

    client = connect(args)
    sim = Simulator(client, args.parking, args.slots)
    sim.heartbeat()
    sim.publish("things/passerelle/status", {"state": "online", "ts": now()})
    print("Simulation en cours (Ctrl+C pour arrêter)...")

    sent, last_beat = 0, time.time()
    try:
        while args.count == 0 or sent < args.count:
            sim.step()
            sent += 1
            if time.time() - last_beat > 15:
                sim.heartbeat()
                last_beat = time.time()
            time.sleep(args.interval)
    except KeyboardInterrupt:
        print("\nArrêt demandé.")
    finally:
        sim.publish("things/passerelle/status", {"state": "offline", "ts": now()})
        client.loop_stop()
        client.disconnect()


if __name__ == "__main__":
    main()