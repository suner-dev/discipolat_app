package com.discipolat.modules.health.api;

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

    private final PatientRecordRepository patientRecordRepository;
    private final MedicalConsultationRepository medicalConsultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PharmacyItemRepository pharmacyItemRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final PharmacyMovementRepository pharmacyMovementRepository;
    private final HealthCampaignRepository healthCampaignRepository;
    private final HealthService healthService;

    public HealthController(PatientRecordRepository patientRecordRepository,
                            MedicalConsultationRepository medicalConsultationRepository,
                            PrescriptionRepository prescriptionRepository,
                            PharmacyItemRepository pharmacyItemRepository,
                            PharmacyStockRepository pharmacyStockRepository,
                            PharmacyMovementRepository pharmacyMovementRepository,
                            HealthCampaignRepository healthCampaignRepository,
                            HealthService healthService) {
        this.patientRecordRepository = patientRecordRepository;
        this.medicalConsultationRepository = medicalConsultationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.pharmacyItemRepository = pharmacyItemRepository;
        this.pharmacyStockRepository = pharmacyStockRepository;
        this.pharmacyMovementRepository = pharmacyMovementRepository;
        this.healthCampaignRepository = healthCampaignRepository;
        this.healthService = healthService;
    }

    // ========== PATIENT RECORDS ==========

    @GetMapping("/patients")
    public ResponseEntity<Page<PatientRecord>> getPatients(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        // TODO: Add search filter
        return ResponseEntity.ok(patientRecordRepository.findByTenantIdAndDeletedFalse(tenantId, pageable));
    }

    @GetMapping("/patients/{id}")
    public ResponseEntity<PatientRecord> getPatient(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        PatientRecord patient = patientRecordRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Patient non trouvé"));
        return ResponseEntity.ok(patient);
    }

    @PostMapping("/patients")
    public ResponseEntity<PatientRecord> createPatient(@RequestBody PatientRecord patient) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        patient.setTenantId(tenantId);
        return ResponseEntity.ok(patientRecordRepository.save(patient));
    }

    @PutMapping("/patients/{id}")
    public ResponseEntity<PatientRecord> updatePatient(@PathVariable UUID id, @RequestBody PatientRecord updates) {
        UUID tenantId = TenantContext.requireTenantId();
        PatientRecord patient = patientRecordRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Patient non trouvé"));
        // Apply updates
        if (updates.getGroupeSanguin() != null) patient.setGroupeSanguin(updates.getGroupeSanguin());
        if (updates.getAllergies() != null) patient.setAllergies(updates.getAllergies());
        if (updates.getAntecedents() != null) patient.setAntecedents(updates.getAntecedents());
        if (updates.getMedecinTraitant() != null) patient.setMedecinTraitant(updates.getMedecinTraitant());
        if (updates.getMedecinTel() != null) patient.setMedecinTel(updates.getMedecinTel());
        if (updates.getMesures() != null) patient.setMesures(updates.getMesures());
        if (updates.getNotesSensibles() != null) patient.setNotesSensibles(updates.getNotesSensibles());
        if (updates.getNumeroAssurance() != null) patient.setNumeroAssurance(updates.getNumeroAssurance());
        if (updates.getPoidsKg() != null) patient.setPoidsKg(updates.getPoidsKg());
        if (updates.getTailleCm() != null) patient.setTailleCm(updates.getTailleCm());
        if (updates.getTensionArterielle() != null) patient.setTensionArterielle(updates.getTensionArterielle());
        if (updates.getGlycemie() != null) patient.setGlycemie(updates.getGlycemie());
        if (updates.getPackYear() != null) patient.setPackYear(updates.getPackYear());
        if (updates.getAbouchement() != null) patient.setAbouchement(updates.getAbouchement());
        if (updates.getConfidentialityLevel() != null) patient.setConfidentialityLevel(updates.getConfidentialityLevel());
        return ResponseEntity.ok(patientRecordRepository.save(patient));
    }

    // ========== MEDICAL CONSULTATIONS ==========

    @GetMapping("/consultations")
    public ResponseEntity<Page<MedicalConsultation>> getConsultations(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("consultationDate").descending());
        if (patientId != null) {
            return ResponseEntity.ok(medicalConsultationRepository.findByTenantIdAndPatientIdOrderByConsultationDateDesc(tenantId, patientId, pageable));
        }
        if (from != null && to != null) {
            return ResponseEntity.ok(medicalConsultationRepository.findByTenantIdAndConsultationDateBetween(tenantId, from, to, pageable));
        }
        return ResponseEntity.ok(medicalConsultationRepository.findByTenantIdAndDeletedFalse(tenantId, pageable));
    }

    @GetMapping("/consultations/{id}")
    public ResponseEntity<MedicalConsultation> getConsultation(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(medicalConsultationRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Consultation non trouvée")));
    }

    @PostMapping("/consultations")
    public ResponseEntity<MedicalConsultation> createConsultation(@RequestBody MedicalConsultation consultation) {
        UUID tenantId = TenantContext.requireTenantId();
        consultation.setTenantId(tenantId);
        return ResponseEntity.ok(medicalConsultationRepository.save(consultation));
    }

    // ========== PRESCRIPTIONS ==========

    @GetMapping("/prescriptions")
    public ResponseEntity<Page<Prescription>> getPrescriptions(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID consultationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        if (patientId != null) {
            return ResponseEntity.ok(prescriptionRepository.findByTenantIdAndPatientIdOrderByCreatedAtDesc(tenantId, patientId, pageable));
        }
        if (consultationId != null) {
            return ResponseEntity.ok(prescriptionRepository.findByConsultationId(consultationId, pageable));
        }
        return ResponseEntity.ok(prescriptionRepository.findByTenantIdAndDeletedFalse(tenantId, pageable));
    }

    @PostMapping("/prescriptions")
    public ResponseEntity<Prescription> createPrescription(@RequestBody Prescription prescription) {
        UUID tenantId = TenantContext.requireTenantId();
        prescription.setTenantId(tenantId);
        return ResponseEntity.ok(prescriptionRepository.save(prescription));
    }

    // ========== PHARMACY ==========

    @GetMapping("/pharmacy/items")
    public ResponseEntity<Page<PharmacyItem>> getPharmacyItems(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("nom"));
        return ResponseEntity.ok(pharmacyItemRepository.findByTenantIdAndDeletedFalse(tenantId, pageable));
    }

    @PostMapping("/pharmacy/items")
    public ResponseEntity<PharmacyItem> createPharmacyItem(@RequestBody PharmacyItem item) {
        UUID tenantId = TenantContext.requireTenantId();
        item.setTenantId(tenantId);
        return ResponseEntity.ok(pharmacyItemRepository.save(item));
    }

    @GetMapping("/pharmacy/stock")
    public ResponseEntity<Page<PharmacyStock>> getPharmacyStock(
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("dateExpiration"));
        if (itemId != null) {
            return ResponseEntity.ok(pharmacyStockRepository.findByTenantIdAndPharmacyItemIdAndDeletedFalse(tenantId, itemId, pageable));
        }
        if (status != null) {
            return ResponseEntity.ok(pharmacyStockRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageable));
        }
        return ResponseEntity.ok(pharmacyStockRepository.findByTenantIdAndDeletedFalse(tenantId, pageable));
    }

    @GetMapping("/pharmacy/stock/alerts")
    public ResponseEntity<List<PharmacyStock>> getStockAlerts() {
        UUID tenantId = TenantContext.requireTenantId();
        // Stock faible + expirant bientôt
        List<PharmacyStock> lowStock = pharmacyStockRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, "STOCK_FAIBLE");
        List<PharmacyStock> expiring = pharmacyStockRepository.findByTenantIdAndDeletedFalse(tenantId, PageRequest.of(0, 100))
                .getContent().stream()
                .filter(s -> s.getDateExpiration().isBefore(LocalDate.now().plusDays(30)))
                .toList();
        // Combine and return
        return ResponseEntity.ok(lowStock);
    }

    @PostMapping("/pharmacy/movements")
    public ResponseEntity<PharmacyMovement> createMovement(@RequestBody PharmacyMovement movement) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        movement.setTenantId(tenantId);
        movement.setResponsibleId(actorId);
        return ResponseEntity.ok(pharmacyMovementRepository.save(movement));
    }

    // ========== HEALTH CAMPAIGNS ==========

    @GetMapping("/campaigns")
    public ResponseEntity<Page<HealthCampaign>> getCampaigns(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 20), Sort.by("startDate").descending());
        if (status != null) {
            return ResponseEntity.ok(healthCampaignRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageable));
        }
        return ResponseEntity.ok(healthCampaignRepository.findByTenantIdAndDeletedFalse(tenantId, pageable));
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<HealthCampaign> getCampaign(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthCampaignRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Campagne non trouvée")));
    }

    @PostMapping("/campaigns")
    public ResponseEntity<HealthCampaign> createCampaign(@RequestBody HealthCampaign campaign) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        campaign.setTenantId(tenantId);
        campaign.setResponsibleId(actorId);
        return ResponseEntity.ok(healthCampaignRepository.save(campaign));
    }

    // ========== DASHBOARD / STATS ==========

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(healthService.getDashboardStats(tenantId));
    }
}