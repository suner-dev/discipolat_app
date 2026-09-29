package com.discipolat.common.scaling;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * A3 (M7) — Fonction de routage de shard : {@code shard = hash(tenantId) % N}.
 *
 * <p>Le hash doit être STABLE dans le temps (un tenant ne change jamais de
 * shard entre deux JVM, deux versions, deux langages) : c'est pourquoi on
 * hache les 16 octets bruts de l'UUID avec un mixeur déterministe
 * (splitmix64 finalizer, opérations entières), et surtout PAS
 * {@code String.hashCode()} ni {@code UUID.hashCode()} dont la stabilité
 * inter-implémentations n'est pas un contrat. {@code Math.floorMod} garantit
 * un résultat dans [0, N) même si le hash est négatif.</p>
 *
 * <p>Conséquence architecturale assumée : la répartition est en anneaux
 * fermes — agrandir N (re-sharding) requiert une migration planifiée des
 * tenants, documentée dans docs/SCALING.md (Niveau 3), pas un re-hash sauvage.</p>
 */
public final class ShardRouting {

    private ShardRouting() {
    }

    /** Hash 64 bits déterministe des 16 octets de l'UUID (splitmix64 finalizer). */
    public static long hash(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId requis pour le routage de shard");
        }
        ByteBuffer buffer = ByteBuffer.allocate(16)
                .putLong(tenantId.getMostSignificantBits())
                .putLong(tenantId.getLeastSignificantBits());
        // Mélange non linéaire des deux halves : évite que la structure
        // temporelle des UUIDv4 (bits fixes) se propage dans le shard.
        long mixed = buffer.getLong(0) * 0x9E3779B97F4A7C15L
                + buffer.getLong(8) * 0xC2B2AE3D27D4EB4FL;
        mixed ^= mixed >>> 33;
        mixed *= 0xFF51AFD7ED558CCDL;
        mixed ^= mixed >>> 33;
        return mixed;
    }

    /** Shard d'un tenant pour un anneau de N shards (N ≥ 1). */
    public static int shardOf(UUID tenantId, int shardCount) {
        if (shardCount < 1) {
            throw new IllegalArgumentException("shardCount doit être >= 1, reçu : " + shardCount);
        }
        return (int) Math.floorMod(hash(tenantId), (long) shardCount);
    }
}
