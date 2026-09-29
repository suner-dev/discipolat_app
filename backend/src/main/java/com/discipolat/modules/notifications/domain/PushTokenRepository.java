package com.discipolat.modules.notifications.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * P0 — Accès aux tokens d'appareil pour les notifications push.
 */
@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, UUID> {

    /** Tokens actifs d'un utilisateur, du plus récent au plus ancien. */
    List<PushToken> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<PushToken> findByToken(String token);

    boolean existsByToken(String token);

    /**
     * Élague les tokens que FCM a déclarés invalides. Sans cette opération,
     * un token révoqué est réessayé à chaque événement publié.
     *
     * <p>Requête en masse : elle ignore le filtre Hibernate de tenant, d'où
     * le {@code tenant_id} explicite — un token n'est élagué que dans son
     * propre tenant.</p>
     */
    @Modifying
    @Query("DELETE FROM PushToken t WHERE t.tenantId = :tenantId AND t.token IN :tokens")
    int deleteInvalidTokens(@Param("tenantId") UUID tenantId, @Param("tokens") Collection<String> tokens);
}
