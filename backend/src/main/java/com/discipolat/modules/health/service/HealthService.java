package com.discipolat.modules.health.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
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
    private final FamilyRepository familyRepository;
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
        // Sans filtre, listes le tenant courant (l'ancien fallback UUID aléatoire renvoyait
        // systématiquement vide — illisible côté clients).
        return prescriptionRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
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

    /** Mouvement créé, rendu en vue aplatie (même transaction : le `responsible` est frais). */
    public Map<String, Object> createMovementView(UUID tenantId, UUID actorId, PharmacyMovement movement) {
        PharmacyMovement saved = createMovement(tenantId, actorId, movement);
        ViewNames names = loadNames(tenantId, null, null, null, null, List.of(saved), null);
        return pharmacyMovementView(saved, names);
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

        long activePrescriptions = prescriptionRepository.countByTenantIdAndStatus(tenantId, Prescription.PrescriptionStatus.ACTIVE);
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

    // ==========================================================================
    // VUES APLATIES (V240) — contrat de sérialisation pour le web et le mobile.
    //
    // Les entités health portent des @ManyToOne LAZY (patient, practitioner,
    // pharmacyItem, responsible, family…) dont la sérialisation Jackson directe
    // est soit partielle (champs absents quand le proxy n'est pas initialisé),
    // soit récursive. Ces vues Map réduisent chaque relation à
    // {xxxId, xxxName} — comme taskView dans TaskService — et excluent tenantId
    // et deleted (interne au serveur).
    //
    // Les noms sont résolus par chargement groupé (findAllById en une requête
    // par page) : sans cela, chaque vue déclencherait une requête par relation
    // (N+1). Les UUID sont rendus en String pour un contrat JSON explicite.
    // ==========================================================================

    /** Noms User et Family résolus pour une page d'entités ; filtrés en Java sur le tenant courant. */
    private record ViewNames(Map<UUID, String> users, Map<UUID, String> families) {}

    private ViewNames loadNames(UUID tenantId,
                                 List<PatientRecord> patients,
                                 List<MedicalConsultation> consultations,
                                 List<Prescription> prescriptions,
                                 List<PharmacyStock> stocks,
                                 List<PharmacyMovement> movements,
                                 List<HealthCampaign> campaigns) {
        return loadNames(tenantId, patients, consultations, prescriptions, stocks, movements, campaigns, null);
    }

    private ViewNames loadNames(UUID tenantId,
                                 List<PatientRecord> patients,
                                 List<MedicalConsultation> consultations,
                                 List<Prescription> prescriptions,
                                 List<PharmacyStock> stocks,
                                 List<PharmacyMovement> movements,
                                 List<HealthCampaign> campaigns,
                                 Collection<UUID> extraUserIds) {
        Set<UUID> userIds = new HashSet<>();
        Set<UUID> familyIds = new HashSet<>();
        if (extraUserIds != null) userIds.addAll(extraUserIds);
        if (patients != null) for (PatientRecord p : patients) {
            if (p.getPerson() != null) userIds.add(p.getPerson().getId());
            if (p.getFamily() != null) familyIds.add(p.getFamily().getId());
        }
        if (consultations != null) for (MedicalConsultation c : consultations) {
            if (c.getPatient() != null) userIds.add(c.getPatient().getId());
            if (c.getPractitioner() != null) userIds.add(c.getPractitioner().getId());
            if (c.getFamily() != null) familyIds.add(c.getFamily().getId());
        }
        if (prescriptions != null) for (Prescription pr : prescriptions) {
            if (pr.getPatient() != null) userIds.add(pr.getPatient().getId());
        }
        if (movements != null) for (PharmacyMovement mv : movements) {
            if (mv.getPatient() != null) userIds.add(mv.getPatient().getId());
            if (mv.getResponsible() != null) userIds.add(mv.getResponsible().getId());
        }
        if (campaigns != null) for (HealthCampaign hc : campaigns) {
            if (hc.getResponsible() != null) userIds.add(hc.getResponsible().getId());
            if (hc.getFamily() != null) familyIds.add(hc.getFamily().getId());
        }

        Map<UUID, String> users = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userRepository.findAllById(userIds)) {
                if (tenantId.equals(u.getTenantId())) {
                    users.put(u.getId(), userName(u));
                }
            }
        }
        Map<UUID, String> families = new HashMap<>();
        if (!familyIds.isEmpty()) {
            for (Family f : familyRepository.findAllById(familyIds)) {
                if (tenantId.equals(f.getTenantId())) {
                    families.put(f.getId(), f.getNom());
                }
            }
        }
        return new ViewNames(users, families);
    }

    private static String userName(User u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName().trim();
        String last = u.getLastName() == null ? "" : u.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "Utilisateur " + u.getId() : full;
    }

    /** Id User de la vue d'ordonnance : patient propre, sinon patient de la consultation. */
    private static UUID prescriptionPatientId(Prescription pr, Map<UUID, UUID> consultationPatientIds) {
        if (pr.getPatient() != null) return pr.getPatient().getId();
        if (pr.getConsultation() != null) return consultationPatientIds.get(pr.getConsultation().getId());
        return null;
    }

    private Map<String, Object> patientView(PatientRecord p, ViewNames names) {
        Map<String, Object> m = new LinkedHashMap<>();
        UUID personId = p.getPerson() != null ? p.getPerson().getId() : null;
        UUID familyId = p.getFamily() != null ? p.getFamily().getId() : null;
        m.put("id", sid(p.getId()));
        m.put("personId", sid(personId));
        m.put("personName", personId == null ? null : names.users().get(personId));
        m.put("familyId", sid(familyId));
        m.put("familyName", familyId == null ? null : names.families().get(familyId));
        m.put("groupeSanguin", p.getGroupeSanguin());
        m.put("allergies", p.getAllergies());
        m.put("antecedents", p.getAntecedents());
        m.put("medecinTraitant", p.getMedecinTraitant());
        m.put("medecinTel", p.getMedecinTel());
        m.put("mesures", p.getMesures());
        m.put("notesSensibles", p.getNotesSensibles());
        m.put("numeroAssurance", p.getNumeroAssurance());
        m.put("poidsKg", p.getPoidsKg());
        m.put("tailleCm", p.getTailleCm());
        m.put("tensionArterielle", p.getTensionArterielle());
        m.put("glycemie", p.getGlycemie());
        m.put("packYear", p.getPackYear());
        m.put("abouchement", p.getAbouchement());
        m.put("confidentialityLevel", p.getConfidentialityLevel() != null ? p.getConfidentialityLevel().name() : null);
        m.put("createdAt", dtoa(p.getCreatedAt()));
        m.put("updatedAt", dtoa(p.getUpdatedAt()));
        return m;
    }

    private Map<String, Object> consultationView(MedicalConsultation c, ViewNames names) {
        Map<String, Object> m = new LinkedHashMap<>();
        UUID patientId = c.getPatient() != null ? c.getPatient().getId() : null;
        UUID practitionerId = c.getPractitioner() != null ? c.getPractitioner().getId() : null;
        UUID familyId = c.getFamily() != null ? c.getFamily().getId() : null;
        m.put("id", sid(c.getId()));
        m.put("patientId", sid(patientId));
        m.put("patientName", patientId == null ? null : names.users().get(patientId));
        m.put("practitionerId", sid(practitionerId));
        m.put("practitionerName", practitionerId == null ? null : names.users().get(practitionerId));
        m.put("familyId", sid(familyId));
        m.put("familyName", familyId == null ? null : names.families().get(familyId));
        m.put("consultationDate", c.getConsultationDate() != null ? c.getConsultationDate().toString() : null);
        m.put("typeConsultation", c.getTypeConsultation());
        m.put("motif", c.getMotif());
        m.put("constantes", c.getConstantes());
        m.put("diagnostic", c.getDiagnostic());
        m.put("traitement", c.getTraitement());
        m.put("resultat", c.getResultat());
        m.put("orientation", c.getOrientation());
        m.put("status", c.getStatus() != null ? c.getStatus().name() : null);
        m.put("createdAt", dtoa(c.getCreatedAt()));
        m.put("updatedAt", dtoa(c.getUpdatedAt()));
        return m;
    }

    private Map<String, Object> prescriptionView(Prescription pr, ViewNames names, Map<UUID, UUID> consultationPatientIds) {
        Map<String, Object> m = new LinkedHashMap<>();
        UUID consultationId = pr.getConsultation() != null ? pr.getConsultation().getId() : null;
        UUID patientId = prescriptionPatientId(pr, consultationPatientIds);
        m.put("id", sid(pr.getId()));
        m.put("consultationId", sid(consultationId));
        m.put("patientId", sid(patientId));
        m.put("patientName", patientId == null ? null : names.users().get(patientId));
        m.put("medicament", pr.getMedicament());
        m.put("dosage", pr.getDosage());
        m.put("posologie", pr.getPosologie());
        m.put("duree", pr.getDuree());
        m.put("status", pr.getStatus() != null ? pr.getStatus().name() : null);
        m.put("notes", pr.getNotes());
        m.put("createdAt", dtoa(pr.getCreatedAt()));
        m.put("updatedAt", dtoa(pr.getUpdatedAt()));
        return m;
    }

    private Map<String, Object> pharmacyStockView(PharmacyStock s) {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDate exp = s.getDateExpiration();
        m.put("id", sid(s.getId()));
        m.put("itemId", s.getPharmacyItem() != null ? sid(s.getPharmacyItem().getId()) : null);
        m.put("itemName", s.getPharmacyItem() != null ? s.getPharmacyItem().getNom() : null);
        m.put("lotNumber", s.getLotNumber());
        m.put("quantite", s.getQuantite());
        m.put("seuilAlerte", s.getSeuilAlerte());
        m.put("dateExpiration", exp != null ? exp.toString() : null);
        m.put("prixUnitaire", s.getPrixUnitaire());
        m.put("status", s.getStatus() != null ? s.getStatus().name() : null);
        m.put("isExpired", exp != null && exp.isBefore(LocalDate.now()));
        m.put("createdAt", dtoa(s.getCreatedAt()));
        m.put("updatedAt", dtoa(s.getUpdatedAt()));
        return m;
    }

    private Map<String, Object> pharmacyMovementView(PharmacyMovement mv, ViewNames names) {
        Map<String, Object> m = new LinkedHashMap<>();
        UUID patientId = mv.getPatient() != null ? mv.getPatient().getId() : null;
        UUID responsibleId = mv.getResponsible() != null ? mv.getResponsible().getId() : null;
        m.put("id", sid(mv.getId()));
        m.put("stockId", mv.getPharmacyStock() != null ? sid(mv.getPharmacyStock().getId()) : null);
        m.put("prescriptionId", mv.getPrescription() != null ? sid(mv.getPrescription().getId()) : null);
        m.put("patientId", sid(patientId));
        m.put("patientName", patientId == null ? null : names.users().get(patientId));
        m.put("movementType", mv.getMovementType());
        m.put("quantite", mv.getQuantite());
        m.put("motif", mv.getMotif());
        m.put("responsibleId", sid(responsibleId));
        m.put("responsibleName", responsibleId == null ? null : names.users().get(responsibleId));
        m.put("preuveReception", mv.getPreuveReception());
        m.put("createdAt", dtoa(mv.getCreatedAt()));
        m.put("updatedAt", dtoa(mv.getUpdatedAt()));
        return m;
    }

    private Map<String, Object> campaignView(HealthCampaign hc, ViewNames names) {
        Map<String, Object> m = new LinkedHashMap<>();
        UUID responsibleId = hc.getResponsible() != null ? hc.getResponsible().getId() : null;
        UUID familyId = hc.getFamily() != null ? hc.getFamily().getId() : null;
        m.put("id", sid(hc.getId()));
        m.put("title", hc.getTitle());
        m.put("description", hc.getDescription());
        m.put("campaignType", hc.getCampaignType() != null ? hc.getCampaignType().name() : null);
        m.put("startDate", hc.getStartDate() != null ? hc.getStartDate().toString() : null);
        m.put("endDate", hc.getEndDate() != null ? hc.getEndDate().toString() : null);
        m.put("lieu", hc.getLieu());
        m.put("responsibleId", sid(responsibleId));
        m.put("responsibleName", responsibleId == null ? null : names.users().get(responsibleId));
        m.put("familyId", sid(familyId));
        m.put("familyName", familyId == null ? null : names.families().get(familyId));
        m.put("objectif", hc.getObjectif());
        m.put("cibles", hc.getCibles());
        m.put("status", hc.getStatus() != null ? hc.getStatus().name() : null);
        m.put("participantsCount", campaignParticipantRepository.findByTenantIdAndCampaignId(hc.getTenantId(), hc.getId()).size());
        m.put("createdAt", dtoa(hc.getCreatedAt()));
        m.put("updatedAt", dtoa(hc.getUpdatedAt()));
        return m;
    }

    private static String sid(UUID u) {
        return u == null ? null : u.toString();
    }

    private static String dtoa(LocalDateTime dt) {
        return dt == null ? null : dt.toString();
    }

    // ---------- Endpoints vus (controller) ----------

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> patientViews(UUID tenantId, Pageable pageable) {
        Page<PatientRecord> src = patientRecordRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        ViewNames names = loadNames(tenantId, src.getContent(), null, null, null, null, null);
        return src.map(p -> patientView(p, names));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> patientView(UUID tenantId, UUID recordId) {
        PatientRecord p = getPatientRecord(tenantId, recordId);
        return patientView(p, loadNames(tenantId, List.of(p), null, null, null, null, null));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> patientViewsByCondition(UUID tenantId, String condition) {
        List<PatientRecord> src = getPatientsByCondition(tenantId, condition);
        ViewNames names = loadNames(tenantId, src, null, null, null, null, null);
        return src.stream().map(p -> patientView(p, names)).toList();
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> consultationViews(UUID tenantId, UUID patientId, LocalDate from, LocalDate to, Pageable pageable) {
        Page<MedicalConsultation> src = getConsultations(tenantId, patientId, from, to, pageable);
        ViewNames names = loadNames(tenantId, null, src.getContent(), null, null, null, null);
        return src.map(c -> consultationView(c, names));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> consultationView(UUID tenantId, UUID consultationId) {
        MedicalConsultation c = getConsultation(tenantId, consultationId);
        return consultationView(c, loadNames(tenantId, null, List.of(c), null, null, null, null));
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> prescriptionViews(UUID tenantId, UUID patientId, UUID consultationId, Pageable pageable) {
        Page<Prescription> src = getPrescriptions(tenantId, patientId, consultationId, pageable);
        Map<UUID, UUID> consultationPatientIds = consultationPatientIds(src.getContent());
        ViewNames names = loadNames(tenantId, null, null, src.getContent(), null, null, null,
                consultationPatientIds.values());
        return src.map(pr -> prescriptionView(pr, names, consultationPatientIds));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> consultationPrescriptionViews(UUID tenantId, UUID consultationId) {
        List<Prescription> src = prescriptionRepository.findByConsultationIdAndTenantId(consultationId, tenantId);
        Map<UUID, UUID> consultationPatientIds = consultationPatientIds(src);
        ViewNames names = loadNames(tenantId, null, null, src, null, null, null,
                consultationPatientIds.values());
        return src.stream().map(pr -> prescriptionView(pr, names, consultationPatientIds)).toList();
    }

    /** Table de jointure {consultationId → patientId} pour les ordonnances sans patient visible. */
    private Map<UUID, UUID> consultationPatientIds(Collection<Prescription> prescriptions) {
        Set<UUID> ids = new HashSet<>();
        for (Prescription pr : prescriptions) {
            if (pr.getPatient() == null && pr.getConsultation() != null) {
                ids.add(pr.getConsultation().getId());
            }
        }
        Map<UUID, UUID> out = new HashMap<>();
        if (!ids.isEmpty()) {
            for (MedicalConsultation c : medicalConsultationRepository.findAllById(ids)) {
                if (c.getPatient() != null) out.put(c.getId(), c.getPatient().getId());
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> prescriptionView(UUID tenantId, UUID prescriptionId) {
        Prescription pr = getPrescription(tenantId, prescriptionId);
        Map<UUID, UUID> consultationPatientIds = consultationPatientIds(List.of(pr));
        ViewNames names = loadNames(tenantId, null, null, List.of(pr), null, null, null,
                consultationPatientIds.values());
        return prescriptionView(pr, names, consultationPatientIds);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> pharmacyStockViews(UUID tenantId, UUID itemId, String status, Pageable pageable) {
        Page<PharmacyStock> src = getPharmacyStock(tenantId, itemId, status, pageable);
        return src.map(this::pharmacyStockView);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> pharmacyStockView(UUID tenantId, UUID stockId) {
        return pharmacyStockView(getPharmacyStockById(tenantId, stockId));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> lowStockAlertViews(UUID tenantId) {
        return getLowStockAlerts(tenantId).stream().map(this::pharmacyStockView).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> expiringSoonAlertViews(UUID tenantId, int days) {
        return getExpiringSoonAlerts(tenantId, days).stream().map(this::pharmacyStockView).toList();
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> campaignViews(UUID tenantId, String status, Pageable pageable) {
        Page<HealthCampaign> src = getCampaigns(tenantId, status, pageable);
        ViewNames names = loadNames(tenantId, null, null, null, null, null, src.getContent());
        return src.map(hc -> campaignView(hc, names));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> campaignView(UUID tenantId, UUID campaignId) {
        HealthCampaign hc = getCampaign(tenantId, campaignId);
        return campaignView(hc, loadNames(tenantId, null, null, null, null, null, List.of(hc)));
    }
}