package com.discipolat.common.scaling;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.UUID;

/**
 * A3 (M7) — Implémentation par défaut : une seule base pour tous les tenants.
 *
 * <p>Délègue au DataSource unique applicatif : COMPORTEMENT IDENTIQUE à
 * aujourd'hui, zéro régression. Le nombre de shards configuré
 * ({@code app.scaling.shard-count}, défaut 1) n'influence AUCUNE lecture — il
 * sert uniquement à rendre l'anneau de routage observable (métriques,
 * onboarding) dès maintenant, pour que le jour du découpage la décision soit
 * opérationnelle et pas un changement de code.</p>
 */
@Component
public class SingleDatabaseTenantDataSource implements TenantDataSource {

    private final DataSource primary;
    private final int shardCount;

    public SingleDatabaseTenantDataSource(DataSource primary,
                                          @Value("${app.scaling.shard-count:1}") int shardCount) {
        if (shardCount < 1) {
            throw new IllegalStateException(
                    "app.scaling.shard-count doit être >= 1 (reçu : " + shardCount + ")");
        }
        this.primary = primary;
        this.shardCount = shardCount;
    }

    @Override
    public DataSource dataSourceFor(UUID tenantId) {
        // Niveau 1 (docs/SCALING.md) : tous les tenants sur la base primaire.
        return primary;
    }

    @Override
    public int shardFor(UUID tenantId) {
        return ShardRouting.shardOf(tenantId, shardCount);
    }
}
