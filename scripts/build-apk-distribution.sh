#!/usr/bin/env bash
#
# ═══════════════════════════════════════════════════════════════════════════
# build-apk-distribution.sh — APK de distribution pour testeurs (Diawi)
# ═══════════════════════════════════════════════════════════════════════════
#
# Construit un APK **release** signé debug-key, pointant vers l'environnement
# cible, avec une version incrémentée.Usage :
#
#   ./scripts/build-apk-distribution.sh                    # défaut PROD
#   ./scripts/build-apk-distribution.sh --api-url https://discipolat-api.onrender.com/api/v1
#
# Règles :
#  1. JAMAIS de build debug pour des testeurs (`kDebugMode` pointe l'API locale
#     10.0.2.2 — cf. mobile/lib/data/services/api_config.dart).
#  2. JAMAIS vers la prod sans isolation : par défaut on pointe la BÊTA
#     (discipolat-beta) si elle répond, sinon on demande confirmation.
#  3. La version est auto-incrémentée (+1 build number) à chaque lancement.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MOBILE_DIR="$REPO_ROOT/mobile"
PUBSPEC="$MOBILE_DIR/pubspec.yaml"

DEFAULT_PROD_URL="https://discipolat-api.onrender.com/api/v1"

API_URL="${1:-}"
if [ "${1:-}" = "--api-url" ]; then
  API_URL="${2:?--api-url requiert une URL}"
fi

probe() { # $1 = url -> code HTTP ou 000
  curl -s -o /dev/null -w '%{http_code}' --max-time 60 "$1/public/legal" 2>/dev/null || echo "000"
}

resolve_url() {
  if [ -n "$API_URL" ]; then
    echo "$API_URL"
    return
  fi
  echo "⚠️  Aucune --api-url fournie." >&2
  echo "   Par défaut : PROD ($DEFAULT_PROD_URL)" >&2
  echo "   ⚠️  Les testeurs écriront sur la BASE DE PRODUCTION." >&2
  echo "   Recommandé : un tenant de test dédié, purgé après la campagne." >&2
  printf '   Continuer vers PROD ? [o/N] ' >&2
  read -r answer
  case "$answer" in
    o|O|oui|OUI|y|Y|yes|YES) echo "$DEFAULT_PROD_URL" ;;
    *) echo "Abandon." >&2; exit 1 ;;
  esac
}

require_flutter() {
  command -v flutter >/dev/null 2>&1 || { echo "❌ flutter introuvable" >&2; exit 1; }
}

bump_build_number() { # incrémente le +N de version: x.y.z+N
  local current
  current="$(grep -E '^version:' "$PUBSPEC" | awk '{print $2}')"
  local base="${current%%+*}"
  local build="${current##*+}"
  [[ "$build" =~ ^[0-9]+$ ]] || { echo "❌ version '$current' sans build number +N" >&2; exit 1; }
  local next=$((build + 1))
  sed -i "s/^version: .*/version: ${base}+${next}/" "$PUBSPEC"
  echo "${base}+${next}"
}

main() {
  require_flutter
  local target_url
  target_url="$(resolve_url)"
  echo "→ Test de l'API cible : $target_url"
  local code
  code="$(probe "$target_url")"
  if [ "$code" != "200" ]; then
    echo "❌ L'API cible ne répond pas (HTTP $code sur /public/legal)." >&2
    echo "   (Render gratuit = cold-start ~1 min : réessayez si le service dort.)" >&2
    exit 1
  fi
  echo "✅ API cible joignable (HTTP 200)."

  local version
  version="$(bump_build_number)"
  echo "→ Version : $version"

  cd "$MOBILE_DIR"
  flutter build apk --release --dart-define="API_URL=$target_url"

  local apk
  apk="$(ls -t build/app/outputs/flutter-apk/app-release.apk | head -1)"
  local size
  size="$(du -h "$apk" | cut -f1)"
  local sha
  sha="$(sha256sum "$apk" | cut -d' ' -f1)"

  echo ""
  echo "═══════════════════════════════════════════════════"
  echo "✅ APK prêt : $apk"
  echo "   Version : $version"
  echo "   API     : $target_url"
  echo "   Taille  : $size (limite gratuite Diawi : vérifier)"
  echo "   SHA256  : $sha"
  echo "═══════════════════════════════════════════════════"
  echo ""
  echo "Étapes Diawi :"
  echo "  1. https://www.diawi.com → upload de l'APK"
  echo "  2. Protéger le lien : mot de passe + expiration (obligatoire — cf. DISTRIBUTION_TESTEURS.md)"
  echo "  3. Envoyer le lien + docs/rapports/DISTRIBUTION_TESTEURS.md aux testeurs"
}

main "$@"