#!/bin/sh
# Applique les scripts .cli de l'enseignant à la configuration standalone.xml de WildFly, en mode « embarqué » :
# le serveur n'a pas besoin d'être démarré ni d'avoir un utilisateur d'administration.
#
#   étape 1  default-host.cli + add-host.cli   PWA automobiliste (default-host) et PWA exploitant (admin-host)
#   étape 2  add-api.cli  (api-*.properties)   api-1.0.war sur api-host
#   étape 3  add-api.cli  (iam-*.properties)   iam-1.0.war sur iam-host
#
# Chaque étape n'est appliquée que si elle ne l'est pas déjà (idempotent, et une ancienne configuration est complétée).
# Si une commande échoue, standalone.xml est restauré tel qu'il était.
#
# Variables : WF_ENV   dev (défaut) ou prod -> choisit les fichiers *-dev.properties / *-prod.properties
#             WF_DIR   dossier contenant cli/ et les .properties (défaut : dossier de ce script)
set -eu

JBOSS_HOME="${JBOSS_HOME:-/opt/jboss/wildfly}"
WF_DIR="${WF_DIR:-$(cd "$(dirname "$0")" && pwd)}"
WF_ENV="${WF_ENV:-dev}"
CONFIG="$JBOSS_HOME/standalone/configuration/standalone.xml"
WORK="$(mktemp -d)"

has_host() { grep -q "<host name=\"$1\"" "$CONFIG"; }
has_filter() { grep -q "name=\"$1\"" "$CONFIG"; }

if has_filter hsts-header && has_host admin-host && has_host api-host && has_host iam-host; then
  echo ">>> Configuration WildFly déjà appliquée : rien à faire."
  exit 0
fi

echo ">>> Application des scripts .cli (environnement : $WF_ENV)"
cp "$CONFIG" "$WORK/standalone.xml.orig"
restore() {
  echo "!!! Échec : restauration de standalone.xml" >&2
  cp "$WORK/standalone.xml.orig" "$CONFIG"
}
trap 'restore' EXIT

# clean FICHIER : retire les retours chariot Windows (tr -d '\015') et ajoute le saut de ligne final qui manque à certains scripts.
clean() { tr -d '\015' < "$1"; echo; }

# run_cli "propriétés..." "scripts..." : une session embarquée avec ces propriétés.
run_cli() {
  props="$1"; shift
  : > "$WORK/p.properties"
  for f in $props; do clean "$WF_DIR/$f-$WF_ENV.properties" >> "$WORK/p.properties"; done
  {
    echo 'embed-server --server-config=standalone.xml --std-out=echo'
    for f in "$@"; do
      # « reload » n'a pas de sens hors ligne : la configuration est écrite à la fin de la session.
      clean "$WF_DIR/cli/$f" | grep -v '^reload$'
    done
    echo 'stop-embedded-server'
  } > "$WORK/s.cli"
  "$JBOSS_HOME/bin/jboss-cli.sh" --file="$WORK/s.cli" --properties="$WORK/p.properties"
}

# Étape 1 : default-host.cli n'est pas rejouable (il crée les filtres communs), add-host.cli non plus.
if ! has_filter hsts-header || ! has_host admin-host; then
  if has_filter hsts-header || has_host admin-host; then
    echo "!!! Configuration partielle inattendue (étape 1). Supprimer le volume wildfly_conf : docker compose ... down -v" >&2
    exit 1
  fi
  run_cli "default-host admin-host" default-host.cli add-host.cli
fi
# Étapes 2 et 3 : add-api.cli ignore lui-même un hôte déjà créé, mais on évite de relancer WildFly pour rien.
has_host api-host || run_cli "api" add-api.cli
has_host iam-host || run_cli "iam" add-api.cli

# Vérification : les quatre hôtes virtuels doivent exister.
for host in default-host admin-host api-host iam-host; do
  if ! has_host "$host"; then
    echo "!!! Hôte virtuel manquant après configuration : $host" >&2
    exit 1
  fi
done
trap - EXIT
echo ">>> OK : hôtes virtuels default-host, admin-host, api-host et iam-host configurés."
