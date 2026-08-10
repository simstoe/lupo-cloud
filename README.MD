# Lupo Cloud — Installation auf einem Root-Server

Backend (Java) und Dashboard (Next.js) laufen als zwei Docker-Container. Der Server braucht dafür **ausschließlich Docker** — kein Java, kein Node.js direkt installiert.

## Voraussetzungen

- Ein leerer Root-Server (Linux, x86_64 oder arm64)
- Öffentlich erreichbare IP oder Domain (z. B. `cloud.example.com` oder `203.0.113.10`)
- Ports 8080 (Backend) und 3000 (Dashboard) offen — oder eigene Ports wählen (Installer fragt danach)

## Installation

Auf dem Server wird nur eine einzige Datei gebraucht — `install.sh`. Sie installiert Docker (falls nötig), holt das Repo, fragt interaktiv nach `PUBLIC_HOST` und den Ports, baut die Images und startet den Stack:

```bash
curl -fsSL https://raw.githubusercontent.com/simstoe/lupo-cloud/master/install.sh -o install.sh
chmod +x install.sh
./install.sh
```

Am Ende zeigt die Konsole automatisch, wie man sich im Dashboard einloggt (URL und beim allerersten Start das generierte Admin-Passwort).

Für Updates einfach erneut ausführen (bereits vorhandenes `.env` und Repo werden übernommen bzw. aktualisiert):

```bash
./install.sh
```

## Admin-Passwort später nachschlagen

Steht nicht mehr in der Konsole (z. B. nach einem Neustart, weil es nur beim allerersten Start ausgegeben wird)? Direkt aus dem Volume lesen:

```bash
docker compose exec backend cat local/web-admin.secret
```

## Nicht Teil dieser Anleitung

- TLS/Reverse-Proxy (z. B. nginx, Caddy, Traefik) — für eine öffentlich erreichbare Domain mit HTTPS separat einrichten
- Firewall-Konfiguration
- Multi-Server-/Cluster-Setups — diese Anleitung deckt einen einzelnen Root-Server ab

---

## Manuelle Installation (Referenz)

Für den Fall, dass `install.sh` nicht genutzt werden soll — das sind die einzelnen Schritte, die der Installer automatisiert.

### 1. Docker installieren

```bash
curl -fsSL https://get.docker.com | sh
```

Prüfen:

```bash
docker --version
docker compose version
```

### 2. Repo clonen

```bash
git clone https://github.com/simstoe/lupo-cloud.git
cd lupo-cloud
```

### 3. `.env` konfigurieren

```bash
cp .env.example .env
```

`.env` bearbeiten und `PUBLIC_HOST` auf die öffentliche Adresse des Servers setzen:

```dotenv
PUBLIC_HOST=cloud.example.com
BACKEND_PORT=8080
DASHBOARD_PORT=3000
```

`PUBLIC_HOST` wird beim Bauen fest ins Dashboard einkompiliert (Next.js-Vorgabe). Wenn sich die Adresse später ändert, muss das Dashboard neu gebaut werden (Schritt 4 erneut ausführen).

### 4. Bauen und starten

```bash
docker compose up -d --build
```

Das baut beide Images (Backend-JAR via Gradle, Dashboard via `next build`) und startet sie. Persistente Daten (Tasks, Templates, Server-/Proxy-Instanzen, das Admin-Passwort) liegen in einem benannten Docker-Volume (`lupo-data`) und überleben Neustarts/Updates.

Status prüfen:

```bash
docker compose ps
```

### 5. Login-Infos holen

Beim allerersten Start zeigt die Konsole automatisch Dashboard-URL und Admin-Passwort an:

```bash
docker compose logs backend | grep -A4 "LUPO CLOUD READY"
```

### 6. Dashboard öffnen

```
http://<PUBLIC_HOST>:<DASHBOARD_PORT>/login
```

Mit dem Passwort aus Schritt 5 einloggen. Da die Cloud noch leer ist, leitet das Dashboard automatisch zur Ersteinrichtung (`/setup`) weiter — dort Proxy und Lobby anlegen (oder überspringen und später manuell über Tasks anlegen).

### Updates

```bash
git pull
docker compose up -d --build
```
