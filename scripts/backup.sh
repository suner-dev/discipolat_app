#!/bin/bash
# Discipolat - Database Backup Script
# Usage: ./scripts/backup.sh [output-dir]
# Default: ./backups/

set -euo pipefail

BACKUP_DIR="${1:-./backups}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/discipolat_${TIMESTAMP}.sql.gz"
RETENTION_DAYS=30

# Database config (overridable via env vars)
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"
DB_NAME="${POSTGRES_DB:-discipolat}"
DB_USER="${POSTGRES_USER:-discipolat}"
DB_PASSWORD="${POSTGRES_PASSWORD:-discipolat_secret}"

# Create backup directory
mkdir -p "${BACKUP_DIR}"

echo "🔐 Starting Discipolat database backup..."
echo "  Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}"
echo "  Output:   ${BACKUP_FILE}"

# Perform backup
PGPASSWORD="${DB_PASSWORD}" pg_dump \
    --host="${DB_HOST}" \
    --port="${DB_PORT}" \
    --username="${DB_USER}" \
    --dbname="${DB_NAME}" \
    --format=custom \
    --verbose \
    --no-owner \
    --compress=9 \
    --file="${BACKUP_FILE}" 2>&1 | grep -v "^$"

echo "✅ Backup completed: ${BACKUP_FILE}"

# Cleanup old backups (older than RETENTION_DAYS)
echo "🧹 Cleaning backups older than ${RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -name "discipolat_*.sql.gz" -type f -mtime "+${RETENTION_DAYS}" -delete

# Generate backup report
BACKUP_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
echo "📊 Backup size: ${BACKUP_SIZE}"
echo "📁 Backup directory: ${BACKUP_DIR}"

echo "✨ Backup completed successfully!"
