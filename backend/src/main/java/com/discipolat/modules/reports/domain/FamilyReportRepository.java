package com.discipolat.modules.reports.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyReportRepository extends JpaRepository<FamilyReport, UUID> {
    Page<FamilyReport> findByFamilleId(UUID familleId, Pageable pageable);
    Page<FamilyReport> findByChefFamilleId(UUID chefFamilleId, Pageable pageable);
    Page<FamilyReport> findBySemaine(LocalDate semaine, Pageable pageable);
    List<FamilyReport> findByFamilleIdAndSemaine(UUID familleId, LocalDate semaine);
    List<FamilyReport> findByFamilleIdOrderBySemaineDesc(UUID familleId);
    List<FamilyReport> findByFamilleIdAndSemaineOrderByCreatedAtDesc(UUID familleId, LocalDate semaine);
    List<FamilyReport> findByFamilleIdInAndSemaine(List<UUID> familleIds, LocalDate semaine);
}
