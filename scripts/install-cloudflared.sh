#!/usr/bin/env bash
# ============================================================
# install-cloudflared.sh
# Installe le binaire cloudflared localement (sans sudo).
# Usage : bash scripts/install-cloudflared.sh
# ============================================================
set -euo pipefail

ARCH=$(uname -m)
case "$ARCH" in
  x86_64|amd64) BIN="cloudflared-linux-amd64" ;;
  aarch64|arm64) BIN="cloudflared-linux-arm64" ;;
  *) echo "❌ Arch non supportée : $ARCH"; exit 1 ;;
esac
URL="https://github.com/cloudflare/cloudflared/releases/latest/download/$BIN"

# Destination : /usr/local/bin si accessible, sinon ~/bin
if [ -w /usr/local/bin ] && command -v install >/dev/null 2>&1; then
  DEST=/usr/local/bin/cloudflared
else
  mkdir -p "$HOME/bin"; DEST="$HOME/bin/cloudflared"
fi

echo "→ Téléchargement de cloudflared ($BIN)…"
curl -fL --max-time 300 -o "$DEST.tmp" "$URL"
chmod +x "$DEST.tmp"
mv "$DEST.tmp" "$DEST"
echo "✅ cloudflared installé : $DEST"
"$DEST" version
# Rappel PATH si ~/bin
if [[ "$DEST" == "$HOME/bin/cloudflared" ]]; then
  echo "💡 Ajoutez ~/bin à votre PATH (echo 'export PATH=\"\$HOME/bin:\$PATH\"' >> ~/.bashrc)"
fi
