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

    /** Recherche paginée des visites avec filtres optionnels (statut + texte).
     *  CAST obligatoire et porté jusqu'à CHAQUE usage du paramètre : sur
     *  PostgreSQL, un paramètre null rendu dans « '%' || ? || '%' » reste
     *  typé bytea par inférence (le CAST de garde ne typant que sa propre
     *  occurrence, chaque `:search` étant un marqueur JDBC distinct)
     *  → 500 « function lower(bytea) does not exist ». */
    @Query("SELECT v FROM Visit v WHERE (CAST(:statut AS string) IS NULL OR v.statut = :statut) AND (" +
            "CAST(:search AS string) IS NULL OR LOWER(COALESCE(v.motif, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "OR LOWER(COALESCE(v.objectif, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "OR LOWER(COALESCE(v.compteRendu, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Visit> search(@Param("statut") Visit.StatutVisite statut,
                       @Param("search") String search,
                       Pageable pageable);
}
