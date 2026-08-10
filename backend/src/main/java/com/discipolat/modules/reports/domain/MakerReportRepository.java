package com.discipolat.modules.reports.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MakerReportRepository extends JpaRepository<MakerReport, UUID> {
    Page<MakerReport> findByFaiseurId(UUID faiseurId, Pageable pageable);
    Page<MakerReport> findByAmeId(UUID ameId, Pageable pageable);
    Page<MakerReport> findBySemaine(LocalDate semaine, Pageable pageable);
    List<MakerReport> findByFaiseurIdAndSemaine(UUID faiseurId, LocalDate semaine);
    List<MakerReport> findByFaiseurIdAndSemaineAndSoumisTrue(UUID faiseurId, LocalDate semaine);
    Optional<MakerReport> findByFaiseurIdAndAmeIdAndSemaine(UUID faiseurId, UUID ameId, LocalDate semaine);
    List<MakerReport> findByAmeIdAndSemaine(UUID ameId, LocalDate semaine);
    List<MakerReport> findAllByAmeIdAndSoumisTrueOrderBySemaineDesc(UUID ameId);
    long countByFaiseurIdAndSemaineAndSoumisTrue(UUID faiseurId, LocalDate semaine);
    long countByFaiseurIdAndSoumisTrue(UUID faiseurId);
    List<MakerReport> findByFaiseurIdInAndSemaine(List<UUID> faiseurIds, LocalDate semaine);
}
