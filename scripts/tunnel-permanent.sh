#!/usr/bin/env bash
# ============================================================
# tunnel-permanent.sh — Tunnel Cloudflare PERMANENT pour tester
# l'application Discipolat en ligne.
#
# Ce script :
#   1. Installe cloudflared si absent
#   2. Démarre la stack locale (DB Redis + API :8080 + Vite :5173)
#   3. Authentifie cloudflared sur votre compte Cloudflare (1ère fois)
#   4. Crée / rattache un tunnel NOMMÉ à votre domaine
#   5. Le garde en vie PERMANENTEMENT (boucle de supervision + reconnect)
#
# Variables d'environnement :
#   CF_TUNNEL_NAME     nom du tunnel            (défaut: discipolat)
#   CF_DOMAIN          domaine cloudflare       ex: app.discipolat.fr
#   CF_NO_AUTOUPDATE   garder --no-autoupdate   (défaut: true)
#
# Usage :
#   ./scripts/tunnel-permanent.sh            → lance et supervise (foreground)
#   ./scripts/tunnel-permanent.sh --daemon   → détache en arrière-plan (nohup)
# ============================================================
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLOUDFLARED="${CLOUDFLARED:-${HOME}/bin/cloudflared}"
if ! command -v cloudflared >/dev/null 2>&1 && [ ! -x "$CLOUDFLARED" ]; then CLOUDFLARED=/usr/local/bin/cloudflared; fi
if ! command -v cloudflared >/dev/null 2>&1 && [ ! -x "$CLOUDFLARED" ]; then CLOUDFLARED=/tmp/cloudflared; fi

CF_DIR="$HOME/.cloudflared"
CF_TUNNEL_NAME="${CF_TUNNEL_NAME:-discipolat}"
CF_DOMAIN="${CF_DOMAIN:-}"          # ex: app.discipolat.fr
CF_NO_AUTOUPDATE="${CF_NO_AUTOUPDATE:-true}"
DAEMON_MODE=false
[ "${1:-}" = "--daemon" ] && DAEMON_MODE=true

API_URL="http://localhost:8080"
WEB_URL="http://localhost:5173"

GREEN='\033[0;32m'; BLUE='\033[0;34m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${BLUE}[i]${NC} $*"; }
log()   { echo -e "${GREEN}[OK]${NC} $*"; }
warn()  { echo -e "${YELLOW}[!]${NC} $*"; }
error() { echo -e "${RED}[X]${NC} $*" >&2; }

