#!/usr/bin/env bash
# V0.16 (TODO_BACKEND_V0_CLEAN_ARCH.md) — palier de pilotage des ruptures de couples, INFORMATIF.
#
# POURQUOI CE SCRIPT : V0.15 doit rompre 45 couples en couplage réciproque (règle R6 d'ArchUnit),
# un couple par PR. L'ordre de ces ruptures est une décision — et tant qu'elle n'est pas chiffrée,
# elle se prend à la dernière minute, typiquement « le fichier que j'étais en train de toucher ».
# Ce script publie le classement demandé par V0.16 pour que l'ordre des ruptures soit documenté
# AVANT d'être exécuté.
#
# DÉFINITION PUBLIÉE AVEC LE CHIFFRE (un classement sans définition est du bruit) :
#   centralité(contexte)  = arêtes R3 incidentes = sortantes + entrantes
#   score(couple A <-> B) = centralité(A) + centralité(B)      (arête comptée par SENS)
#   R3<->(couple)         = nombre de SENS du couple qui sont des accès aux INTERNES (0, 1 ou 2)
#   embrouillement(ctx)   = nombre de couples R6 où le contexte apparaît
#   masse(contexte)       = lignes de source Java du contexte (coût de DÉPLACEMENT, pas de rupture)
# Attention à la source des deux chiffres : R3 ne compte que les accès aux INTERNEs (domain,
# repository) d'un contexte voisin, alors que R6 est calculé par ArchUnit sur TOUTE dépendance
# entre contexts (voir `aretesParContexte()` dans `ArchitectureRulesTest`). La centralité ici est
# donc un PROXY des blocages qui comptent pour l'extraction (ce que dit ADR-002), pas une mesure
# du graphe complet — c'est écrit dans le rapport, pas caché. Le gel ne contient pas le graphe
# complet : le recalculer ici dupliquerait le travail d'ArchUnit avec un risque de divergence.
#
# CE QUE LE CLASSEMENT NE DIT PAS (écrit dans le rapport, pas caché) : la centralité mesure
# l'importance du blocage, PAS le coût de la rupture — celui-ci dépend du nombre de classes et de
# méthodes touchées sur l'arête, ce qu'une PR de rupture seule révèle. Le palier ordonne, il ne
# dispense pas de juger.
#
# Source de vérité : `architecture-freeze.txt`, le gel écrit par `ArchitectureRulesTest`. Le palier
# lit le MÊME fichier que la gate, donc il ne peut pas raconter une histoire différente d'elle.
#
# Modes :
#   (défaut) rapport lisible ; sort 0 tant que le gel existe — informatif, non bloquant
#   --check  gate de cohérence : sort 1 si le gel est impossible (0 arête ou 0 couple, ligne R3/R6
#            mal formée, arête dupliquée, couple non canonique a<b, contexte sans dossier source)
#   --rang "a <-> b"  rang d'un couple ; sort 1 si ce couple n'est pas dans le gel
#
# Usage : scripts/architecture-couples.sh [--top N] [--freeze FICHIER] [--sources REP]
#                                         [--out FICHIER] [--check] [--rang "a <-> b"]
set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RACINE"

TOP=15
GELE="backend/src/test/resources/architecture/architecture-freeze.txt"
SOURCES="backend/src/main/java/com/discipolat/modules"
OUT=""
CHECK=0
RANG=""

while [ $# -gt 0 ]; do
  case "$1" in
    --top) TOP="$2"; shift 2 ;;
    --freeze) GELE="$2"; shift 2 ;;
    --sources) SOURCES="$2"; shift 2 ;;
    --out) OUT="$2"; shift 2 ;;
    --check) CHECK=1; shift ;;
    --rang) RANG="$2"; shift 2 ;;
    -h|--help) sed -n '2,35p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "option inconnue : $1" >&2; exit 2 ;;
  esac
done

[ -f "$GELE" ] || { echo "gel d'architecture introuvable : $GELE — le palier ne devine pas la dette." >&2; exit 2; }
[ -d "$SOURCES" ] || { echo "racine des contexts introuvable : $SOURCES — les masses seraient toutes nulles." >&2; exit 2; }

t_couples="$(mktemp)"; t_contextes="$(mktemp)"; t_problemes="$(mktemp)"; t_dossiers="$(mktemp)"; t_masses="$(mktemp)"
trap 'rm -f "$t_couples" "$t_contextes" "$t_problemes" "$t_dossiers" "$t_masses"' EXIT

find "$SOURCES" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort > "$t_dossiers"

