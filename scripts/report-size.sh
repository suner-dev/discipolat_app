#!/usr/bin/env bash
# V0.2 (TODO_BACKEND_V0_CLEAN_ARCH.md) — rapport de taille, INFORMATIF.
#
# Publie la liste des fichiers de source volumineux (défaut : > 500 lignes) et le compte
# agrégé par pile. C'est la donnée d'entrée des décisions de découpage : un méga-service de
# 1 545 lignes n'est pas un problème de style, c'est un use-case qui n'a jamais été nommé.
#
# Volontairement non bloquant : le gate de V0.2 est « le job publie un rapport », pas
# « la dette est nulle ». Passer en bloquant se fait par --strict, le jour où l'équipe
# décide qu'un fichier neuf de 500 lignes doit être justifié en revue de code.
#
# Usage : scripts/report-size.sh [--seuil N] [--top N] [--scope be|fe|mobile|all]
#                                [--out FICHIER] [--strict] [--include-tests]
set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RACINE"   # les chemins du rapport sont relatifs à la racine du dépôt, pas absolus
SEUIL=500
TOP=20
SCOPE="all"
OUT=""
STRICT=0
INCLUDE_TESTS=0

while [ $# -gt 0 ]; do
  case "$1" in
    --seuil) SEUIL="$2"; shift 2 ;;
    --top) TOP="$2"; shift 2 ;;
    --scope) SCOPE="$2"; shift 2 ;;
    --out) OUT="$2"; shift 2 ;;
    --include-tests) INCLUDE_TESTS=1; shift ;;
    --strict) STRICT=1; shift ;;
    -h|--help) sed -n '2,15p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "option inconnue : $1" >&2; exit 2 ;;
  esac
done

# une ligne = "<dossier> <glob...>" ; le périmètre par défaut est la SOURCE DE PRODUCTION :
# un test de 1 100 lignes n'est pas une dette d'architecture, et il masquerait les
# méga-services que V0.2 cherche justement à lister (--include-tests pour tout voir).
PILES=()
case "$SCOPE" in
  be) PILES+=("backend/src/main/java *.java") ;;
  fe) PILES+=("frontend/src *.ts *.tsx") ;;
  mobile) PILES+=("mobile/lib *.dart") ;;
  all) PILES+=("backend/src/main/java *.java" "frontend/src *.ts *.tsx" "mobile/lib *.dart") ;;
  *) echo "--scope attendu be|fe|mobile|all, reçu : $SCOPE" >&2; exit 2 ;;
esac

if [ "$INCLUDE_TESTS" -eq 1 ]; then
  case "$SCOPE" in
    be) PILES=("backend *.java") ;;
    fe) PILES=("frontend/src *.ts *.tsx") ;;
    mobile) PILES=("mobile *.dart") ;;
    all) PILES=("backend *.java" "frontend/src *.ts *.tsx" "mobile *.dart") ;;
  esac
fi

tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

total_tous=0
printf '%s\n' "Rapport de taille — seuil $SEUIL lignes, top $TOP par pile" \
  "généré : $(date -u '+%Y-%m-%dT%H:%M:%SZ') · dépôt : $(cd "$RACINE" && git rev-parse --short HEAD 2>/dev/null || echo inconnu)" \
  "" >> "$tmp"

for pile in "${PILES[@]}"; do
  set -- $pile
  dossier="$1"; shift; globs=("$@")
  [ -d "$RACINE/$dossier" ] || { printf '%s\n' "== $dossier : absent, ignoré" "" >> "$tmp"; continue; }

  find_args=(-type f)
  for g in "${globs[@]}"; do find_args+=(-name "$g"); done
  find_args+=(-not -path '*/node_modules/*' -not -path '*/build/*' -not -path '*/.dart_tool/*'
    -not -path '*/target/*' -not -path '*/dist/*'
    -not -path '*__tests__*' -not -name '*.test.ts' -not -name '*.test.tsx'
    -not -name '*.g.dart' -not -name '*.freezed.dart' -not -name '*.gr.dart')

  # `wc -l` en lot, puis on garde ce qui dépasse le seuil. Aucun fichier n'est modifié.
  corps="$(find "$RACINE/$dossier" "${find_args[@]}" -print0 \
    | xargs -0 -r wc -l 2>/dev/null \
    | awk -v s="$SEUIL" '$1 > s && $2 != "total" {print $1 "\t" $2}' \
    | sort -rn || true)"

  if [ -z "$corps" ]; then
    printf '%s\n' "== $dossier : 0 fichier > $SEUIL lignes" "" >> "$tmp"
    continue
  fi

  compte="$(printf '%s\n' "$corps" | wc -l)"
  somme="$(printf '%s\n' "$corps" | awk '{t += $1} END {print t+0}')"
  total_tous=$((total_tous + compte))
  printf '%s\n' "== $dossier : $compte fichier(s) > $SEUIL lignes, $somme lignes concernées" >> "$tmp"
  printf '%s\n' "$corps" | head -n "$TOP" | awk -F'\t' '{printf "  %6d l.  %s\n", $1, $2}' >> "$tmp"
  if [ "$compte" -gt "$TOP" ]; then
    printf '%s\n' "  … $(($compte - TOP)) autre(s) hors du top $TOP" >> "$tmp"
  fi
  printf '%s\n' "" >> "$tmp"
done

printf '%s\n' "total inter-piles : $total_tous fichier(s) > $SEUIL lignes" >> "$tmp"
printf '%s\n---\n' "$(cat "$tmp")"

if [ -n "$OUT" ]; then
  mkdir -p "$(dirname "$OUT")"
  cp "$tmp" "$OUT"
  echo "(rapport écrit : $OUT)" >&2
fi

if [ "$STRICT" -eq 1 ] && [ "$total_tous" -gt 0 ]; then
  echo "--strict : $total_tous fichier(s) dépassent le seuil" >&2
  exit 1
fi
exit 0
