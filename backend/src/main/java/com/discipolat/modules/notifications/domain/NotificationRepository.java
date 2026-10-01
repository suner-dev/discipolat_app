package com.discipolat.modules.notifications.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByDestinataireIdOrderByCreatedAtDesc(UUID destinataireId, Pageable pageable);
    Page<Notification> findByDestinataireIdAndLuFalseOrderByCreatedAtDesc(UUID destinataireId, Pageable pageable);
    long countByDestinataireIdAndLuFalse(UUID destinataireId);

    /** Déduplication : une notification du même type pour la même entité et le même destinataire existe déjà. */
    boolean existsByDestinataireIdAndTypeAndEntiteReferenceIdAndEntiteReferenceType(
            UUID destinataireId, com.discipolat.common.enums.TypeNotification type,
            UUID entiteReferenceId, String entiteReferenceType);

    /**
     * PORT Develop1 (§G4.1 rappels de suivi) : déduplication dans le TEMPS — au
     * plus un rappel SUIVI_RAPPEL par visite et par destinataire pour une journée,
     * sans interdire les rappels des jours suivants sur la même visite.
     */
    boolean existsByDestinataireIdAndTypeAndEntiteReferenceIdAndCreatedAtAfter(
            UUID destinataireId, com.discipolat.common.enums.TypeNotification type,
            UUID entiteReferenceId, java.time.LocalDateTime after);

    @Modifying
    @Query("UPDATE Notification n SET n.lu = true, n.dateLecture = CURRENT_TIMESTAMP WHERE n.destinataireId = :userId AND n.lu = false")
    void markAllAsRead(@Param("userId") UUID userId);
}