# Masse par contexte : 135 dossiers, UN seul `wc` en lot (pas 135 forks). /dev/null est l'opérande
# factice qui garantit un "<lignes> <chemin>" même quand le lot ne contient qu'un fichier — sans
# lui, le premier contexte du lot imprime un nombre nu et sa masse est perdue.
find "$SOURCES" -type f -name '*.java' -print0 2>/dev/null \
  | xargs -0 -r wc -l /dev/null 2>/dev/null \
  | awk -v racine="$SOURCES/" '
      $2 != "total" && $2 != "/dev/null" {
        chemin = $2; sub(racine, "", chemin); split(chemin, morceaux, "/")
        if (morceaux[1] != "") { compte[morceaux[1]] += $1 }
      }
      END { for (c in compte) printf "%s\t%d\n", c, compte[c] }' \
  | sort > "$t_masses"

# Une passe awk sur le gel, puis `sort` pour le tri (mawk n'a pas asort(), et un tri fait en aval
# est revérifiable à la main). Sorties :
#   t_couples   : score<TAB>couple<TAB>centrA<TAB>centrB<TAB>masseA<TAB>masseB
#   t_contextes : embrouillement<TAB>centralite<TAB>contexte<TAB>masse
#   t_problemes : incohérences du gel (consommé par --check)
awk -v dossiers_f="$t_dossiers" -v masses_f="$t_masses" \
    -v couples_f="$t_couples" -v contextes_f="$t_contextes" -v problemes_f="$t_problemes" '
  BEGIN {
    while ((getline ligne < dossiers_f) > 0) { existe[ligne] = 1 }
    close(dossiers_f)
    while ((getline ligne < masses_f) > 0) { split(ligne, m, "\t"); masse[m[1]] = m[2] }
    close(masses_f)
  }
  /^objet\|R3\|/ {
    t = substr($0, index($0, "|R3|") + 4)
    n = split(t, morceaux, " -> ")
    if (n != 2 || morceaux[1] == "" || morceaux[2] == "") {
      print "ligne R3 mal formée : " $0 >> problemes_f; nb_malformees++; next
    }
    a = morceaux[1]; b = morceaux[2]; cle = a "\t" b
    if (cle in arete) { printf "arête R3 dupliquée dans le gel : %s -> %s\n", a, b >> problemes_f }
    else { nb_arretes++ }
    arete[cle]++
    degout[a]++; degin[b]++
    if (!(a in existe)) print "contexte R3 sans dossier de source : " a >> problemes_f
    if (!(b in existe)) print "contexte R3 sans dossier de source : " b >> problemes_f
    next
  }
  /^objet\|R6\|/ {
    t = substr($0, index($0, "|R6|") + 4)
    n = split(t, morceaux, " <-> ")
    if (n != 2 || morceaux[1] == "" || morceaux[2] == "") {
      print "ligne R6 mal formée : " $0 >> problemes_f; nb_malformees++; next
    }
    a = morceaux[1]; b = morceaux[2]; nb_couples++
    embrouille[a]++; embrouille[b]++
    # R6 émet le couple en ordre canonique (`source.compareTo(cible) < 0`) : un ordre non canonique
    # signe un gel édité à la main, pas un gel écrit par la règle.
    if (!(a < b)) { printf "couple non canonique (attendu a<b) : %s <-> %s\n", a, b >> problemes_f }
    # les deux SENS ne sont pas forcément des accès aux internes : R6 voit toute dépendance
    sens = ((a "\t" b) in arete) + ((b "\t" a) in arete)
    printf "%d\t%s <-> %s\t%d\t%d\t%d\t%d\t%d\n", degout[a] + degin[a] + degout[b] + degin[b], \
      a, b, degout[a] + degin[a], degout[b] + degin[b], masse[a] + 0, masse[b] + 0, sens >> couples_f
    next
  }
  END {
    for (c in embrouille) {
      printf "%d\t%d\t%s\t%d\n", embrouille[c], degout[c] + degin[c], c, masse[c] + 0 >> contextes_f
    }
  }
' "$GELE"

# Compteurs relus directement dans le gel : le rapport doit annoncer les mêmes chiffres que la gate.
meta="$(awk '/^objet\|R3\|/ { a++ } /^objet\|R6\|/ { c++ } END { printf "%d %d", a+0, c+0 }' "$GELE")"
NB_ARRETES="${meta%% *}"; NB_COUPLES="${meta##* }"

# Tri : score décroissant, puis couple croissant (déterministe — un rapport dont l'ordre change
# entre deux exécutions ne pilote rien).
sort -k1,1nr -k2,2 "$t_couples" -o "$t_couples.tri"
sort -k1,1nr -k3,3 "$t_contextes" -o "$t_contextes.tri"
trap 'rm -f "$t_couples" "$t_contextes" "$t_problemes" "$t_dossiers" "$t_masses" "$t_couples.tri" "$t_contextes.tri"' EXIT

