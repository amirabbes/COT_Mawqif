#!/usr/bin/env bash
# À lancer UNE FOIS depuis la racine du dépôt :  bash infra/setup-dev.sh
# Crée tout ce qu'il faut pour que le broker MQTT sécurisé fonctionne en local :
#   - une autorité de certification de test (CA) et un certificat pour Mosquitto
#   - le fichier truststore.jks (utilisé par le backend Java pour faire confiance à la CA)
#   - le fichier de mots de passe de Mosquitto
# Outils nécessaires : openssl, keytool (livré avec Java), docker.

set -euo pipefail

# Windows (Git Bash) : empêche la transformation automatique des chemins
# (sinon "/CN=..." et "/work" sont modifiés et openssl / docker échouent).
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "Erreur : le fichier .env n'existe pas. Faites d'abord :  cp .env.example .env" >&2
  exit 1
fi
set -a; source .env; set +a

CERTS=infra/certs
mkdir -p "$CERTS"

echo "1/4  Autorité de certification (CA) de test..."
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out "$CERTS/ca.key" 2>/dev/null
openssl req -x509 -new -key "$CERTS/ca.key" -sha256 -days 825 \
  -subj "/CN=Mawqif Dev CA" -out "$CERTS/ca.crt"

echo "2/4  Certificat du broker (valable pour 'mosquitto' et 'localhost')..."
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out "$CERTS/server.key" 2>/dev/null
openssl req -new -key "$CERTS/server.key" -subj "/CN=mosquitto" -out "$CERTS/server.csr"
printf "subjectAltName=DNS:mosquitto,DNS:localhost,IP:127.0.0.1\n" > "$CERTS/server.ext"
openssl x509 -req -in "$CERTS/server.csr" -CA "$CERTS/ca.crt" -CAkey "$CERTS/ca.key" \
  -CAcreateserial -out "$CERTS/server.crt" -days 825 -sha256 -extfile "$CERTS/server.ext" 2>/dev/null

echo "3/4  Truststore Java (truststore.jks)..."
rm -f "$CERTS/truststore.jks"
keytool -importcert -noprompt -alias mawqif-ca -file "$CERTS/ca.crt" \
  -keystore "$CERTS/truststore.jks" -storetype JKS -storepass "$TRUSTSTORE_PASSWORD" >/dev/null

# Les conteneurs lisent ces fichiers avec un autre utilisateur : on autorise la lecture (dev seulement).
chmod 644 "$CERTS"/*.crt "$CERTS"/server.key "$CERTS"/truststore.jks

echo "4/4  Mot de passe MQTT..."
if ! command -v docker >/dev/null 2>&1; then
  echo "Erreur : docker est introuvable (il sert à créer le fichier de mots de passe)." >&2
  exit 1
fi
rm -f infra/mosquitto/passwd
docker run --rm -v "$PWD/infra/mosquitto:/work" eclipse-mosquitto:2 \
  mosquitto_passwd -b -c /work/passwd "$MQTT_USER" "$MQTT_PASSWORD"
chmod 644 infra/mosquitto/passwd

echo
echo "Terminé. Vous pouvez lancer :  docker compose up -d"