#!/usr/bin/env bash
# Lupo Cloud — Einzeiler-Installation für einen leeren Root-Server.
#
# Nur diese Datei wird auf dem Server gebraucht:
#   curl -fsSL https://raw.githubusercontent.com/simstoe/lupo-cloud/master/install.sh -o install.sh
#   chmod +x install.sh
#   ./install.sh
#
# Übernimmt: Docker installieren (falls nötig), Repo holen/aktualisieren,
# .env interaktiv einrichten, Images bauen und starten.
set -euo pipefail

REPO_URL="https://github.com/simstoe/lupo-cloud.git"
INSTALL_DIR="lupo-cloud"

log()  { echo -e "\033[1;32m==>\033[0m $*"; }
warn() { echo -e "\033[1;33m==>\033[0m $*"; }

# 1. Docker installieren, falls nicht vorhanden
if ! command -v docker >/dev/null 2>&1; then
    log "Docker ist nicht installiert — installiere über get.docker.com..."
    curl -fsSL https://get.docker.com | sh
else
    log "Docker ist bereits installiert ($(docker --version))."
fi

if ! docker compose version >/dev/null 2>&1; then
    warn "docker compose (Plugin) wurde nicht gefunden. Bitte Docker aktualisieren und erneut ausführen."
    exit 1
fi

# 2. Repo holen — falls install.sh außerhalb eines Checkouts läuft, selbst klonen.
if [ -f docker-compose.yml ] && [ -f Dockerfile ]; then
    log "Bestehendes Repo-Checkout erkannt, verwende aktuelles Verzeichnis."
elif [ -d "$INSTALL_DIR" ]; then
    log "Verzeichnis '$INSTALL_DIR' existiert bereits, aktualisiere..."
    cd "$INSTALL_DIR"
    git pull
else
    log "Klone Lupo Cloud nach '$INSTALL_DIR'..."
    git clone "$REPO_URL" "$INSTALL_DIR"
    cd "$INSTALL_DIR"
fi

# 3. .env einrichten (nur beim ersten Mal)
if [ -f .env ]; then
    log ".env existiert bereits, wird nicht verändert."
    # shellcheck disable=SC1091
    set -a; source .env; set +a
else
    log "Ersteinrichtung — ein paar Angaben zum Server:"
    read -rp "  Öffentliche Adresse dieses Servers (Domain oder IP) [localhost]: " PUBLIC_HOST
    PUBLIC_HOST=${PUBLIC_HOST:-localhost}
    read -rp "  Backend-Port [8080]: " BACKEND_PORT
    BACKEND_PORT=${BACKEND_PORT:-8080}
    read -rp "  Dashboard-Port [3000]: " DASHBOARD_PORT
    DASHBOARD_PORT=${DASHBOARD_PORT:-3000}

    cat > .env <<EOF
PUBLIC_HOST=$PUBLIC_HOST
BACKEND_PORT=$BACKEND_PORT
DASHBOARD_PORT=$DASHBOARD_PORT
EOF
    log ".env geschrieben."
fi

# 4. Bauen und starten
log "Baue und starte Lupo Cloud (kann beim ersten Mal einige Minuten dauern)..."
docker compose up -d --build

# 5. Auf den ersten Start warten und die Login-Infos aus dem Backend-Log anzeigen
log "Warte auf den ersten Start des Backends..."
ready=""
for _ in $(seq 1 90); do
    if docker compose logs backend 2>/dev/null | grep -q "LUPO CLOUD READY"; then
        ready="1"
        break
    fi
    sleep 2
done

echo
if [ -n "$ready" ]; then
    docker compose logs backend 2>/dev/null | grep -A4 "LUPO CLOUD READY" | tail -n 5
else
    warn "Backend hat sich nicht rechtzeitig gemeldet — Status prüfen mit:"
    echo "  docker compose logs -f backend"
fi
echo
log "Fertig. Dashboard: http://${PUBLIC_HOST:-localhost}:${DASHBOARD_PORT:-3000}"