if [ "$CHECK" -eq 1 ]; then
  echec=0
  [ "$NB_COUPLES" -gt 0 ] || { echo "--check : aucun couple R6 dans $GELE" >&2; echec=1; }
  [ "$NB_ARRETES" -gt 0 ] || { echo "--check : aucune arête R3 dans $GELE" >&2; echec=1; }
  [ "$(wc -l < "$t_couples")" -eq "$NB_COUPLES" ] || {
    echo "--check : $(wc -l < "$t_couples") couple(s) exploitable(s) pour $NB_COUPLES ligne(s) R6" >&2; echec=1; }
  if [ -s "$t_problemes" ]; then
    sed 's/^/--check : /' "$t_problemes" >&2
    echec=1
  fi
  if [ "$echec" -eq 0 ]; then
    echo "--check : gel cohérent ($NB_ARRETES arêtes R3, $NB_COUPLES couples R6, 0 contradiction)"
  fi
  exit "$echec"
fi

if [ -n "$RANG" ]; then
  rang="$(awk -v cherche="$RANG" -F'\t' '$2 == cherche { print NR; exit }' "$t_couples.tri")"
  if [ -z "$rang" ]; then
    echo "--rang : couple inconnu dans le gel R6 : « $RANG » (attendu « a <-> b », ordre tel que gelé)" >&2
    exit 1
  fi
  echo "$rang"
  exit 0
fi

rapport="$(mktemp)"
trap 'rm -f "$t_couples" "$t_contextes" "$t_problemes" "$t_dossiers" "$t_masses" "$t_couples.tri" "$t_contextes.tri" "$rapport"' EXIT

{
  echo "Palier de pilotage V0.16 — couples en couplage réciproque (R6) classés par centralité"
  echo "généré : $(date -u '+%Y-%m-%dT%H:%M:%SZ') · dépôt : $(git rev-parse --short HEAD 2>/dev/null || echo inconnu)"
  echo "gel    : $GELE · arêtes R3 : $NB_ARRETES · couples R6 : $NB_COUPLES · contexts : $(wc -l < "$t_dossiers")"
  echo "déf    : centralité = arêtes R3 sortantes + entrantes (proxy, R3 ne voit que les internes)"
  echo "       : score(couple) = somme des deux centralités ; R3<-> = sens du couple qui touchent l'interne"
  echo ""

  echo "== A. couples par centralité décroissante (top $TOP sur $NB_COUPLES)"
  printf '   %-4s %-34s %8s %8s %7s %5s %9s %9s\n' "rang" "couple" "centr.A" "centr.B" "score" "R3<->" "masse.A" "masse.B"
  awk -F'\t' -v top="$TOP" 'NR <= top { printf "   %-4d %-34s %8d %8d %7d %5d %9d %9d\n", NR, $2, $3, $4, $1, $7, $5, $6 }' "$t_couples.tri"
  if [ "$NB_COUPLES" -gt "$TOP" ]; then
    echo "   … $((NB_COUPLES - TOP)) autre(s) couple(s) hors du top $TOP (--top N pour élargir)"
  fi
  echo ""

  echo "== B. contexts les plus embrouillés (nb de couples R6, puis centralité)"
  printf '   %-24s %12s %10s %9s\n' "contexte" "couples R6" "centralité" "masse(l.)"
  awk -F'\t' -v top="$TOP" 'NR <= top { printf "   %-24s %12d %10d %9d\n", $3, $1, $2, $4 }' "$t_contextes.tri"
  echo ""

  echo "== C. ce que ce classement NE dit PAS"
  echo "   - Le coût d'une rupture : la centralité compte des ARÊTES, pas les classes et méthodes"
  echo "     à découper. Deux couples de même score peuvent coûter 1 fichier ou 40."
  echo "   - Le sens de la fuite : rompre « A -> B » peut coûter autant que rompre « B -> A » ;"
  echo "     la colonne R3<-> dit seulement si les deux sens pénètrent l'interne (2 = les deux)."
  echo "     le gel ne stocke pas le nombre de points d'appel par arête (V0.15 le mesurera couple par couple)."
  echo "   - La légitimité du couple : un score bas ne veut pas dire « sans valeur », mais « débloquant"
  echo "     peu » — la valeur métier reste une décision humaine (ADR-002, TODO V0.15)."
  echo "   - Ce rapport est INFORMATIF : la gate qui compte reste ArchitectureRulesTest (le gel est un"
  echo "     plafond, toute arête ou tout couple nouveau rougit). --check vérifie la cohérence du gel,"
  echo "     pas la dette."
} > "$rapport"

cat "$rapport"
if [ -n "$OUT" ]; then
  mkdir -p "$(dirname "$OUT")"
  cp "$rapport" "$OUT"
  # `cp` recopie le mode du fichier source, et `mktemp` crée en 0600 : sans ce `chmod`, le premier
  # rapport écrit dans un répertoire neuf y reste illisible pour tout autre utilisateur (et pour un
  # upload d'artefact qui tourne sous un autre uid que celui du script).
  chmod 0644 "$OUT"
  echo "(rapport écrit : $OUT)" >&2
fi
exit 0
