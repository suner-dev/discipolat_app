#!/bin/bash
# G6.9 — dry-run + replay de la migration legacy (engine G4.6).
# N'écrit RIEN en base : dryRun=true, puis replay sur le même job.
# Usage :
#   ./scripts/legacy-migration-dryrun.sh <API_BASE> <JWT_ADMIN> <JOB_ID> <CSV_FILE>
# Ex: ./scripts/legacy-migration-dryrun.sh https://discipolat-beta-api.onrender.com "$TOKEN" <jobUuid> ./pilote-afrique.csv
set -euo pipefail
API="${1:?Usage: $0 <API_BASE> <JWT> <JOB_ID> <CSV_FILE>}"
JWT="${2:?Usage: $0 <API_BASE> <JWT> <JOB_ID> <CSV_FILE>}"
JOB="${3:?Usage: $0 <API_BASE> <JWT> <JOB_ID> <CSV_FILE>}"
CSV="${4:?Usage: $0 <API_BASE> <JWT> <JOB_ID> <CSV_FILE>}"
[ -f "$CSV" ] || { echo "CSV introuvable: $CSV"; exit 1; }
echo "== 1/2 dry-run (aucune ecriture) =="
curl -fsS -X POST "$API/api/v1/data-migration/$JOB/execute?dryRun=true" \
  -H "Authorization: Bearer $JWT" -F "file=@$CSV;type=text/csv" | tee /tmp/dryrun.json
echo; echo "== 2/2 replay (reexecution idempotente) =="
curl -fsS -X POST "$API/api/v1/data-migration/$JOB/replay" \
  -H "Authorization: Bearer $JWT" -F "file=@$CSV;type=text/csv" | tee /tmp/replay.json
echo; echo "OK — controle : aucune donnee source effacee (rollback dispo: DELETE /api/v1/data-migration/$JOB)."
