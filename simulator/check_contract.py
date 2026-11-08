#!/usr/bin/env python3
"""
Vérifie que les messages du simulateur respectent le contrat MQTT
(docs/contracts/mqtt-messages.schema.json). Aucun broker n'est nécessaire.

Utilisation (depuis la racine du dépôt) :
    pip install jsonschema
    python simulator/check_contract.py            # 200 événements
    python simulator/check_contract.py --count 1000
"""
import argparse
import json
import random
import sys
from pathlib import Path

from jsonschema import Draft202012Validator, FormatChecker

sys.path.insert(0, str(Path(__file__).resolve().parent))
import simulator as sim  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
SCHEMA = json.loads((ROOT / "docs" / "contracts" / "mqtt-messages.schema.json").read_text(encoding="utf-8"))


def schema_for(topic: str) -> str | None:
    """Associe un topic au schéma du contrat."""
    parts = topic.split("/")
    if parts[0] == "things" and len(parts) == 3:
        return {"status": "status", "datum": "datum", "cmd": "cmd", "ack": "ack"}.get(parts[2])
    if parts[0] == "alerts" and len(parts) == 3:
        return f"alert_{parts[2]}"
    return None


def validator(name: str) -> Draft202012Validator:
    return Draft202012Validator({"$ref": f"#/$defs/{name}", "$defs": SCHEMA["$defs"]},
                                format_checker=FormatChecker())


class FakePublish:
    def wait_for_publish(self, timeout=None):
        return True


class FakeClient:
    """Remplace paho : enregistre les messages au lieu de les envoyer."""
    def __init__(self):
        self.messages: list[tuple[str, dict]] = []

    def publish(self, topic, payload, qos=0, retain=False):
        self.messages.append((topic, json.loads(payload)))
        return FakePublish()


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--count", type=int, default=200)
    p.add_argument("--slots", type=int, default=10)
    args = p.parse_args()

    random.seed(1)
    client = FakeClient()
    simulator = sim.Simulator(client, "parking-1", args.slots)
    sim.time.sleep = lambda _s: None  # pas d'attente
    simulator.heartbeat()
    for _ in range(args.count):
        simulator.step()

    errors, seen = 0, {}
    for topic, payload in client.messages:
        name = schema_for(topic)
        if name is None or name not in SCHEMA["$defs"]:
            print(f"[INCONNU] topic hors contrat : {topic}")
            errors += 1
            continue
        seen[name] = seen.get(name, 0) + 1
        for err in validator(name).iter_errors(payload):
            print(f"[ERREUR] {topic} : {err.message}\n         {json.dumps(payload, ensure_ascii=False)}")
            errors += 1

    print(f"\n{len(client.messages)} messages vérifiés : {seen}")
    print("OK : le simulateur respecte le contrat." if errors == 0 else f"{errors} erreur(s).")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
