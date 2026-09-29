-- V189 : catalogue des sauvegardes applicatives (constat M2, tâche A2).
--
-- Le cahier de charge annonce `POST /backups/{id}/verify` et un module de
-- sauvegarde/restauration en Java, mais aucun `modules/backup/` n'existait :
-- seules des preuves par `scripts/backup.sh`. L'API annoncée était donc fictive.
--
-- ADDITIF STRICT : aucun DROP, aucune modification de migration existante.
--
-- Le module exporte un dump LOGIQUE par tenant (gzip d'INSERT) et en conserve
-- l'empreinte SHA-256, afin que `verify` puisse relire réellement le fichier et
-- détecter une corruption. Une empreinte sans relecture serait un mensonge de
-- plus : c'est précisément ce que la vérification doit empêcher.

CREATE TABLE IF NOT EXISTS backup_archive (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id  UUID        NOT NULL REFERENCES tenants (id),
    file_name  VARCHAR(255) NOT NULL,
    size_bytes BIGINT      NOT NULL,
    sha256     VARCHAR(64)  NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    status     VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    CONSTRAINT ck_backup_archive_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'VERIFIED'))
);

-- Liste des sauvegardes d'un tenant : l'ordre antéchronologique est celui du
-- parcours utilisateur, donc l'index le porte.
CREATE INDEX IF NOT EXISTS idx_backup_archive_tenant
    ON backup_archive (tenant_id, created_at DESC);

-- Un nom de fichier ne doit pas être réutilisé au sein d'un même tenant :
-- deux sauvegardes concurrentes ne peuvent pas s'écraser.
CREATE UNIQUE INDEX IF NOT EXISTS uk_backup_archive_tenant_file
    ON backup_archive (tenant_id, file_name);

COMMENT ON TABLE backup_archive IS
    'Catalogue des sauvegardes logiques par tenant (constat M2). sha256 est '
    'recalcule a la verification : une archive corrompue doit etre signalee, '
    'jamais declaree valide.';
