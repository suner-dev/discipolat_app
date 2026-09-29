package com.discipolat.common.scaling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A3 (M7) — Le routage de shard doit être STABLE (même résultat partout,
 * tout le temps), borné ({@code [0, N)} même sur hash négatif), équilibré, et
 * l'implémentation par défaut ne doit RIEN changer aux lectures existantes.
 */
class ShardingRoutingTest {

    /** UUIDs séquentiels « comme en production » (un tenant créé après l'autre). */
    private static UUID tenantAt(int i) {
        return UUID.nameUUIDFromBytes(("tenant-" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("Stabilité du hash")
    class Stabilité {

        @Test
        @DisplayName("deux appels sur le même UUID produisent le même hash, même entre instances")
        void hashDeterministe() {
            UUID tenant = tenantAt(42);
            UUID reparsed = UUID.fromString(tenant.toString());

            // Stabilité exigée par M7 : même entrée ⇔ même sortie, ici et toujours.
            assertThat(ShardRouting.hash(tenant)).isEqualTo(ShardRouting.hash(reparsed));
            assertThat(ShardRouting.hash(tenant)).isEqualTo(ShardRouting.hash(tenant));
            assertThat(ShardRouting.shardOf(tenant, 64)).isEqualTo(ShardRouting.shardOf(reparsed, 64));
        }

        @Test
        @DisplayName("des tenants différents tombent sur des hashes différents (pas d'écrasement)")
        void hashQuasiInjectif() {
            Map<Long, UUID> seen = new HashMap<>();
            for (int i = 0; i < 10_000; i++) {
                UUID tenant = tenantAt(i);
                UUID collision = seen.put(ShardRouting.hash(tenant), tenant);
                assertThat(collision).as("collision de hash entre %s et %s", collision, tenant).isNull();
            }
        }

        @Test
        @DisplayName("shardOf cohérent avec floorMod(hash, N) — la règle figée par l'architecture")
        void shardRespecteLaRegleOfficielle() {
            for (int i = 0; i < 500; i++) {
                UUID tenant = tenantAt(i);
                for (int n : new int[]{1, 2, 7, 16, 1024}) {
                    assertThat(ShardRouting.shardOf(tenant, n))
                            .isEqualTo((int) Math.floorMod(ShardRouting.hash(tenant), (long) n));
                }
            }
        }
    }

    @Nested
    @DisplayName("Bornes et distribution")
    class Distribution {

        @Test
        @DisplayName("anneau de 1 : tout le monde sur le shard 0 (configuration par défaut)")
        void unSeulShardToutALaMemePlace() {
            for (int i = 0; i < 1000; i++) {
                assertThat(ShardRouting.shardOf(tenantAt(i), 1)).isZero();
            }
        }

        @Test
        @DisplayName("hash négatif inclus : le shard reste dans [0, N) — Math.floorMod, pas %")
        void bornesMemeSurHashNegatif() {
            boolean negativeSeen = false;
            for (int i = 0; i < 5_000 && !negativeSeen; i++) {
                UUID tenant = tenantAt(i);
                if (ShardRouting.hash(tenant) < 0) {
                    negativeSeen = true;
                    assertThat(ShardRouting.shardOf(tenant, 64)).isBetween(0, 63);
                }
            }
            assertThat(negativeSeen).as("le jeu doit contenir des hashes négatifs pour prouver floorMod").isTrue();
        }

        @Test
        @DisplayName("20 000 tenants sur 8 shards : répartition équilibrée, aucun shard vide ni saturé")
        void repartitionEquilibree() {
            int shards = 8;
            int tenants = 20_000;
            Map<Integer, Integer> counts = new HashMap<>();
            for (int i = 0; i < tenants; i++) {
                int shard = ShardRouting.shardOf(UUID.randomUUID(), shards);
                assertThat(shard).isBetween(0, shards - 1);
                counts.merge(shard, 1, Integer::sum);
            }
            double expected = (double) tenants / shards;
            assertThat(counts).hasSize(shards);
            counts.forEach((shard, count) -> assertThat(count.doubleValue())
                    .as("shard %d sur-représenté (écart > 25 %)", shard)
                    .isBetween(expected * 0.75, expected * 1.25));
        }
    }

    @Nested
    @DisplayName("Garde-fous des paramètres")
    class GardeFous {

        @Test
        @DisplayName("tenantId nul = refus immédiat, jamais un shard 0 silencieux")
        void tenantNulRefuse() {
            assertThatThrownBy(() -> ShardRouting.hash(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ShardRouting.shardOf(null, 16)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("shardCount < 1 = refus (un anneau vide n'existe pas)")
        void shardCountInferieurAUnRefuse() {
            UUID tenant = tenantAt(1);
            assertThatThrownBy(() -> ShardRouting.shardOf(tenant, 0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ShardRouting.shardOf(tenant, -8))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatCode(() -> ShardRouting.shardOf(tenant, 1)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Implémentation par défaut : une seule base, zéro régression")
    class SourceUnique {

        @Test
        @DisplayName("dataSourceFor renvoie TOUJOURS la base primaire, quel que soit le shard annoncé")
        void jamaisDeBasculeSilencieuse() {
            DataSource primary = Mockito.mock(DataSource.class);
            TenantDataSource routing = new SingleDatabaseTenantDataSource(primary, 16);

            for (int i = 0; i < 100; i++) {
                UUID tenant = tenantAt(i);
                assertThat(routing.dataSourceFor(tenant)).isSameAs(primary);
                assertThat(routing.shardFor(tenant)).isBetween(0, 15);
            }
        }

        @Test
        @DisplayName("shard-count par défaut (1) : shardFor = 0 pour tous — observable, inoffensif")
        void defautAucunDecoupage() {
            DataSource primary = Mockito.mock(DataSource.class);
            TenantDataSource routing = new SingleDatabaseTenantDataSource(primary, 1);

            assertThat(routing.shardFor(UUID.randomUUID())).isZero();
            assertThat(routing.dataSourceFor(UUID.randomUUID())).isSameAs(primary);
        }

        @Test
        @DisplayName("configuration invalide au démarrage = échec de bootstrap, pas un comportement ambigu")
        void configurationInvalideRefuseAuDemarrage() {
            DataSource primary = Mockito.mock(DataSource.class);
            assertThatThrownBy(() -> new SingleDatabaseTenantDataSource(primary, 0))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("app.scaling.shard-count");
        }
    }
}
