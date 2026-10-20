-- G4.6 — moteur d'import réel : traçabilité des entités créées (rollback) + dry-run
ALTER TABLE data_migration_jobs ADD COLUMN IF NOT EXISTS created_entity_ids TEXT;
ALTER TABLE data_migration_jobs ADD COLUMN IF NOT EXISTS rolled_back_at TIMESTAMP;
ALTER TABLE data_migration_jobs ADD COLUMN IF NOT EXISTS last_run_dry BOOLEAN;