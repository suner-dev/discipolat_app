package com.discipolat.modules.health.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    List<Prescription> findByConsultationIdAndTenantId(UUID consultationId, UUID tenantId);
    List<Prescription> findByPatientIdAndTenantIdOrderByCreatedAtDesc(UUID patientId, UUID tenantId);
    Page<Prescription> findByPatientIdAndTenantId(UUID patientId, UUID tenantId, Pageable pageable);
    List<Prescription> findByPatientIdAndTenantIdAndStatus(UUID patientId, UUID tenantId, Prescription.PrescriptionStatus status);
    long countByPatientIdAndTenantId(UUID patientId, UUID tenantId);
}
