#!/bin/bash
# Discipolat - Export PostgreSQL Render (pg_dump) — filet de sécurité plan Free
#
# ⚠️ Contexte : les bases PostgreSQL du plan Free Render expirent 30 jours après
# leur création et n'ont AUCUN backup automatique (ni manuel côté Render). Le seul
# moyen de sauvegarder est un export pg_dump EXTERNE depuis votre machine
# (procédure complète : DEPLOYMENT.md §8.6 Option B).
#
# Usage :
#   ./scripts/backup-render.sh "<External Database URL>"
#   RENDER_DB_URL="postgresql://..." ./scripts/backup-render.sh
#
# L'URL est copiée depuis :
#   Dashboard Render → Databases → discipolat-db → Connect → External Database URL
#   ⚠️ PAS l'Internal URL (accessible uniquement depuis le réseau privé Render).
#
# Prérequis : pg_dump (PostgreSQL 16) installé localement.
# Vérifier :  pg_dump --version

set -euo pipefail

DB_URL="${1:-${RENDER_DB_URL:-}}"

if [ -z "${DB_URL}" ]; then
    echo "❌ Usage: $0 \"<External Database URL>\""
    echo "   ou définir la variable RENDER_DB_URL"
    echo "   (Dashboard Render → discipolat-db → Connect → External Database URL)"
    exit 1
fi

BACKUP_DIR="${BACKUP_DIR:-./backups}"
mkdir -p "${BACKUP_DIR}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/discipolat_render_${TIMESTAMP}.sql"

echo "🔐 Export pg_dump de la base Render..."
echo "  Fichier : ${BACKUP_FILE}"
echo ""

# --no-owner / --no-privileges : dump portable vers toute nouvelle base
# (les rôles Render n'existent pas hors de leur infrastructure).
pg_dump "${DB_URL}" \
    --no-owner \
    --no-privileges \
    --file="${BACKUP_FILE}"

echo ""
echo "✅ Export terminé : ${BACKUP_FILE} ($(du -h "${BACKUP_FILE}" | cut -f1))"

# Vérification : le fichier doit commencer par l'en-tête pg_dump
if head -1 "${BACKUP_FILE}" | grep -q "PostgreSQL database dump"; then
    echo "✅ Contenu vérifié (dump PostgreSQL valide)"
else
    echo "❌ Le fichier ne ressemble pas à un dump PostgreSQL — vérifier l'URL"
    exit 1
fi

echo ""
echo "📁 Stocker ce fichier HORS de Render (GitHub privé, NAS, machine...)."
echo "⏰ Ne pas attendre le 30ᵉ jour : prévoir l'upgrade Starter (~7 \$/mois)"
echo "   ou la restauration dans une nouvelle base (procédure §8.6)."
