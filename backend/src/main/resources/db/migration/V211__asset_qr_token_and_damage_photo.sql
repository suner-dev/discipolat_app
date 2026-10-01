-- V211 (PORTÉ de Develop1 V166) — G5.6 : mobile terrain connecté — QR d'inventaire signé + photo de dommage d'actif
-- Le QR d'actif contient un jeton opaque aléatoire par objet (infalsifiable, tenant-scopé à la résolution).
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS qr_token VARCHAR(36) UNIQUE;
CREATE INDEX IF NOT EXISTS idx_inventory_items_qr_token ON inventory_items (qr_token);

-- Photo de dommage stockée sur le serveur (chemin relatif), enregistrée au retour d'actif.
ALTER TABLE asset_checkout ADD COLUMN IF NOT EXISTS damage_photo_path VARCHAR(512);
