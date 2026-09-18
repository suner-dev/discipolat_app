package com.discipolat.modules.families.repository;

import com.discipolat.modules.families.domain.FamilyActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FamilyActivityRepository extends JpaRepository<FamilyActivity, UUID> {

    List<FamilyActivity> findByFamilyIdOrderByActivityDateDesc(UUID familyId);

    List<FamilyActivity> findByFamilyIdAndActivityDateBetween(UUID familyId, java.time.LocalDate from, java.time.LocalDate to);

    Optional<FamilyActivity> findByReferenceId(UUID referenceId);

    Page<FamilyActivity> findByTenantIdOrderByActivityDateDesc(UUID tenantId, Pageable pageable);
}