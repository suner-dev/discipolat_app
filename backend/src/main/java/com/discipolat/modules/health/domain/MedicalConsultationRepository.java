package com.discipolat.modules.health.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicalConsultationRepository extends JpaRepository<MedicalConsultation, UUID> {
    List<MedicalConsultation> findByPatientIdAndTenantIdOrderByConsultationDateDesc(UUID patientId, UUID tenantId);
    Page<MedicalConsultation> findByPatientIdAndTenantIdOrderByConsultationDateDesc(UUID patientId, UUID tenantId, Pageable pageable);
    Page<MedicalConsultation> findByFamilyIdAndTenantId(UUID familyId, UUID tenantId, Pageable pageable);
    List<MedicalConsultation> findByPractitionerIdAndTenantId(UUID practitionerId, UUID tenantId);
    List<MedicalConsultation> findByConsultationDateBetweenAndTenantId(LocalDate start, LocalDate end, UUID tenantId);
    Page<MedicalConsultation> findByConsultationDateBetweenAndTenantId(LocalDate start, LocalDate end, UUID tenantId, Pageable pageable);
    Page<MedicalConsultation> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    long countByTenantIdAndConsultationDateBetween(UUID tenantId, LocalDate from, LocalDate to);
    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
