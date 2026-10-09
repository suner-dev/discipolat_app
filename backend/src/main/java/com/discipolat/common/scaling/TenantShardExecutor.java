package com.discipolat.common.scaling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A3 (M7) — Exécution d'une unité de travail sur le shard d'un tenant.
 *
 * <p>Le contrat est volontairement plus étroit qu'un {@code DataSource} : il ne
 * s'agit PAS d'ouvrir une connexion par requête (ce qu'un pool fait déjà), mais
 * de donner au code un point d'entrée où lire et écrire les données d'un tenant
 * donné, en Leaving au pool la gestion des connexions.</p>
 *
 * <p>Deux implémentations :</p>
 * <ul>
 *   <li>{@link SingleDatabaseTenantDataSource} — une seule base, comportement
 *       actuel, zéro régression (par défaut) ;</li>
 *   <li>{@link ShardedTenantDataSource} — plusieurs bases, une par shard.</li>
 * </ul>
 *
 * <p>Le choix se fait à la construction du bean : aucune classe métier ne
 * référence une implémentation précise, donc passer en mode sharded ne demande
 * qu'une configuration.</p>
 */
public interface TenantShardExecutor extends TenantDataSource {

    /**
     * Exécute [work] en résolvant d'abord le shard du tenant.
     *
     * <p>Un tenant inconnu ou absent de l'anneau ne doit pas écrire dans un
     * shard arbitraire : cela reviendrait à melanger les données d'une église
     * dans la base d'une autre. On refuse donc explicitement.</p>
     */
    <R> R execute(UUID tenantId, ShardedWork<R> work);

    /** Alias fortement typé de {@link #execute}, pour la lecture. */
    default <R> R query(UUID tenantId, ShardedWork<R> work) {
        return execute(tenantId, work);
    }

    /** Alias fortement typé de {@link #execute}, pour l'écriture. */
    default <R> R mutate(UUID tenantId, ShardedWork<R> work) {
        return execute(tenantId, work);
    }

    /** Unit of work exécutée dans le contexte d'un tenant. */
    @FunctionalInterface
    interface ShardedWork<R> {
        R runInShard(java.sql.Connection connection) throws Exception;
    }

    /**
     * Shards effectivement configurés, indexés par leur identifiant.
     *
     * <p>Exposé pour l'observabilité et la recette : sans cette vue, on ne peut
     * ni mesurer la répartition, ni écrire un test qui vérifie qu'un tenant
     * atterrit bien où l'on croit.</p>
     */
    default Map<Integer, String> shardTopology() {
        return Collections.emptyMap();
    }

    /**
     * Identifiants de tous les shards de l'anneau, TRIÉS.
     *
     * <p>Le tri est délibéré : {@code keySet()} d'une {@code Map.copyOf} ou d'une
     * {@code HashMap} n'a aucun ordre garanti, donc un affichage ou une assertion
     * sur l'ordre pouvait varier d'une JVM à l'autre. Un observabilité instable
     * est une observabilité que l'on ne peut pas comparer.</p>
     */
    default List<Integer> shardIds() {
        return shardTopology().keySet().stream().sorted().toList();
    }
}

/**
 * A3 (M7) — Anneau de shards : une entrée par shard, Fail-fast sur l'incohérence.
 *
 * <p>Un anneau mal configuré (ids non contigus, doublons, shard manquant) est un
 * bug de DÉPLOIEMENT qui produirait des écritures dans la mauvaise base. Plutôt
 * que de le détecter en production, on le refuse au démarrage.</p>
 */
final class ShardRing {

    private final Map<Integer, String> topology;

    private ShardRing(Map<Integer, String> topology) {
        this.topology = Map.copyOf(topology);
    }

    static ShardRing of(Map<Integer, String> topology) {
        if (topology == null || topology.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aucun shard configuré. Le mode sharded exige au moins une base.");
        }
        for (Map.Entry<Integer, String> entry : topology.entrySet()) {
            if (entry.getKey() == null || entry.getKey() < 0) {
                throw new IllegalStateException(
                        "Identifiant de shard invalide : " + entry.getKey()
                                + " (attendu : entier >= 0)");
            }
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                throw new IllegalStateException(
                        "Shard " + entry.getKey() + " sans identifiant de base.");
            }
        }
        // Un anneau contigu 0..N-1 rend `hash % N` et `shardFor` cohérents avec
        // la topologie déclarée : sans cette contrainte, un shard 3 déclaré et
        // absent de la table de routage serait inatteignable.
        for (int expected = 0; expected < topology.size(); expected++) {
            if (!topology.containsKey(expected)) {
                throw new IllegalStateException(
                        "Anneau de shards incomplet : identifiant " + expected
                                + " attendu mais absent. Les ids doivent être contigus (0..N-1).");
            }
        }
        return new ShardRing(new LinkedHashMap<>(topology));
    }

    int shardCount() {
        return topology.size();
    }

    Map<Integer, String> topology() {
        return topology;
    }

    String label(int shard) {
        String label = topology.get(shard);
        if (label == null) {
            throw new IllegalStateException(
                    "Shard " + shard + " absent de l'anneau (taille " + topology.size() + ").");
        }
        return label;
    }
}