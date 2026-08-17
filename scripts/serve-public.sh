#!/bin/bash
# ============================================================
# serve-public.sh — expose l'application locale à Internet
# via un tunnel Cloudflare (trycloudflare.com), sans compte.
#
# Démarre : API Spring (:8080) + frontend Vite (:5173) + tunnel.
# L'URL publique est affichée en fin de script — gardez le script
# en cours d'exécution : tant qu'il tourne, l'URL est active.
#
# Usage :  bash scripts/serve-public.sh
# Arrêt :  Ctrl+C (arrête la stack locale + le tunnel)
# ============================================================
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLOUDFLARED="${CLOUDFLARED:-/tmp/cloudflared}"
CLOUDFLARED_URL="${CLOUDFLARED_URL:-https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64}"
API_URL="${API_URL:-http://localhost:8080}"
WEB_URL="${WEB_URL:-http://localhost:5173}"

# 0. Binaire cloudflared (téléchargé dans /tmp si absent)
if [ ! -x "$CLOUDFLARED" ]; then
  echo "→ Téléchargement de cloudflared…"
  curl -sL --max-time 120 "$CLOUDFLARED_URL" -o "$CLOUDFLARED"
  chmod +x "$CLOUDFLARED"
fi

cleanup() {
  echo ""
  echo "→ Arrêt de la stack locale et du tunnel…"
  pkill -f 'spring-boot:run' 2>/dev/null || true
  pkill -f 'vite' 2>/dev/null || true
  pkill -f 'cloudflared tunnel' 2>/dev/null || true
}
trap cleanup EXIT INT TERM

# 1. Base de données Docker (si absente)
if ! docker ps --format '{{.Names}}' | grep -q '^discipolat-db$'; then
  echo "→ Démarrage de la base Docker (discipolat-db :5433)…"
  docker compose -f "$ROOT/docker-compose.yml" up -d discipolat-db redis 2>/dev/null || true
  sleep 5
fi

# 2. Backend Spring Boot (:8080)
echo "→ Démarrage de l'API Spring Boot (:8080)…"
(
  cd "$ROOT/backend"
  export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5433/discipolat}"
  export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-discipolat}"
  export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-discipolat_secret}"
  export SPRING_REDIS_HOST=localhost SPRING_REDIS_PORT=6379
  nohup mvn -q spring-boot:run > /tmp/backend-run.log 2>&1 &
)
echo -n "   attente API"
until curl -s -o /dev/null "$API_URL/api/v1/public/meta" 2>/dev/null; do
  echo -n "."; sleep 2
  if ! pgrep -f 'spring-boot:run' > /dev/null; then
    echo " [ERREUR] L'API s'est arrêtée — voir /tmp/backend-run.log"; exit 1
  fi
done
echo " OK"

# 3. Frontend Vite (:5173)
echo "→ Démarrage du frontend Vite (:5173)…"
(cd "$ROOT/frontend" && nohup npm run dev > /tmp/web-run.log 2>&1 &)
echo -n "   attente WEB"
until curl -s -o /dev/null "$WEB_URL/login" 2>/dev/null; do
  echo -n "."; sleep 2
done
echo " OK"

# 4. Tunnel Cloudflare (quick tunnel — sans compte)
echo "→ Démarrage du tunnel Cloudflare…"
nohup "$CLOUDFLARED" tunnel --url "$WEB_URL" --no-autoupdate > /tmp/cloudflared.log 2>&1 &
echo -n "   attente URL publique"
PUBLIC_URL=""
for _ in $(seq 1 60); do
  PUBLIC_URL=$(grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' /tmp/cloudflared.log | head -1 || true)
  if [ -n "$PUBLIC_URL" ]; then break; fi
  echo -n "."; sleep 2
done

echo ""
if [ -z "$PUBLIC_URL" ]; then
  echo "❌ Tunnel non démarré — log : /tmp/cloudflared.log"
  exit 1
fi

# 5. Vérification de bout en bout
echo "→ Vérification (login + meta via le tunnel)…"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" --max-time 30 "$PUBLIC_URL/api/v1/public/meta" 2>/dev/null || echo "000")
echo "   GET $PUBLIC_URL/api/v1/public/meta → HTTP $HTTP"

cat << "EOF"

============================================================
✅  APPLICATION PUBLIQUE EN LIGNE
============================================================
URL à partager : 
EOF
echo "   $PUBLIC_URL"
cat << "EOF"

🔑  COMPTES DE DÉMONSTRATION (mot de passe : password123)
-----------------------------------------------------------
Admin complet      : admin@discipolat.com
Pasteur            : pasteur@discipolat.com
Responsable        : responsable@discipolat.com   (département Audiovisuel)
Chef de famille    : chef@discipolat.com
Faiseur de disc.   : faiseur@discipolat.com
Membre             : membre@discipolat.com
Multi-rôles        : paul@discipolat.com          (responsable + chef)

Base de données volumineuse (seed fictif) : 16 départements, 44 familles,
1015 âmes, 73 utilisateurs — de quoi explorer tous les modules.

⚠️  Ce tunnel est une URL temporaire (trycloudflare). Tant que ce script
    tourne, l'URL est active. Pour un lien permanent avec votre domaine :
    cloudflared tunnel login   (compte Cloudflare)
    cloudflared tunnel create discipolat
    puis renseigner la config (voir docs/DEPLOYMENT.md §Tunnel).

Pour redonner l'accès plus tard :  bash scripts/serve-public.sh
============================================================
EOF

# Garde le script vivant (la stack + le tunnel tournent sous ce process)
echo "(Ctrl+C pour tout arrêter)"
while true; do sleep 60; done