# ----------------------------------------------------------------
# 1. cloudflared installé ?
ensure_cloudflared() {
  if [ -x "$CLOUDFLARED" ] && "$CLOUDFLARED" version >/dev/null 2>&1; then
    log "cloudflared présent : $CLOUDFLARED ($("$CLOUDFLARED" version | head -1))"
    return 0
  fi
  info "cloudflared absent → lancement de l'installeur…"
  if [ -x "$ROOT/scripts/install-cloudflared.sh" ]; then
    bash "$ROOT/scripts/install-cloudflared.sh" || true
  else
    info "Téléchargement direct…"
    mkdir -p "$HOME/bin"
    BIN="cloudflared-linux-amd64"; [ "$(uname -m)" = "aarch64" ] && BIN="cloudflared-linux-arm64"
    curl -fL --max-time 300 -o "$HOME/bin/cloudflared" \
      "https://github.com/cloudflare/cloudflared/releases/latest/download/$BIN"
    chmod +x "$HOME/bin/cloudflared"; CLOUDFLARED="$HOME/bin/cloudflared"
  fi
  "$CLOUDFLARED" version || { error "cloudflared unreachable"; exit 1; }
}
# ----------------------------------------------------------------
# 2. Stack locale (DB/Redis + API + Vite)
# ----------------------------------------------------------------
ensure_local_stack() {
  # 2a. PostgreSQL + Redis via docker-compose (si absents)
  if ! nc -z localhost 5433 2>/dev/null; then
    info "Démarrage de PostgreSQL (:5433) et Redis (:6379) via Docker…"
    docker compose -f "$ROOT/docker-compose.yml" up -d discipolat-db redis 2>/dev/null || true
    for _ in $(seq 1 20); do nc -z localhost 5433 2>/dev/null && break; sleep 1; done
  else
    log "PostgreSQL (:5433) disponible"
  fi
  nc -z localhost 6379 2>/dev/null && log "Redis (:6379) disponible" \
    || warn "Redis (:6379) absent (le backend Spring s'en sert)"

  # 2b. Backend Spring Boot :8080
  if ! curl -s -o /dev/null "$API_URL/api/v1/public/meta" 2>/dev/null; then
    info "Démarrage de l'API Spring Boot (:8080)…"
    (
      cd "$ROOT/backend"
      SPRING_PROFILES_ACTIVE=default \
      JWT_PRIVATE_KEY_PATH="$ROOT/keys/private.pem" \
      JWT_PUBLIC_KEY_PATH="$ROOT/keys/public.pem" \
      SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5433/discipolat}" \
      SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-discipolat}" \
      SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-discipolat_secret}" \
      REDIS_URL="${REDIS_URL:-redis://localhost:6379}" \
        nohup mvn -q spring-boot:run > "$ROOT/backend.log" 2>&1 &
      echo $! > /tmp/discipolat-backend.pid
    )
    cd "$ROOT"
    info "  attente API…"
    for _ in $(seq 1 40); do
      curl -s -o /dev/null "$API_URL/api/v1/public/meta" 2>/dev/null && { log "API prête (:8080)"; break; }
      sleep 2
    done
  else
    log "API déjà en cours (:8080)"
  fi

  # 2c. Frontend Vite :5173 (proxy /api → localhost:8080)
  if ! curl -s -o /dev/null "$WEB_URL/login" 2>/dev/null; then
    info "Démarrage du frontend Vite (:5173)…"
    (cd "$ROOT/frontend" && nohup npm run dev > "$ROOT/frontend.log" 2>&1 &)
    for _ in $(seq 1 25); do
      curl -s -o /dev/null "$WEB_URL/login" 2>/dev/null && { log "Frontend prêt (:5173)"; break; }
      sleep 2
    done
  else
    log "Frontend déjà en cours (:5173)"
  fi
}
# ----------------------------------------------------------------
# 3. Auth Cloudflare (1ère fois) + création du tunnel nommé
# ----------------------------------------------------------------
ensure_auth_and_tunnel() {
  mkdir -p "$CF_DIR"

  # 3a. cert.pem = compte Cloudflare lié
  if [ ! -f "$CF_DIR/cert.pem" ]; then
    warn "Pas de certificat Cloudflare (~/.cloudflared/cert.pem)."
    warn "Connexion via le navigateur nécessaire — autorisez cloudflared."
        # NB: --no-autoupdate est un FLAG GLOBAL → il va AVANT 'tunnel login'
    info "Lancement de cloudflared --no-autoupdate tunnel login…"
    set +e
    "$CLOUDFLARED" --no-autoupdate tunnel login > /tmp/cf-login.log 2>&1 &
    LOGIN_PID=$!
    sleep 6
    # cloudflared inscrit l'URL d'auth ; on la capture pour la coller dans le browser.
    AUTH_URL=$(grep -oiE 'https://[a-z0-9./_?=&-]+' /tmp/cf-login.log 2>/dev/null \
               | grep -iE 'dash|oauth|authorize' | head -1 || true)
    if [ -n "$AUTH_URL" ]; then
      echo -e "${YELLOW}🌐 Ouvrez ce lien pour autoriser cloudflared :${NC}"
      echo "   $AUTH_URL"
      echo -e "${YELLOW}   → Connectez-vous, choisissez le domaine, puis cliquez sur « Allow ».${NC}"
      echo "   → Le script détecte le certificat (~/.cloudflared/cert.pem) dès qu’il est créé."
    else
      echo -e "${YELLOW}🔄 cloudflared a peut-être ouvert votre navigateur automatiquement.${NC}"
      echo "   → Si rien ne se passe : ouvrez https://dash.cloudflare.com/argotunnel"
      echo "   → Relancez ensuite : cloudflared --no-autoupdate tunnel login"
    fi
    # On attend que le cert.pem apparaisse (browseur authentifié) — max 600s
    for _ in $(seq 1 120); do
      [ -f "$CF_DIR/cert.pem" ] && break
      kill -0 "$LOGIN_PID" 2>/dev/null || break
      sleep 5
    done
    kill "$LOGIN_PID" 2>/dev/null || true
    wait "$LOGIN_PID" 2>/dev/null || true
    set -e
    sleep 1
  fi

  if [ -f "$CF_DIR/cert.pem" ]; then
    log "Certificat Cloudflare présent (~/.cloudflared/cert.pem)"
  else
    error "Connexion Cloudflare non aboutie (cert.pem absent)."
    error "Relancez 'cloudflared tunnel login' dans un terminal puis relancez ce script."
    exit 1
  fi

  # 3b. Tunnel nommé (création idempotente)
  info "Tunnel Cloudflare : '$CF_TUNNEL_NAME'"
  if ! "$CLOUDFLARED" tunnel list 2>/dev/null | grep -q "$CF_TUNNEL_NAME"; then
    "$CLOUDFLARED" tunnel create "$CF_TUNNEL_NAME" 2>&1 | tail -6 || true
  fi
  # ID du tunnel
  TUNNEL_ID=$("$CLOUDFLARED" tunnel list --output json 2>/dev/null \
              | python3 -c "import sys,json
for t in json.load(sys.stdin):
    if t.get('name')=='$CF_TUNNEL_NAME': print(t['id']); break" 2>/dev/null || true)
  log "Tunnel ID : ${TUNNEL_ID:-?}"

  # 3c. credentials-file + nom du tunnel dans le creds JSON
  CREDS_FILE="$CF_DIR/${TUNNEL_ID}.json"
  [ -z "$TUNNEL_ID" ] && { error "Tunnel introuvable."; exit 1; }
  [ ! -f "$CREDS_FILE" ] && \
    { error "Fichier creds manquant : $CREDS_FILE"; exit 1; } || log "Creds : $CREDS_FILE"

  # 3d. config.yml → route le (sous-)domaine sur le frontend Vite :5173
  cat > "$CF_DIR/config.yml" <<EOF
# Généré par scripts/tunnel-permanent.sh — TUNNEL PERMANENT Discipolat
tunnel: $TUNNEL_ID
credentials-file: $CREDS_FILE
ingress:
  # Frontend Vite (:5173) — proxy /api → API Spring (:8080) via Vite
  - hostname: ${CF_DOMAIN:-localhost}
    service: http://localhost:5173
  - service: http_status:404
EOF
  log "config.yml écrit → $CF_DIR/config.yml"

  # 3e. Route DNS (CNAME) vers le tunnel si un domaine fourni
  if [ -n "$CF_DOMAIN" ]; then
    info "Création/maj de la route DNS : $CF_DOMAIN → tunnel $CF_TUNNEL_NAME"
    "$CLOUDFLARED" tunnel route dns "$CF_TUNNEL_NAME" "$CF_DOMAIN" 2>&1 | tail -2 || true
    info "→ https://$CF_DOMAIN"
  else
    warn "CF_DOMAIN non fourni → pas de route DNS ; le tunnel tourne en mode « fallback »."
  fi
}
# ----------------------------------------------------------------
# 4. Supervision permanente du tunnel (auto-restart)
# ----------------------------------------------------------------
run_tunnel_forever() {
  info "Démarrage du tunnel permanent (connexion + auto-restart)…"
  while true; do
    set +e
    if [ -n "${TUNNEL_ID:-}" ] && [ -f "$CF_DIR/config.yml" ] && [ -n "${CF_DOMAIN:-}" ]; then
      "$CLOUDFLARED" --no-autoupdate --log-level info \
        tunnel --config "$CF_DIR/config.yml" run "$CF_TUNNEL_NAME"
    else
      # Fallback : tunnel rapide trycloudflare (URL temporaire)
      "$CLOUDFLARED" --no-autoupdate tunnel --url "$WEB_URL"
    fi
    rc=$?
    set -e
    warn "cloudflared arrêté (rc=$rc) — redémarrage dans 5s…"
    sleep 5
  done
}

# ----------------------------------------------------------------
# Main
# ----------------------------------------------------------------
echo ""
cat <<'BANNER'
 ╔══════════════════════════════════════════════╗
 ║  Cloudflare Tunnel — Discipolat (PERMANENT)  ║
 ╚══════════════════════════════════════════════╝
BANNER
info "Version interagie : tunnel NOMMÉ + supervision permanente."
[ -n "$CF_DOMAIN" ] && log "Domaine cible : $CF_DOMAIN" \
  || warn "CF_DOMAIN absent → URL trycloudflare rotative."

ensure_cloudflared
ensure_local_stack

# Auth + tunnel nommé (nécessite un compte Cloudflare + domaine)
if [ ! -f "$CF_DIR/config.yml" ] || [ -z "${CF_DOMAIN:-}" ]; then
  ensure_auth_and_tunnel
fi

if $DAEMON_MODE; then
  info "Détachement en arrière-plan (nohup)…"
  nohup bash "$0" > "$ROOT/tunnel-permanent.log" 2>&1 &
  echo $! > /tmp/discipolat-tunnel.pid
  log "Superviseur tunnel PID : $(cat /tmp/discipolat-tunnel.pid)"
  echo "   logs : tail -f $ROOT/tunnel-permanent.log"
  for _ in $(seq 1 90); do
    URL=$(grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' "$ROOT/tunnel-permanent.log" 2>/dev/null | head -1 || true)
    [ -n "$URL" ] && break
    sleep 2
  done
  if [ -n "$URL" ]; then
    log "URL publique (fallback) : $URL"
  else
    warn "URL pas encore dispo — vérifiez $ROOT/tunnel-permanent.log"
  fi
else
  run_tunnel_forever
fi



