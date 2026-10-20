-- Inventory module: church equipment, stock, assignments, maintenance
CREATE TABLE IF NOT EXISTS inventory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    categorie VARCHAR(50) NOT NULL DEFAULT 'MATERIEL',
    statut VARCHAR(50) NOT NULL DEFAULT 'DISPONIBLE',
    quantite INTEGER DEFAULT 1,
    quantite_disponible INTEGER DEFAULT 1,
    valeur_unitaire DOUBLE PRECISION,
    lieu_stockage VARCHAR(255),
    numero_serie VARCHAR(100),
    date_acquisition TIMESTAMP,
    derniere_maintenance TIMESTAMP,
    prochaine_maintenance TIMESTAMP,
    departement_id UUID,
    affecte_a_id UUID,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inventory_tenant ON inventory_items(tenant_id);
CREATE INDEX idx_inventory_categorie ON inventory_items(tenant_id, categorie);
CREATE INDEX idx_inventory_statut ON inventory_items(tenant_id, statut);
CREATE INDEX idx_inventory_departement ON inventory_items(tenant_id, departement_id);
