package com.discipolat.modules.health.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.health.domain.*;
import com.discipolat.modules.people.domain.Person;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final HealthMedicationRepository healthMedicationRepository;
    private final HealthKitRepository healthKitRepository;
    private final HealthDutyRepository healthDutyRepository;
    private final CampaignParticipantRepository campaignParticipantRepository;

    // @RequiredArgsConstructor génère le constructeur ci-dessous à partir des champs final.

    public PatientRecord createPatientRecord(UUID tenantId, UUID actorId, PatientRecord record) {
        record.setTenantId(tenantId);
        return patientRecordRepository.save(record);
    }

    public PatientRecord getPatientRecord(UUID tenantId, UUID recordId) {
        return patientRecordRepository.findById(recordId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Patient record not found"));
    }

    public Page<PatientRecord> getPatientRecords(UUID tenantId, Pageable pageable) {
        return patientRecordRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
    }

    public PatientRecord updatePatientRecord(UUID tenantId, UUID actorId, UUID recordId, PatientRecord updates) {
        PatientRecord record = getPatientRecord(tenantId, recordId);
        if (updates.getGroupeSanguin() != null) record.setGroupeSanguin(updates.getGroupeSanguin());
        if (updates.getAllergies() != null) record.setAllergies(updates.getAllergies());
        if (updates.getAntecedents() != null) record.setAntecedents(updates.getAntecedents());
        if (updates.getMedecinTraitant() != null) record.setMedecinTraitant(updates.getMedecinTraitant());
        if (updates.getMedecinTel() != null) record.setMedecinTel(updates.getMedecinTel());
        if (updates.getMesures() != null) record.setMesures(updates.getMesures());
        if (updates.getNotesSensibles() != null) record.setNotesSensibles(updates.getNotesSensibles());
        if (updates.getNumeroAssurance() != null) record.setNumeroAssurance(updates.getNumeroAssurance());
        if (updates.getPoidsKg() != null) record.setPoidsKg(updates.getPoidsKg());
        if (updates.getTailleCm() != null) record.setTailleCm(updates.getTailleCm());
        if (updates.getTensionArterielle() != null) record.setTensionArterielle(updates.getTensionArterielle());
        if (updates.getGlycemie() != null) record.setGlycemie(updates.getGlycemie());
        if (updates.getPackYear() != null) record.setPackYear(updates.getPackYear());
        if (updates.getAbouchement() != null) record.setAbouchement(updates.getAbouchement());
        if (updates.getConfidentialityLevel() != null) record.setConfidentialityLevel(updates.getConfidentialityLevel());
        return patientRecordRepository.save(record);
    }

    // ========== MEDICAL CONSULTATIONS ==========

    /**
     * Mise à jour d'une consultation (appelée par le mobile).
     *
     * <p>Application par champs présents uniquement : une absence de clé
     * conserve la valeur enregistrée, au lieu de l'effacer.
     */
    public MedicalConsultation updateConsultation(UUID tenantId, UUID actorId,
                                                   UUID consultationId, MedicalConsultation updates) {
        MedicalConsultation consultation = getConsultation(tenantId, consultationId);
        if (updates.getConsultationDate() != null) consultation.setConsultationDate(updates.getConsultationDate());
        if (updates.getTypeConsultation() != null) consultation.setTypeConsultation(updates.getTypeConsultation());
        if (updates.getMotif() != null) consultation.setMotif(updates.getMotif());
        if (updates.getConstantes() != null) consultation.setConstantes(updates.getConstantes());
        if (updates.getDiagnostic() != null) consultation.setDiagnostic(updates.getDiagnostic());
        if (updates.getTraitement() != null) consultation.setTraitement(updates.getTraitement());
        if (updates.getResultat() != null) consultation.setResultat(updates.getResultat());
        if (updates.getOrientation() != null) consultation.setOrientation(updates.getOrientation());
        if (updates.getStatus() != null) consultation.setStatus(updates.getStatus());
        return medicalConsultationRepository.save(consultation);
    }

    public MedicalConsultation createConsultation(UUID tenantId, UUID actorId, MedicalConsultation consultation) {
        consultation.setTenantId(tenantId);
        return medicalConsultationRepository.save(consultation);
    }

    public MedicalConsultation getConsultation(UUID tenantId, UUID consultationId) {
        return medicalConsultationRepository.findById(consultationId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found"));
    }

    public Page<MedicalConsultation> getConsultations(UUID tenantId, UUID patientId, LocalDate from, LocalDate to, Pageable pageable) {
        if (patientId != null) {
            return medicalConsultationRepository.findByPatientIdAndTenantIdOrderByConsultationDateDesc(patientId, tenantId, pageable);
        }
        if (from != null && to != null) {
            return medicalConsultationRepository.findByConsultationDateBetweenAndTenantId(from, to, tenantId, pageable);
        }
        return medicalConsultationRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
    }

    // ========== PRESCRIPTIONS ==========

    public Prescription createPrescription(UUID tenantId, UUID actorId, Prescription prescription) {
        prescription.setTenantId(tenantId);
        return prescriptionRepository.save(prescription);
    }

    public Prescription getPrescription(UUID tenantId, UUID prescriptionId) {
        return prescriptionRepository.findById(prescriptionId)
                .filter(p -> p.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));
    }

    public Page<Prescription> getPrescriptions(UUID tenantId, UUID patientId, UUID consultationId, Pageable pageable) {
        if (consultationId != null) {
            return prescriptionRepository.findByConsultationIdAndTenantId(consultationId, tenantId, pageable);
        }
        if (patientId != null) {
            return prescriptionRepository.findByPatientIdAndTenantId(patientId, tenantId, pageable);
        }
        // Fallback - not ideal but works
        return prescriptionRepository.findByPatientIdAndTenantId(UUID.randomUUID(), tenantId, pageable); // Will return empty
    }

    // ========== PHARMACY ==========

    public PharmacyItem createPharmacyItem(UUID tenantId, PharmacyItem item) {
        item.setTenantId(tenantId);
        return pharmacyItemRepository.save(item);
    }

    public Page<PharmacyItem> getPharmacyItems(UUID tenantId, String search, String categorie, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return pharmacyItemRepository.searchByNom(tenantId, search, pageable);
        }
        if (categorie != null && !categorie.isBlank()) {
            return pharmacyItemRepository.findByCategorieAndTenantIdAndDeletedFalse(categorie, tenantId, pageable);
        }
        return pharmacyItemRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
    }

    public PharmacyItem getPharmacyItem(UUID tenantId, UUID itemId) {
        return pharmacyItemRepository.findById(itemId)
                .filter(i -> i.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy item not found"));
    }

    public PharmacyItem updatePharmacyItem(UUID tenantId, UUID itemId, PharmacyItem updates) {
        PharmacyItem item = getPharmacyItem(tenantId, itemId);
        if (updates.getNom() != null) item.setNom(updates.getNom());
        if (updates.getDescription() != null) item.setDescription(updates.getDescription());
        if (updates.getCategorie() != null) item.setCategorie(updates.getCategorie());
        if (updates.getUnite() != null) item.setUnite(updates.getUnite());
        if (updates.getFournisseur() != null) item.setFournisseur(updates.getFournisseur());
        if (updates.getPrixAchat() != null) item.setPrixAchat(updates.getPrixAchat());
        return pharmacyItemRepository.save(item);
    }

    public Page<PharmacyStock> getPharmacyStock(UUID tenantId, UUID itemId, String status, Pageable pageable) {
        if (itemId != null) {
            return pharmacyStockRepository.findByPharmacyItemIdAndTenantId(itemId, tenantId, pageable);
        }
        if (status != null && !status.isBlank()) {
            return pharmacyStockRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageable);
        }
        return pharmacyStockRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
    }

    public List<PharmacyStock> getLowStockAlerts(UUID tenantId) {
        return pharmacyStockRepository.findLowStock(tenantId);
    }

    public List<PharmacyStock> getExpiringSoonAlerts(UUID tenantId, int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        return pharmacyStockRepository.findExpiringBefore(tenantId, threshold);
    }

    public PharmacyMovement createMovement(UUID tenantId, UUID actorId, PharmacyMovement movement) {
        movement.setTenantId(tenantId);
        User responsible = userRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + actorId));
        movement.setResponsible(responsible);
        return pharmacyMovementRepository.save(movement);
    }

    // ========== HEALTH CAMPAIGNS ==========

    public HealthCampaign createCampaign(UUID tenantId, UUID actorId, HealthCampaign campaign) {
        campaign.setTenantId(tenantId);
        User responsible = userRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + actorId));
        campaign.setResponsible(responsible);
        return healthCampaignRepository.save(campaign);
    }

    public HealthCampaign getCampaign(UUID tenantId, UUID campaignId) {
        return healthCampaignRepository.findById(campaignId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
    }

    public Page<HealthCampaign> getCampaigns(UUID tenantId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return healthCampaignRepository.findByStatusAndTenantIdAndDeletedFalse(tenantId, HealthCampaign.CampaignStatus.valueOf(status.toUpperCase()), pageable);
        }
        return healthCampaignRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
    }

    // ========== DASHBOARD / STATS ==========

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(UUID tenantId) {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalPatients = patientRecordRepository.countByTenantIdAndDeletedFalse(tenantId);
        stats.put("totalPatients", totalPatients);

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        long consultationsThisMonth = medicalConsultationRepository.countByTenantIdAndConsultationDateBetween(
                tenantId, startOfMonth, LocalDate.now());
        stats.put("consultationsThisMonth", consultationsThisMonth);

        long activePrescriptions = prescriptionRepository.countByPatientIdAndTenantId(UUID.randomUUID(), tenantId); // placeholder
        stats.put("activePrescriptions", activePrescriptions);

        long totalItems = pharmacyItemRepository.findByTenantIdAndDeletedFalse(tenantId).size();
        long lowStock = pharmacyStockRepository.findLowStock(tenantId).size();
        long expiring = pharmacyStockRepository.findExpiringBefore(tenantId, LocalDate.now().plusDays(30)).size();
        stats.put("pharmacyItems", totalItems);
        stats.put("lowStockAlerts", lowStock);
        stats.put("expiringSoon", expiring);

        long activeCampaigns = healthCampaignRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, HealthCampaign.CampaignStatus.IN_PROGRESS);
        long plannedCampaigns = healthCampaignRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, HealthCampaign.CampaignStatus.PLANNED);
        stats.put("activeCampaigns", activeCampaigns);
        stats.put("plannedCampaigns", plannedCampaigns);

        return stats;
    }

    // ========== MEDICATIONS (V235) ==========

    public List<HealthMedication> getMedications(UUID tenantId) {
        return healthMedicationRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId);
    }

    public HealthMedication getMedication(UUID tenantId, UUID id) {
        return healthMedicationRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("HealthMedication", "id", id.toString()));
    }

    public HealthMedication createMedication(UUID tenantId, UUID actorId, HealthMedication medication) {
        medication.setTenantId(tenantId);
        return healthMedicationRepository.save(medication);
    }

    // ========== KITS (V235) ==========

    public List<HealthKit> getKits(UUID tenantId) {
        return healthKitRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId);
    }

    public HealthKit getKit(UUID tenantId, UUID id) {
        return healthKitRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("HealthKit", "id", id.toString()));
    }

    // ========== DUTIES (V235) ==========

    public List<HealthDuty> getDuties(UUID tenantId) {
        return healthDutyRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId);
    }

    // ========== CAMPAIGN PARTICIPANTS (V235) ==========

    public List<CampaignParticipant> getCampaignParticipants(UUID tenantId, UUID campaignId) {
        return campaignParticipantRepository.findByTenantIdAndCampaignId(tenantId, campaignId);
    }

    public CampaignParticipant registerCampaignParticipant(UUID tenantId, UUID actorId, UUID campaignId, UUID userId) {
        CampaignParticipant cp = CampaignParticipant.builder()
                .tenantId(tenantId)
                .campaignId(campaignId)
                .userId(userId)
                .status(CampaignParticipant.Status.REGISTERED)
                .build();
        return campaignParticipantRepository.save(cp);
    }

    // ========== PHARMACY STOCK DETAIL (V235) ==========

    public PharmacyStock getPharmacyStockById(UUID tenantId, UUID stockId) {
        return pharmacyStockRepository.findByTenantIdAndId(tenantId, stockId)
                .orElseThrow(() -> new EntityNotFoundException("PharmacyStock", "id", stockId.toString()));
    }

    /**
     * Mise à jour d'un lot de pharmacie (appelée par le mobile).
     *
     * <p>La quantité ne peut pas devenir négative : une valeur négative
     * produirait un stock incohérent avec les alertes de seuil.
     */
    public PharmacyStock updatePharmacyStock(UUID tenantId, UUID stockId, PharmacyStock updates) {
        PharmacyStock stock = getPharmacyStockById(tenantId, stockId);
        if (updates.getQuantite() != null) {
            if (updates.getQuantite() < 0) {
                throw new IllegalArgumentException("La quantité ne peut pas être négative");
            }
            stock.setQuantite(updates.getQuantite());
        }
        if (updates.getSeuilAlerte() != null) stock.setSeuilAlerte(updates.getSeuilAlerte());
        if (updates.getPrixUnitaire() != null) stock.setPrixUnitaire(updates.getPrixUnitaire());
        if (updates.getDateExpiration() != null) stock.setDateExpiration(updates.getDateExpiration());
        if (updates.getStatus() != null) stock.setStatus(updates.getStatus());
        return pharmacyStockRepository.save(stock);
    }

    // ========== PATIENTS BY CONDITION (V235) ==========

    public List<PatientRecord> getPatientsByCondition(UUID tenantId, String condition) {
        String c = condition == null ? "" : condition.toLowerCase();
        return patientRecordRepository.findByTenantIdAndDeletedFalse(tenantId).stream()
                .filter(p -> c.isBlank()
                        || (p.getAntecedents() != null && p.getAntecedents().toLowerCase().contains(c))
                        || (p.getAllergies() != null && p.getAllergies().toLowerCase().contains(c))
                        || (p.getNotesSensibles() != null && p.getNotesSensibles().toLowerCase().contains(c)))
                .toList();
    }

    // ========== HEALTH REPORTS STATISTICS (V235) ==========

    public Map<String, Object> getHealthReportsStatistics(UUID tenantId) {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("medications", healthMedicationRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId).size());
        stats.put("kits", healthKitRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId).size());
        stats.put("duties", healthDutyRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId).size());
        stats.put("patients", patientRecordRepository.countByTenantIdAndDeletedFalse(tenantId));
        stats.put("consultations", medicalConsultationRepository.countByTenantIdAndDeletedFalse(tenantId));
        stats.put("prescriptions", prescriptionRepository.countByTenantIdAndDeletedFalse(tenantId));
        return stats;
    }
}