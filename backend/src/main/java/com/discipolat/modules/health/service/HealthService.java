package com.discipolat.modules.health.service;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.health.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class HealthService {

    private final PatientRecordRepository patientRecordRepository;
    private final MedicalConsultationRepository medicalConsultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PharmacyItemRepository pharmacyItemRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final PharmacyMovementRepository pharmacyMovementRepository;
    private final HealthCampaignRepository healthCampaignRepository;

    public Map<String, Object> getDashboardStats(UUID tenantId) {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Patients
        long totalPatients = patientRecordRepository.countByTenantIdAndDeletedFalse(tenantId);
        stats.put("totalPatients", totalPatients);

        // Consultations this month
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        long consultationsThisMonth = medicalConsultationRepository.countByTenantIdAndConsultationDateBetween(
                tenantId, startOfMonth, LocalDate.now());
        stats.put("consultationsThisMonth", consultationsThisMonth);

        // Active prescriptions
        long activePrescriptions = prescriptionRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, "ACTIVE");
        stats.put("activePrescriptions", activePrescriptions);

        // Pharmacy stock
        long totalItems = pharmacyItemRepository.countByTenantIdAndDeletedFalse(tenantId);
        long lowStock = pharmacyStockRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, "STOCK_FAIBLE");
        long expiring = pharmacyStockRepository.findByTenantIdAndDeletedFalse(tenantId, org.springframework.data.domain.PageRequest.of(0, 1000))
                .getContent().stream()
                .filter(s -> s.getDateExpiration().isBefore(LocalDate.now().plusDays(30)))
                .count();
        stats.put("pharmacyItems", totalItems);
        stats.put("lowStockAlerts", lowStock);
        stats.put("expiringSoon", expiring);

        // Campaigns
        long activeCampaigns = healthCampaignRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, "IN_PROGRESS");
        long plannedCampaigns = healthCampaignRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, "PLANNED");
        stats.put("activeCampaigns", activeCampaigns);
        stats.put("plannedCampaigns", plannedCampaigns);

        return stats;
    }

    // Helper methods for alerts
    @Transactional(readOnly = true)
    public List<PharmacyStock> getLowStockAlerts(UUID tenantId) {
        return pharmacyStockRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, "STOCK_FAIBLE");
    }

    @Transactional(readOnly = true)
    public List<PharmacyStock> getExpiringSoonAlerts(UUID tenantId, int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        return pharmacyStockRepository.findByTenantIdAndDeletedFalse(tenantId, org.springframework.data.domain.PageRequest.of(0, 1000))
                .getContent().stream()
                .filter(s -> s.getDateExpiration().isBefore(threshold) && !s.getDateExpiration().isBefore(LocalDate.now()))
                .toList();
    }
}