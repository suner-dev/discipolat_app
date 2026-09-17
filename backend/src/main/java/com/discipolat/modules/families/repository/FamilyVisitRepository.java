package com.discipolat.modules.families.repository;

import com.discipolat.modules.families.domain.FamilyVisit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyVisitRepository extends JpaRepository<FamilyVisit, UUID> {

    List<FamilyVisit> findByFamilyIdAndDeletedFalse(UUID familyId);

    List<FamilyVisit> findBySoulIdAndDeletedFalse(UUID soulId);

    List<FamilyVisit> findByFaiseurIdAndDeletedFalse(UUID faiseurId);

    List<FamilyVisit> findByFamilyIdAndVisitDateBetweenAndDeletedFalse(UUID familyId, LocalDate from, LocalDate to);

    @Query("SELECT fv FROM FamilyVisit fv WHERE fv.tenantId = :tenantId AND fv.deleted = false AND fv.visitDate BETWEEN :from AND :to ORDER BY fv.visitDate ASC")
    List<FamilyVisit> findByTenantIdAndVisitDateBetween(@Param("tenantId") UUID tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    Page<FamilyVisit> findByTenantIdAndDeletedFalseOrderByVisitDateDesc(UUID tenantId, Pageable pageable);
}