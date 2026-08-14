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
    Page<Event> findByFamilleIdAndDeletedFalse(UUID familleId, Pageable pageable);
    Page<Event> findByDepartmentIdAndDeletedFalse(UUID departmentId, Pageable pageable);
    List<Event> findByDepartmentIdAndTitreContainingIgnoreCaseAndDeletedFalse(UUID departmentId, String titre);
    Page<Event> findByOrganisateurIdAndDeletedFalse(UUID organisateurId, Pageable pageable);
    Page<Event> findByTypeEvenementAndDeletedFalse(String typeEvenement, Pageable pageable);
    Page<Event> findByStatutAndDeletedFalse(String statut, Pageable pageable);
    Page<Event> findByDateDebutBetweenAndDeletedFalse(LocalDateTime start, LocalDateTime end, Pageable pageable);
    List<Event> findByFamilleIdAndStatutAndDeletedFalse(UUID familleId, String statut);
    List<Event> findByDateDebutBetweenAndDeletedFalse(LocalDateTime start, LocalDateTime end);
    long countByFamilleIdAndDeletedFalse(UUID familleId);
    long countByDepartmentIdAndDeletedFalse(UUID departmentId);
}
