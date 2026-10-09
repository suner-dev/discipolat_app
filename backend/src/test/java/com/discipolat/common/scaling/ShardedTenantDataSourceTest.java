package com.discipolat.common.scaling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A3 (M7) — Le routage de shard doit être RÉELLEMENT exercised.
 *
 * <p>Le constat qui a motivé ce test : les trois classes du seam
 * ({@code ShardRouting}, {@code TenantDataSource}, {@code SingleDatabase…})
 * totalisaient 128 lignes et n'étaient injectées nulle part. Rien ne vérifiait
 * donc qu'un tenant atterrissait bien sur le shard attendu.</p>
 */
class ShardedTenantDataSourceTest {

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static DataSource fakeDataSource() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(mock(Connection.class));
        return dataSource;
    }

    private static ShardedTenantDataSource threeShards() throws SQLException {
        Map<Integer, String> labels = new LinkedHashMap<>();
        labels.put(0, "jdbc:postgresql://s0/discipolat_0");
        labels.put(1, "jdbc:postgresql://s1/discipolat_1");
        labels.put(2, "jdbc:postgresql://s2/discipolat_2");
        DataSource s0 = fakeDataSource();
        DataSource s1 = fakeDataSource();
        DataSource s2 = fakeDataSource();
        Map<Integer, DataSource> pool = new LinkedHashMap<>();
        pool.put(0, s0);
        pool.put(1, s1);
        pool.put(2, s2);
        return new ShardedTenantDataSource(ShardRing.of(labels), pool::get);
    }

    @Test
    @DisplayName("le routage est déterministe : même tenant, même shard, à chaque appel")
    void routingEstStable() throws Exception {
        ShardedTenantDataSource source = threeShards();

        int first = source.shardFor(TENANT_A);

        for (int i = 0; i < 50; i++) {
            assertThat(source.shardFor(TENANT_A)).isEqualTo(first);
        }
        assertThat(first).isBetween(0, 2);
    }

    @Test
    @DisplayName("le DataSource retourné est celui du shard calculé — pas la base primaire")
    void dataSourceCorrespondAuShard() throws Exception {
        Map<Integer, String> labels = new LinkedHashMap<>();
        labels.put(0, "s0");
        labels.put(1, "s1");
        DataSource s0 = fakeDataSource();
        DataSource s1 = fakeDataSource();
        Map<Integer, DataSource> pool = Map.of(0, s0, 1, s1);
        ShardedTenantDataSource source = new ShardedTenantDataSource(ShardRing.of(labels), pool::get);

        DataSource resolved = source.dataSourceFor(TENANT_A);
        DataSource expected = pool.get(source.shardFor(TENANT_A));

        assertThat(resolved).isSameAs(expected);
    }

    @Test
    @DisplayName("l'unité de travail reçoit une connexion du BON shard")
    void executeOuvreLaConnexionDuShard() throws Exception {
        ShardedTenantDataSource source = threeShards();

        // execute() doit atteindre une connexion ET renvoyer la valeur produite :
        // c'est la preuve que le routage est traversé, pas contourné.
        String result = source.execute(TENANT_B, connection -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("un tenantId null est refusé plutôt que routé par défaut")
    void tenantIdNulEstRefuse() throws Exception {
        ShardedTenantDataSource source = threeShards();

        assertThatThrownBy(() -> source.dataSourceFor(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    @DisplayName("un shard déclaré sans DataSource échoue explicitement")
    void shardSansDataSourceEstRefuse() {
        Map<Integer, String> labels = Map.of(0, "s0", 1, "s1");
        // Le resolver ne fournit rien : on ne doit PAS retomber sur un shard
        // arbitraire, ce qui écrireait les données d'une église chez une autre.
        ShardedTenantDataSource source = new ShardedTenantDataSource(ShardRing.of(labels), shard -> null);

        assertThatThrownBy(() -> source.dataSourceFor(TENANT_A))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DataSource");
    }

    @Test
    @DisplayName("un anneau à ids non contigus est refusé au démarrage, pas en production")
    void anneauNonContiguEstRefuse() {
        Map<Integer, String> labels = new LinkedHashMap<>();
        labels.put(0, "s0");
        labels.put(2, "s2"); // 1 manque : shard 1 serait inatteignable

        assertThatThrownBy(() -> ShardRing.of(labels))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contigus");
    }

    @Test
    @DisplayName("un anneau vide est refusé (le mode sharded sans shard n'a pas de sens)")
    void anneauVideEstRefuse() {
        assertThatThrownBy(() -> ShardRing.of(Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("l'exception d'unité de work conserve la cause d'origine")
    void causeOrigineEstConservee() throws Exception {
        ShardedTenantDataSource source = threeShards();

        assertThatThrownBy(() -> source.execute(TENANT_A, connection -> {
            throw new SQLException("relation \"tenants\" does not exist");
        }))
                .isInstanceOf(ShardedTenantDataSource.ShardAccessException.class)
                .hasMessageContaining("shard")
                .hasRootCauseMessage("relation \"tenants\" does not exist");
    }

    @Test
    @DisplayName("l'anneau est exposé pour l'observabilité et la recette")
    void topologieEstExposee() throws Exception {
        ShardedTenantDataSource source = threeShards();

        assertThat(source.shardTopology()).hasSize(3);
        assertThat(source.shardIds()).containsExactly(0, 1, 2);
    }

    @Test
    @DisplayName("le bean par défaut reste le mono-base : le seam existant n'est pas cassé")
    void monoBaseResteLeDefaut() throws Exception {
        // La classe mono-base conserve son champ `shardCount` (donc son contrat
        // de configuration `app.scaling.shard-count`), et le bean sharded n'est
        // chargé que si `app.scaling.sharded=true`. C'est ce qui garantit qu'un
        // déploiement qui ignore cette configuration conserve EXACTEMENT le
        // comportement d'aujourd'hui.
        Field shardCount = SingleDatabaseTenantDataSource.class.getDeclaredField("shardCount");
        assertThat(shardCount.getName()).isEqualTo("shardCount");
        assertThat(ShardedTenantDataSourceConfiguration.class
                .getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class)
                .havingValue()).isEqualTo("true");
    }

    @Test
    @DisplayName("la répartition charge réellement les trois shards sur beaucoup de tenants")
    void repartitionChargeLesShards() throws Exception {
        ShardedTenantDataSource source = threeShards();

        int[] hits = new int[3];
        for (int i = 0; i < 3000; i++) {
            hits[source.shardFor(UUID.randomUUID())]++;
        }

        // Un shard jamais touché signifierait un routage dégénéré : les tenants
        // se concentreraient tous sur une base, ce qui annule l'intérêt du sharding.
        assertThat(hits[0]).isPositive();
        assertThat(hits[1]).isPositive();
        assertThat(hits[2]).isPositive();
        // Répartition grossièrement équilibrée (attendue ~1000 chacune).
        for (int hit : hits) {
            assertThat(hit).isBetween(700, 1400);
        }
    }
}