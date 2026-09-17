package com.discipolat.modules.health.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRecordRepository extends JpaRepository<PatientRecord, UUID> {
    Optional<PatientRecord> findByPersonIdAndTenantId(UUID personId, UUID tenantId);
    Page<PatientRecord> findByFamilyIdAndTenantId(UUID familyId, UUID tenantId, Pageable pageable);
    List<PatientRecord> findByFamilyIdAndTenantId(UUID familyId, UUID tenantId);
    List<PatientRecord> findByTenantIdAndDeletedFalse(UUID tenantId);
    Page<PatientRecord> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
