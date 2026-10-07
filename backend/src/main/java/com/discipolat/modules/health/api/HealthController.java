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
    // Vues aplaties (V240) : les entités portent des @ManyToOne LAZY dont la
    // sérialisation directe était partielle ou dangereuse ; le contrat client
    // est désormais {personId, personName, ...}. Les entités restent le type
    // d'@RequestBody (le mobile envoie `person: {id}`) mais la réponse est
    // toujours une Map stable.

    @GetMapping("/patients")
    public ResponseEntity<PageResponse<Map<String, Object>>> getPatients(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        Page<Map<String, Object>> result = healthService.patientViews(tenantId, pageable);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/patients/{id}")
    public ResponseEntity<Map<String, Object>> getPatient(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.patientView(tenantId, id));
    }

    @PostMapping("/patients")
    public ResponseEntity<Map<String, Object>> createPatient(@RequestBody PatientRecord patient) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(healthService.patientView(tenantId,
                healthService.createPatientRecord(tenantId, actorId, patient).getId()));
    }

    @PutMapping("/patients/{id}")
    public ResponseEntity<Map<String, Object>> updatePatient(@PathVariable UUID id, @RequestBody PatientRecord updates) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        healthService.updatePatientRecord(tenantId, actorId, id, updates);
        return ResponseEntity.ok(healthService.patientView(tenantId, id));
    }

    // ========== MEDICAL CONSULTATIONS ==========

    @GetMapping("/consultations")
    public ResponseEntity<PageResponse<Map<String, Object>>> getConsultations(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("consultationDate").descending());
        Page<Map<String, Object>> result = healthService.consultationViews(tenantId, patientId, from, to, pageable);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/consultations/{id}")
    public ResponseEntity<Map<String, Object>> getConsultation(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.consultationView(tenantId, id));
    }

    @PostMapping("/consultations")
    public ResponseEntity<Map<String, Object>> createConsultation(@RequestBody MedicalConsultation consultation) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        MedicalConsultation saved = healthService.createConsultation(tenantId, actorId, consultation);
        return ResponseEntity.ok(healthService.consultationView(tenantId, saved.getId()));
    }

    // ========== PRESCRIPTIONS ==========

    @GetMapping("/prescriptions")
    public ResponseEntity<PageResponse<Map<String, Object>>> getPrescriptions(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID consultationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        Page<Map<String, Object>> result = healthService.prescriptionViews(tenantId, patientId, consultationId, pageable);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/prescriptions/{id}")
    public ResponseEntity<Map<String, Object>> getPrescription(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.prescriptionView(tenantId, id));
    }

    @PostMapping("/prescriptions")
    public ResponseEntity<Map<String, Object>> createPrescription(@RequestBody Prescription prescription) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        Prescription saved = healthService.createPrescription(tenantId, actorId, prescription);
        return ResponseEntity.ok(healthService.prescriptionView(tenantId, saved.getId()));
    }

    // ========== PHARMACY ==========
    // PharmacyItem n'a aucune relation LAZY : contrat brut conservé.

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
    public ResponseEntity<PageResponse<Map<String, Object>>> getPharmacyStock(
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("dateExpiration"));
        Page<Map<String, Object>> result = healthService.pharmacyStockViews(tenantId, itemId, status, pageable);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/pharmacy/stock/alerts/low")
    public ResponseEntity<List<Map<String, Object>>> getLowStockAlerts() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.lowStockAlertViews(tenantId));
    }

    @GetMapping("/pharmacy/stock/alerts/expiring")
    public ResponseEntity<List<Map<String, Object>>> getExpiringSoonAlerts(@RequestParam(defaultValue = "30") int days) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.expiringSoonAlertViews(tenantId, days));
    }

    @PostMapping("/pharmacy/movements")
    public ResponseEntity<Map<String, Object>> createMovement(@RequestBody PharmacyMovement movement) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(healthService.createMovementView(tenantId, actorId, movement));
    }

    // ========== HEALTH CAMPAIGNS ==========

    @GetMapping("/campaigns")
    public ResponseEntity<PageResponse<Map<String, Object>>> getCampaigns(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 20), Sort.by("startDate").descending());
        Page<Map<String, Object>> result = healthService.campaignViews(tenantId, status, pageable);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<Map<String, Object>> getCampaign(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.campaignView(tenantId, id));
    }

    @PostMapping("/campaigns")
    public ResponseEntity<Map<String, Object>> createCampaign(@RequestBody HealthCampaign campaign) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        HealthCampaign saved = healthService.createCampaign(tenantId, actorId, campaign);
        return ResponseEntity.ok(healthService.campaignView(tenantId, saved.getId()));
    }

    // ========== DASHBOARD / STATS ==========

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getDashboardStats(tenantId));
    }

    // ========== MEDICATIONS (V235) ==========
    // HealthMedication/HealthKit/HealthDuty/CampaignParticipant : aucune
    // relation LAZY, contrat brut conservé (pas de régression).

    @GetMapping("/medications")
    public ResponseEntity<List<HealthMedication>> getMedications() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getMedications(tenantId));
    }

    @GetMapping("/medications/{id}")
    public ResponseEntity<HealthMedication> getMedication(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getMedication(tenantId, id));
    }

    @PostMapping("/medications")
    public ResponseEntity<HealthMedication> createMedication(@RequestBody HealthMedication medication) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(healthService.createMedication(tenantId, actorId, medication));
    }

    // ========== KITS (V235) ==========

    @GetMapping("/kits")
    public ResponseEntity<List<HealthKit>> getKits() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getKits(tenantId));
    }

    @GetMapping("/kits/{id}")
    public ResponseEntity<HealthKit> getKit(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getKit(tenantId, id));
    }

    // ========== DUTIES (V235) ==========

    @GetMapping("/duties")
    public ResponseEntity<List<HealthDuty>> getDuties() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getDuties(tenantId));
    }

    // ========== CAMPAIGN PARTICIPANTS (V235) ==========

    @GetMapping("/campaigns/{campaignId}/participants")
    public ResponseEntity<List<CampaignParticipant>> getCampaignParticipants(@PathVariable UUID campaignId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getCampaignParticipants(tenantId, campaignId));
    }

    @PostMapping("/campaigns/{campaignId}/register")
    public ResponseEntity<CampaignParticipant> registerCampaignParticipant(@PathVariable UUID campaignId, @RequestBody Map<String, UUID> body) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        UUID userId = body.get("userId");
        return ResponseEntity.ok(healthService.registerCampaignParticipant(tenantId, actorId, campaignId, userId));
    }

    // ========== CONSULTATION PRESCRIPTIONS (V235) ==========

    @GetMapping("/consultations/{consultationId}/prescriptions")
    public ResponseEntity<List<Map<String, Object>>> getConsultationPrescriptions(@PathVariable UUID consultationId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.consultationPrescriptionViews(tenantId, consultationId));
    }

    // ========== PHARMACY STOCK DETAIL (V235) ==========

    @GetMapping("/pharmacy/stock/{id}")
    public ResponseEntity<Map<String, Object>> getPharmacyStockById(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.pharmacyStockView(tenantId, id));
    }

    // ========== PATIENTS BY CONDITION (V235) ==========

    @GetMapping("/patients/by-condition")
    public ResponseEntity<List<Map<String, Object>>> getPatientsByCondition(@RequestParam String condition) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.patientViewsByCondition(tenantId, condition));
    }

    // ========== MISES À JOUR APPELÉES PAR LE MOBILE (V235) ==========

    /**
     * {@code PUT /health/consultations/{id}} — appelé par
     * {@code HealthService.updateConsultation} du mobile, sans endpoint
     * correspondant côté serveur à l'origine.
     */
    @PutMapping("/consultations/{id}")
    public ResponseEntity<Map<String, Object>> updateConsultation(
            @PathVariable UUID id, @RequestBody MedicalConsultation updates) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        healthService.updateConsultation(tenantId, actorId, id, updates);
        return ResponseEntity.ok(healthService.consultationView(tenantId, id));
    }

    /**
     * {@code PUT /health/pharmacy/stock/{id}} — mise à jour d'un lot de
     * pharmacie consommée par le mobile.
     */
    @PutMapping("/pharmacy/stock/{id}")
    public ResponseEntity<Map<String, Object>> updatePharmacyStock(
            @PathVariable UUID id, @RequestBody PharmacyStock updates) {
        UUID tenantId = TenantContext.requireTenantId();
        healthService.updatePharmacyStock(tenantId, id, updates);
        return ResponseEntity.ok(healthService.pharmacyStockView(tenantId, id));
    }

    // ========== HEALTH REPORTS STATISTICS (V235) ==========

    @GetMapping("/reports/statistics")
    public ResponseEntity<Map<String, Object>> getHealthReportsStatistics() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getHealthReportsStatistics(tenantId));
    }
}
