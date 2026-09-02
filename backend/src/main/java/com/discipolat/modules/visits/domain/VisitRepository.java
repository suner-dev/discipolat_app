package com.discipolat.modules.visits.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Recherche paginée des visites avec filtres optionnels (statut + texte). */
    @Query("SELECT v FROM Visit v WHERE (:statut IS NULL OR v.statut = :statut) AND (" +
            ":search IS NULL OR LOWER(COALESCE(v.motif, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(COALESCE(v.objectif, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(COALESCE(v.compteRendu, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Visit> search(@Param("statut") Visit.StatutVisite statut,
                       @Param("search") String search,
                       Pageable pageable);
}
