#!/usr/bin/env bash
# Tunnel Cloudflare de test — mode quick tunnel (trycloudflare.com)
# Supervision permanente : relance automatiquement le tunnel si cloudflared
# s'arrête, afin de garder l'URL publique disponible le plus longtemps possible.
set -uo pipefail
CLOUDFLARED="${CLOUDFLARED:-$HOME/bin/cloudflared}"
[ -x "$CLOUDFLARED" ] || CLOUDFLARED=/usr/local/bin/cloudflared
[ -x "$CLOUDFLARED" ] || CLOUDFLARED=/tmp/cloudflared
LOG="${LOG:-/tmp/cloudflared-quick.log}"
: > "$LOG"        # (re)démarre le log à blanc
echo "[supervisor] lancement de $CLOUDFLARED → http://localhost:5173" | tee -a "$LOG"
while true; do
        # --protocol http2 : contourne le blocage UDP/QUIC sur ce réseau (forcé TCP)
  "$CLOUDFLARED" --no-autoupdate --protocol http2 tunnel --url http://localhost:5173 2>&1 | tee -a "$LOG"
  echo "[supervisor] cloudflared s'est arrêté — relance dans 4s" | tee -a "$LOG"
  sleep 4
done
