package com.discipolat.modules.ussd.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository des sessions USSD.
 */
@Repository
public interface UssdSessionRepository extends JpaRepository<UssdSession, UUID> {

    /** Trouve une session active (non terminée) par son ID Africa's Talking. */
    Optional<UssdSession> findBySessionIdAndEndedFalse(String sessionId);

    /** Trouve toutes les sessions d'un numéro de téléphone. */
    List<UssdSession> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    /** Trouve toutes les sessions d'un tenant. */
    List<UssdSession> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    /** Termine les sessions inactives depuis un seuil donné. */
    @Modifying
    @Query("UPDATE UssdSession s SET s.ended = true WHERE s.lastActivity < :threshold AND s.ended = false")
    int endInactiveSessions(@Param("threshold") LocalDateTime threshold);

    /** Compte les sessions actives pour un tenant. */
    long countByTenantIdAndEndedFalse(String tenantId);
}
