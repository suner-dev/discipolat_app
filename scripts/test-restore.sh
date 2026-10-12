#!/bin/bash
# G6.9 — test de restauration : restaure un dump chiffre (backup-postgres.yml)
# dans une BASE STAGING DEDIEE (jamais prod), puis verifie l'integrite.
# Usage :
#   ENC_KEY=<passphrase> POSTGRES_HOST=... ./scripts/test-restore.sh <dump.sql.enc> [DB_STAGING]
set -euo pipefail
ENC="${1:?Usage: ENC_KEY=... $0 <dump.sql.enc> [DB_STAGING]}"
STAGING_DB="${2:-${POSTGRES_DB_STAGING:-discipolat_staging_restore}}"
: "${ENC_KEY:?ENC_KEY manquante (passphrase BACKUP_ENCRYPTION_KEY)}"
DB_HOST="${POSTGRES_HOST:-localhost}"; DB_PORT="${POSTGRES_PORT:-5432}"
DB_USER="${POSTGRES_USER:-discipolat}"; DB_PASSWORD="${POSTGRES_PASSWORD:-discipolat_secret}"
TMP="$(mktemp /tmp/restore_XXXX.sql)"
trap 'rm -f "$TMP"' EXIT
echo "== 1/4 dechiffrement (memoire/tmp, jamais commite) =="
openssl enc -d -aes-256-cbc -pbkdf2 -iter 100000 -pass env:ENC_KEY -in "$ENC" -out "$TMP"
# Garde d'en-tete : le fichier dechiffre doit bien etre un dump pg_dump.
#
# CORRECTION 2026-10-12 — l'ancien test (head -1 | grep -q « PostgreSQL database
# dump ») etait faux sur deux points mesures : (1) depuis PostgreSQL 12, pg_dump
# ecrit « -- » sur la premiere ligne et le libelle « PostgreSQL database dump »
# sur la DEUXIEME ; le test rejetait donc un dump parfaitement valide
# (« Dump invalide », reproduit avec pg_dump 16.15) ; (2) un tube head | grep -q
# sous set -o pipefail peut rendre 141 (SIGPIPE) quand grep s'arrete a la 2e
# ligne : le test de securite « une mauvaise cle ne doit rien dechiffrer »
# echouerait alors dans le bon sens pour une mauvaise raison. grep -m1 lit le
# fichier sans tube : comportement deterministe.
grep -q -m1 "PostgreSQL database dump" "$TMP" || { echo "Dump invalide"; exit 1; }
echo "== 2/4 reset base staging $STAGING_DB =="
export PGPASSWORD="$DB_PASSWORD"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS \"$STAGING_DB\";"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "CREATE DATABASE \"$STAGING_DB\";"
echo "== 3/4 restore =="
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$STAGING_DB" -f "$TMP" -q
echo "== 4/4 controles integrite =="
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$STAGING_DB" -c "SELECT count(*) AS flyway_migrations FROM flyway_schema_history;"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$STAGING_DB" -c "SELECT count(*) AS tenants FROM tenants;" || true
echo "OK restauration staging — consigner date/heure/taille dans reports/PROD_READINESS_REPORT.md."
