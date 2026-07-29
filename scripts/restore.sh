#!/bin/bash
# Discipolat - Database Restore Script
# Usage: ./scripts/restore.sh <backup-file>
# Example: ./scripts/restore.sh ./backups/discipolat_20260729_120000.sql.gz

set -euo pipefail

if [ $# -lt 1 ]; then
    echo "❌ Usage: $0 <backup-file>"
    echo "   Example: $0 ./backups/discipolat_20260729_120000.sql.gz"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "❌ Backup file not found: ${BACKUP_FILE}"
    exit 1
fi

# Database config (overridable via env vars)
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"
DB_NAME="${POSTGRES_DB:-discipolat}"
DB_USER="${POSTGRES_USER:-discipolat}"
DB_PASSWORD="${POSTGRES_PASSWORD:-discipolat_secret}"

echo "⚠️  WARNING: This will OVERWRITE the current database!"
echo "  Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}"
echo "  Source:   ${BACKUP_FILE}"
echo ""
read -p "Are you sure you want to continue? (yes/no): " CONFIRM

if [ "${CONFIRM}" != "yes" ]; then
    echo "❌ Restore cancelled."
    exit 0
fi

echo "🔄 Starting database restore..."

# Drop and recreate database
echo "  Dropping existing connections..."
PGPASSWORD="${DB_PASSWORD}" psql \
    --host="${DB_HOST}" \
    --port="${DB_PORT}" \
    --username="${DB_USER}" \
    --dbname="postgres" \
    -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${DB_NAME}';" 2>/dev/null || true

echo "  Restoring from backup..."
PGPASSWORD="${DB_PASSWORD}" pg_restore \
    --host="${DB_HOST}" \
    --port="${DB_PORT}" \
    --username="${DB_USER}" \
    --dbname="${DB_NAME}" \
    --format=custom \
    --verbose \
    --clean \
    --if-exists \
    --no-owner \
    "${BACKUP_FILE}" 2>&1 | grep -v "^$"

echo "✅ Database restore completed successfully!"
echo "  Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}"
echo "  Source:   ${BACKUP_FILE}"
