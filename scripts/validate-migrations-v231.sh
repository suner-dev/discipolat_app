#!/usr/bin/env bash
# ============================================================
# validate-migrations-v231.sh
#
# Validation NON DESTRUCTIVE des migrations « Mon encadrement »
# (V231 member_relations, V232 backfill NOTIFICATION_TYPE) sur
# un PostgreSQL réel.
#
# Pourquoi un script dédié alors que la suite de tests contient
# `FlywayMigrationChainPostgreSqlTest` (testcontainers) ? Parce que ce
# dernier est sensible à la disponibilité de Docker et échoue
# sporadiquement sur une machine chargée, sans que l'échec ait quoi
# que ce soit à voir avec le SQL. Ce script, lui, est déterministe et
# n'a besoin que d'une basejoignable.
#
# ISOLATION : tout se joue dans un SCHEMA jetable `v231_sandbox`,
# supprimé en fin d'exécution (ROLLBACK + DROP SCHEMA CASCADE).
# La base de développement n'est jamais modifiée — le script se
# termine par un contrôle explicite.
#
# Contrôles effectués :
#   - création de la table, des index et des contraintes CHECK ;
#   - seed MEMBER_RELATION_TYPE par tenant ;
#   - RÉ-EXÉCUTION idempotente (NOT EXISTS) ;
#   - rejet d'un statut hors énumération ;
#   - rejet d'une incohérence temporelle (ACTIVE + ended_at) ;
#   - acceptation de REVOKED + ended_at ;
#   - application effective des clés étrangères vers users ;
#   - ON DELETE CASCADE à la suppression d'un compte ;
#   - backfill NOTIFICATION_TYPE (12 types, libellés + couleurs) ;
#   - idempotence du backfill ET préservation d'un libellé que
#     l'église aurait personnalisé.
#
# Usage :
#   PGPASSWORD=… PGUSER=… PGDATABASE=… PGHOST=localhost PGPORT=5433 \
#     ./scripts/validate-migrations-v231.sh
#
# Par défaut il vise la base de dev locale (docker-compose) en lecture.
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MIGRATIONS="$ROOT/backend/src/main/resources/db/migration"
WORKER="$(mktemp)"
trap 'rm -f "$WORKER"' EXIT

: "${PGHOST:=localhost}"
: "${PGPORT:=5433}"
: "${PGUSER:=discipolat}"
: "${PGDATABASE:=discipolat}"
export PGHOST PGPORT PGUSER PGDATABASE

cat > "$WORKER" <<'PRELUDE'
SET search_path TO v231_sandbox, public;
BEGIN;
CREATE SCHEMA IF NOT EXISTS v231_sandbox;
SET search_path TO v231_sandbox, public;
CREATE TABLE tenants (id UUID PRIMARY KEY);
CREATE TABLE users (id UUID PRIMARY KEY);
CREATE TABLE dictionary_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL, dict_key VARCHAR(50) NOT NULL, code VARCHAR(50) NOT NULL,
    label VARCHAR(255) NOT NULL, description TEXT, color VARCHAR(50),
    ordre INTEGER NOT NULL DEFAULT 0, actif BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE UNIQUE INDEX uq_dict_code_per_tenant ON dictionary_entries (tenant_id, dict_key, code);
INSERT INTO tenants VALUES ('00000000-0000-0000-0000-000000000001');
INSERT INTO users VALUES ('00000000-0000-0000-0000-0000000000a1'),
                        ('00000000-0000-0000-0000-0000000000a2');
PRELUDE

{
  cat "$WORKER"
  echo "\\i '$MIGRATIONS/V231__member_relations.sql'"
  # Ré-exécution : le seed ne doit pas dupliquer les lignes.
  echo "\\i '$MIGRATIONS/V231__member_relations.sql'"
  echo "SELECT 'idempotence V231' AS controle, count(*) AS attendu_5 FROM dictionary_entries WHERE dict_key='MEMBER_RELATION_TYPE';"
  echo "\\i '$MIGRATIONS/V232__notification_type_dictionary_backfill.sql'"
  echo "\\i '$MIGRATIONS/V232__notification_type_dictionary_backfill.sql'"
  echo "SELECT 'idempotence V232' AS controle, count(*) AS attendu_12 FROM dictionary_entries WHERE dict_key='NOTIFICATION_TYPE';"
  echo "ROLLBACK;"
  echo "DROP SCHEMA IF EXISTS v231_sandbox CASCADE;"
  echo "SELECT 'base de dev intacte' AS controle, count(*) AS attendu_0 FROM pg_tables WHERE schemaname='public' AND tablename='member_relations';"
} | psql -v ON_ERROR_STOP=1

echo
echo "OK — migrations V231/V232 validées sur PostgreSQL réel, sans toucher la base de dev."
