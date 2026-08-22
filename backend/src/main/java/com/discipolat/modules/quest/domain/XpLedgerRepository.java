package com.discipolat.modules.quest.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface XpLedgerRepository extends JpaRepository<XpLedger, UUID> {

    List<XpLedger> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndActionAndCreatedAtAfter(UUID userId, XpLedger.QuestAction action, LocalDateTime after);

    @Query("SELECT l.userId, SUM(l.points) FROM XpLedger l " +
           "WHERE l.tenantId = :tenantId GROUP BY l.userId ORDER BY SUM(l.points) DESC")
    List<Object[]> sumPointsByUser(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(l.points), 0) FROM XpLedger l WHERE l.userId = :userId")
    long totalXpForUser(@Param("userId") UUID userId);
}
