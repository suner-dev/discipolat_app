package com.discipolat.common.scaling;


import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;

/**
 * A3 (M7) — Implémentation du routage par shard : une base par shard, un tenant
 * par shard.
 *
 * <p>Le routage utilise {@link ShardRouting} (hash stable, {@code floorMod}) :
 * le même tenant tombe donc sur le même shard dans toutes les JVM, ce qui est la
 * condition pour un routage déterministe en production.</p>
 *
 * <p>Différence de comportement avec {@link SingleDatabaseTenantDataSource}, et
 * elle est volontaire : ici, un tenant hors anneau est REFUSÉ. Dans le mode
 * mono-base, un tenant inconnu renvoie la base primaire. En multi-base, renvoyer
 * « la base par défaut » pour un tenant non routé reviendrait à écrire les
 * données d'une église dans le shard d'une autre — perte de données silencieuse
 * et violation d'isolation. On préfère une erreur explicite.</p>
 */
public class ShardedTenantDataSource implements TenantShardExecutor {

    private final ShardRing ring;
    private final DataSourceResolver resolver;

    public ShardedTenantDataSource(ShardRing ring, DataSourceResolver resolver) {
        this.ring = ring;
        this.resolver = resolver;
    }

    /** Résout le shard courant vers le DataSource correspondant. */
    @FunctionalInterface
    public interface DataSourceResolver {
        DataSource resolve(int shard);
    }

    @Override
    public DataSource dataSourceFor(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException(
                    "tenantId requis pour le routage de shard");
        }
        int shard = shardFor(tenantId);
        DataSource dataSource = resolver.resolve(shard);
        if (dataSource == null) {
            throw new IllegalStateException(
                    "Aucun DataSource pour le shard " + shard + " (tenant " + tenantId + ").");
        }
        return dataSource;
    }

    @Override
    public int shardFor(UUID tenantId) {
        return ShardRouting.shardOf(tenantId, ring.shardCount());
    }

    @Override
    public <R> R execute(UUID tenantId, ShardedWork<R> work) {
        DataSource dataSource = dataSourceFor(tenantId);
        try (java.sql.Connection connection = dataSource.getConnection()) {
            return work.runInShard(connection);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception checked) {
            // On ne masque pas la cause derrière une exception generique : un
            // defaut SQL du shard est actionnable, un `RuntimeException(e)` sans
            // message ne l'est pas.
            throw new ShardAccessException(
                    "Echec de l'unit of work sur le shard "
                            + shardFor(tenantId) + " (tenant " + tenantId + ")", checked);
        }
    }

    @Override
    public Map<Integer, String> shardTopology() {
        return ring.topology();
    }

    /** Erreur d'accès à un shard, avec la cause d'origine conservée. */
    public static class ShardAccessException extends RuntimeException {
        public ShardAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}