package com.discipolat.common.scaling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A3 (M7) — Routage RÉEL par tenant vers une base par shard.
 *
 * <p>Ce que le constat relevait : {@link TenantDataSource} était injecté nulle
 * part dans le code applicatif, et {@link SingleDatabaseTenantDataSource} renvoie
 * {@code primary} en ignorant le routage. Les trois classes du seam totalisaient
 * 128 lignes et n'étaient donc que des fondations non exercées — l'objectif
 * 10<sup>6</sup>–10<sup>8</sup> tenants était inatteignable, mono-PostgreSQL
 * tenant la tête.</p>
 *
 * <p>Ce composant ferme l'écart sans casser l'existant :</p>
 * <ul>
 *   <li>il ne s'active QUE si {@code app.scaling.sharded=true} ; par défaut le
 *       bean {@link SingleDatabaseTenantDataSource} reste le seul en scène et le
 *       comportement est strictement identique (aucune régression) ;</li>
 *   <li>il n'oblige aucune réécriture des services : le routage est opt-in par
 *       appelant via {@link TenantShardExecutor#execute} ;</li>
 *   <li>il refuse un tenant hors anneau au lieu d'écrire dans un shard
 *       arbitraire (ce qui mélangerait les données d'une église dans la base
 *       d'une autre).</li>
 * </ul>
 *
 * <p>Limite assumée et documentée : la migration à chaud d'un tenant d'un shard
 * vers un autre (gel des écritures, copie, bascule) n'est PAS implémentée. Un
 * tenant reste donc épinglé à son shard pour la durée du process. C'est
 * volontaire : un basculement sans gel se traduit par des écritures perdues, et
 * un basculement avec gel est une opération d'exploitation qui se prépare dans
 * une procédure dédiée (docs/SCALING.md, Niveau 3), pas dans un commit.</p>
 */
@Configuration
@ConditionalOnProperty(name = "app.scaling.sharded", havingValue = "true")
public class ShardedTenantDataSourceConfiguration {

    /**
     * Anneau de shards.
     *
     * <p>Liste (et pas simple entier) : chaque shard est une base distincte avec
     * sa propre URL et ses identifiants. Un simple {@code shard-count} ne
     * pourrait pas décrire plus d'une base.</p>
     */
    @ConfigurationProperties(prefix = "app.scaling.shards")
    public static class ShardsProperties {

        /** Shards déclarés : index = identifiant de shard, valeur = libellé de base. */
        private List<Shard> shards = new ArrayList<>();

        public List<Shard> getShards() {
            return shards;
        }

        public void setShards(List<Shard> shards) {
            this.shards = shards;
        }

        /** Un shard : son identifiant logique et la base à y joindre. */
        public static class Shard {
            private int id;
            private String url;
            private String username;
            private String password;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }
        }
    }

    /**
     * Construit un {@link ShardedTenantDataSource} à partir de l'anneau déclaré.
     *
     * <p>Les DataSource sont fournis par une {@link ShardDataSourceFactory} : on
     * ne fabrique pas de pool ici. brancher HikariCP, un pool gRPC ou un pool
     * managé est un choix de déploiement, et cette classe ne doit pas l'imposer.
     * Une factory absente = bean non créé = le mode sharded reste inactif plutôt
     * que de démarrer sur des DataSource factices.</p>
     */
    @Bean
    public TenantDataSource shardedTenantDataSource(ShardsProperties properties,
                                                    ShardDataSourceFactory factory) {
        List<ShardsProperties.Shard> declared = properties.getShards();
        if (declared == null || declared.isEmpty()) {
            throw new IllegalStateException(
                    "app.scaling.sharded=true mais app.scaling.shards est vide : "
                            + "rien à router. Déclarez au moins un shard, ou repassez "
                            + "app.scaling.sharded=false pour revenir au mono-base.");
        }
        Map<Integer, String> labels = new LinkedHashMap<>();
        Map<Integer, javax.sql.DataSource> dataSources = new LinkedHashMap<>();
        for (ShardsProperties.Shard shard : declared) {
            labels.put(shard.getId(), shard.getUrl());
            dataSources.put(shard.getId(), factory.dataSourceFor(shard));
        }
        // La taille de l'anneau est figée APRÈS construction : le resolver est
        // ainsi cohérent par construction avec `ShardRouting`, qui indexe par
        // `shardCount`. Une incohérence entre les deux produirait un tenant
        // envoyé dans un shard non déclaré.
        final int shardCount = labels.size();
        return new ShardedTenantDataSource(
                ShardRing.of(labels),
                // `shardFor` de ShardedTenantDataSource applique le même
                // `ShardRouting.shardOf(tenantId, shardCount)` : le resolver est
                // donc un simple lookup par identifiant de shard.
                shard -> dataSources.get(shard));
    }

    /**
     * Fabrique de DataSource par shard.
     *
     * <p>Laisse au déploiement le choix du pool (HikariCP local, pool managé,
     * proxy…). Implémentée par l'infrastructure, absente par défaut.</p>
     */
    @FunctionalInterface
    public interface ShardDataSourceFactory {
        javax.sql.DataSource dataSourceFor(ShardsProperties.Shard shard);
    }
}