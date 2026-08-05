package com.discipolat.modules.visits.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID> {
    List<Visit> findBySoulIdOrderByDatePrevueDesc(UUID soulId);
    List<Visit> findByVisiteurIdOrderByDatePrevueDesc(UUID visiteurId);
    List<Visit> findByStatutOrderByDatePrevueAsc(Visit.StatutVisite statut);
    long countByVisiteurIdAndStatut(UUID visiteurId, Visit.StatutVisite statut);
    long countByVisiteurIdAndDateRealiseeBetween(UUID visiteurId, LocalDate from, LocalDate to);
    long countByDatePrevueBetween(LocalDate from, LocalDate to);
    long countByStatutAndDatePrevueBefore(Visit.StatutVisite statut, LocalDate date);
    long countByVisiteurId(UUID visiteurId);
}
