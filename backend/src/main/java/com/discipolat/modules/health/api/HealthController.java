package com.discipolat.modules.health.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.health.domain.*;
import com.discipolat.modules.health.service.HealthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health")
@PreAuthorize("hasAnyRole('HEALTH_STAFF', 'HEALTH_LEAD', 'ADMIN', 'PASTEUR')")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    // ========== PATIENT RECORDS ==========

    @GetMapping("/patients")
    public ResponseEntity<PageResponse<PatientRecord>> getPatients(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        Page<PatientRecord> result = healthService.getPatientRecords(tenantId, pageable);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/patients/{id}")
    public ResponseEntity<PatientRecord> getPatient(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getPatientRecord(tenantId, id));
    }

    @PostMapping("/patients")
    public ResponseEntity<PatientRecord> createPatient(@RequestBody PatientRecord patient) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(healthService.createPatientRecord(tenantId, actorId, patient));
    }

    @PutMapping("/patients/{id}")
    public ResponseEntity<PatientRecord> updatePatient(@PathVariable UUID id, @RequestBody PatientRecord updates) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(healthService.updatePatientRecord(tenantId, actorId, id, updates));
    }

    // ========== MEDICAL CONSULTATIONS ==========

    @GetMapping("/consultations")
    public ResponseEntity<PageResponse<MedicalConsultation>> getConsultations(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("consultationDate").descending());
        Page<MedicalConsultation> result = healthService.getConsultations(tenantId, patientId, from, to, pageable);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/consultations/{id}")
    public ResponseEntity<MedicalConsultation> getConsultation(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getConsultation(tenantId, id));
    }

    @PostMapping("/consultations")
    public ResponseEntity<MedicalConsultation> createConsultation(@RequestBody MedicalConsultation consultation) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(healthService.createConsultation(tenantId, actorId, consultation));
    }

    // ========== PRESCRIPTIONS ==========

    @GetMapping("/prescriptions")
    public ResponseEntity<PageResponse<Prescription>> getPrescriptions(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID consultationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        Page<Prescription> result = healthService.getPrescriptions(tenantId, patientId, consultationId, pageable);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/prescriptions/{id}")
    public ResponseEntity<Prescription> getPrescription(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getPrescription(tenantId, id));
    }

    @PostMapping("/prescriptions")
    public ResponseEntity<Prescription> createPrescription(@RequestBody Prescription prescription) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(healthService.createPrescription(tenantId, actorId, prescription));
    }

    // ========== PHARMACY ==========

    @GetMapping("/pharmacy/items")
    public ResponseEntity<PageResponse<PharmacyItem>> getPharmacyItems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categorie,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("nom"));
        Page<PharmacyItem> result = healthService.getPharmacyItems(tenantId, search, categorie, pageable);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/pharmacy/items/{id}")
    public ResponseEntity<PharmacyItem> getPharmacyItem(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getPharmacyItem(tenantId, id));
    }

    @PostMapping("/pharmacy/items")
    public ResponseEntity<PharmacyItem> createPharmacyItem(@RequestBody PharmacyItem item) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.createPharmacyItem(tenantId, item));
    }

    @PutMapping("/pharmacy/items/{id}")
    public ResponseEntity<PharmacyItem> updatePharmacyItem(@PathVariable UUID id, @RequestBody PharmacyItem item) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.updatePharmacyItem(tenantId, id, item));
    }

    @GetMapping("/pharmacy/stock")
    public ResponseEntity<PageResponse<PharmacyStock>> getPharmacyStock(
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("dateExpiration"));
        Page<PharmacyStock> result = healthService.getPharmacyStock(tenantId, itemId, status, pageable);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/pharmacy/stock/alerts/low")
    public ResponseEntity<List<PharmacyStock>> getLowStockAlerts() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getLowStockAlerts(tenantId));
    }

    @GetMapping("/pharmacy/stock/alerts/expiring")
    public ResponseEntity<List<PharmacyStock>> getExpiringSoonAlerts(@RequestParam(defaultValue = "30") int days) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getExpiringSoonAlerts(tenantId, days));
    }

    @PostMapping("/pharmacy/movements")
    public ResponseEntity<PharmacyMovement> createMovement(@RequestBody PharmacyMovement movement) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(healthService.createMovement(tenantId, actorId, movement));
    }

    // ========== HEALTH CAMPAIGNS ==========

    @GetMapping("/campaigns")
    public ResponseEntity<PageResponse<HealthCampaign>> getCampaigns(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 20), Sort.by("startDate").descending());
        Page<HealthCampaign> result = healthService.getCampaigns(tenantId, status, pageable);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<HealthCampaign> getCampaign(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getCampaign(tenantId, id));
    }

    @PostMapping("/campaigns")
    public ResponseEntity<HealthCampaign> createCampaign(@RequestBody HealthCampaign campaign) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(healthService.createCampaign(tenantId, actorId, campaign));
    }

    // ========== DASHBOARD / STATS ==========

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getDashboardStats(tenantId));
    }
}