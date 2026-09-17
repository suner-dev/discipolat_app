package com.discipolat.modules.families.repository;

import com.discipolat.modules.families.domain.FamilyReception;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyReceptionRepository extends JpaRepository<FamilyReception, UUID> {

    List<FamilyReception> findByFamilyIdAndDeletedFalse(UUID familyId);

    List<FamilyReception> findBySoulIdAndDeletedFalse(UUID soulId);

    List<FamilyReception> findByFamilyIdAndReceptionDateBetween(UUID familyId, LocalDate from, LocalDate to);

    Page<FamilyReception> findByTenantIdAndDeletedFalseOrderByReceptionDateDesc(UUID tenantId, Pageable pageable);
}