package com.discipolat.common.scaling;

import java.util.UUID;

/**
 * A3 (M7) — Abstraction d'accès base de données PAR TENANT.
 *
 * <p>Contexte : le produit vise plusieurs millions de tenants. Aujourd'hui,
 * tout le monde vit dans une seule base PostgreSQL (schéma partagé + filtre
 * Hibernate {@code tenant_id}). Cette interface est la frontière non
 * rétrocompatible qui permet demain de router un tenant vers un autre cluster
 * SANS toucher aux services : la signature prend le tenantId et renvoie le
 * {@link javax.sql.DataSource} qui lui appartient.</p>
 *
 * <p>Règle de routage figée par l'architecture : shard = {@code hash(tenantId) % N}
 * (voir {@link ShardRouting}), N configurable. L'implémentation par défaut
 * {@link SingleDatabaseTenantDataSource} ignore le routage et renvoie toujours
 * le DataSource courant : comportement strictement identique à aujourd'hui,
 * zéro régression — c'est la fondation, pas la mise en shard.</p>
 */
public interface TenantDataSource {

    /** DataSource sur lequel les données de ce tenant doivent être lues/écrites. */
    javax.sql.DataSource dataSourceFor(UUID tenantId);

    /**
     * Identifiant de shard calculé pour ce tenant (0 si routage unique).
     * Exposée pour l'observabilité : métriques, logs d'onboarding, plans de
     * migration — jamais pour porter de la logique métier.
     */
    int shardFor(UUID tenantId);
}
