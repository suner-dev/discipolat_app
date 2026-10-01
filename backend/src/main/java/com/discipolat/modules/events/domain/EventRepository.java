package com.discipolat.modules.events.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    // Suppression logique « deleted_at » du modèle vivant (V158/V203) : les
    // dérivés « …DeletedFalse » de la table héritée deviennent « …DeletedAtIsNull ».
    /** P3 #113 — Événements à venir (calendrier personnel du membre). */
    Page<Event> findByTenantIdAndDeletedAtIsNullAndDateDebutAfterOrderByDateDebutAsc(
            UUID tenantId, java.time.LocalDateTime from, org.springframework.data.domain.Pageable pageable);
    Page<Event> findByFamilleIdAndDeletedAtIsNull(UUID familleId, Pageable pageable);
    Page<Event> findByDepartmentIdAndDeletedAtIsNull(UUID departmentId, Pageable pageable);
    List<Event> findByDepartmentIdAndDeletedAtIsNull(UUID departmentId);
    List<Event> findByDepartmentIdAndTitreContainingIgnoreCaseAndDeletedAtIsNull(UUID departmentId, String titre);
    Page<Event> findByOrganisateurIdAndDeletedAtIsNull(UUID organisateurId, Pageable pageable);
    Page<Event> findByTypeEvenementAndDeletedAtIsNull(String typeEvenement, Pageable pageable);
    Page<Event> findByStatutAndDeletedAtIsNull(String statut, Pageable pageable);
    Page<Event> findByDateDebutBetweenAndDeletedAtIsNull(LocalDateTime start, LocalDateTime end, Pageable pageable);
    List<Event> findByFamilleIdAndStatutAndDeletedAtIsNull(UUID familleId, String statut);
    List<Event> findByDepartmentIdInAndDeletedAtIsNull(List<UUID> departmentIds);
    List<Event> findByDateDebutBetweenAndDeletedAtIsNull(LocalDateTime start, LocalDateTime end);
    List<Event> findByDepartmentIdIsNotNullAndDeletedAtIsNullAndDateDebutBetween(LocalDateTime start, LocalDateTime end);
    /**
     * G5.9 — Portail basse connexion (#PLANNING / #PRESENCE) : fenêtre
     * d'événements d'UN SEUL tenant. La variante sans `TenantId` ne doit jamais
     * être appelée depuis un canal anonyme.
     */
    List<Event> findByTenantIdAndDeletedAtIsNullAndDateDebutBetween(
            UUID tenantId, LocalDateTime start, LocalDateTime end);
    long countByFamilleIdAndDeletedAtIsNull(UUID familleId);

    /**
     * Constat M3 — comptage des événements NON clos d'un tenant.
     *
     * <p>Colonnes vérifiées sur l'entité {@code Event} : {@code tenant_id},
     * {@code statut} (String, défaut {@code PLANIFIE}), {@code deleted_at}
     * (suppression logique, null = actif). Les statuts de clôture réellement
     * utilisés par l'application sont {@code TERMINE} et {@code ANNULE} : seuls
     * eux sont exclus du quota, un événement planifié compte même s'il est passé.
     */
    long countByTenantIdAndStatutNotInAndDeletedAtIsNull(UUID tenantId, java.util.Collection<String> closedStatuts);
    long countByDepartmentIdAndDeletedAtIsNull(UUID departmentId);

    /** Sources du Page Builder : événements à venir (non supprimés). */
    long countByDeletedAtIsNullAndDateDebutAfter(LocalDateTime dateDebut);

    List<Event> findTop10ByDeletedAtIsNullAndDateDebutAfterOrderByDateDebutAsc(LocalDateTime dateDebut);
    List<Event> findAllByFamilleIdAndDeletedAtIsNull(UUID familleId);
}
