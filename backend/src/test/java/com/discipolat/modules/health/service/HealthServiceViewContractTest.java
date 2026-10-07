package com.discipolat.modules.health.service;

import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.health.domain.*;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * Contrat des vues aplaties V240 : les clients (web et mobile) consistent sur
 * {xxxId, xxxName} en String, jamais sur des entités LAZY partiellement
 * sérialisées. Le test verrouille trois choses :
 * <ol>
 *   <li>la forme de la vue (clés, UUID en String, absence de tenantId/deleted) ;</li>
 *   <li>le chargement groupé des noms (une seule findAllById par page, jamais N) ;</li>
 *   <li>l'isolation tenant (un User d'un autre tenant ne fournit jamais son nom).</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class HealthServiceViewContractTest {

    @Mock private PatientRecordRepository patientRecordRepository;
    @Mock private MedicalConsultationRepository medicalConsultationRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private PharmacyItemRepository pharmacyItemRepository;
    @Mock private PharmacyStockRepository pharmacyStockRepository;
    @Mock private PharmacyMovementRepository pharmacyMovementRepository;
    @Mock private HealthCampaignRepository healthCampaignRepository;
    @Mock private PersonRepository personRepository;
    @Mock private UserRepository userRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private HealthMedicationRepository healthMedicationRepository;
    @Mock private HealthKitRepository healthKitRepository;
    @Mock private HealthDutyRepository healthDutyRepository;
    @Mock private CampaignParticipantRepository campaignParticipantRepository;

    @InjectMocks private HealthService healthService;

    private final UUID tenant = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 20);

    private User user(UUID id, UUID tenantId, String first, String last) {
        return User.builder().id(id).tenantId(tenantId).firstName(first).lastName(last).build();
    }

    // ========== PATIENTS ==========

    @Test
    void patientViews_flattensPersonAndFamily_andNeverLeaksTenantInternals() {
        UUID personId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        User person = user(personId, tenant, "Grace", "Kabila");
        Family family = Family.builder().id(familyId).tenantId(tenant).nom("Kabila").build();
        PatientRecord record = PatientRecord.builder()
                .id(UUID.randomUUID()).tenantId(tenant)
                .person(person).family(family)
                .allergies("Pénicilline")
                .confidentialityLevel(PatientRecord.ConfidentialityLevel.STRICT)
                .createdAt(LocalDateTime.now()).build();
        when(patientRecordRepository.findByTenantIdAndDeletedFalse(tenant, pageable))
                .thenReturn(new PageImpl<>(List.of(record)));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(person));
        when(familyRepository.findAllById(anyCollection())).thenReturn(List.of(family));

        Page<Map<String, Object>> views = healthService.patientViews(tenant, pageable);

        Map<String, Object> v = views.getContent().get(0);
        assertEquals(personId.toString(), v.get("personId"));
        assertEquals("Grace Kabila", v.get("personName"));
        assertEquals(familyId.toString(), v.get("familyId"));
        assertEquals("Kabila", v.get("familyName"));
        assertEquals("Pénicilline", v.get("allergies"));
        assertEquals("STRICT", v.get("confidentialityLevel"));
        // Champs internes au serveur : absents du contrat client.
        assertFalse(v.containsKey("tenantId"));
        assertFalse(v.containsKey("deleted"));
        assertFalse(v.containsKey("person"));
        assertFalse(v.containsKey("family"));
    }

    @Test
    void patientViews_batchLoadsNames_withOneQueryPerType() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        PatientRecord r1 = PatientRecord.builder().id(UUID.randomUUID()).tenantId(tenant)
                .person(user(p1, tenant, "A", "B")).createdAt(LocalDateTime.now()).build();
        PatientRecord r2 = PatientRecord.builder().id(UUID.randomUUID()).tenantId(tenant)
                .person(user(p2, tenant, "C", "D")).createdAt(LocalDateTime.now()).build();
        when(patientRecordRepository.findByTenantIdAndDeletedFalse(tenant, pageable))
                .thenReturn(new PageImpl<>(List.of(r1, r2)));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(
                user(p1, tenant, "A", "B"), user(p2, tenant, "C", "D")));

        Page<Map<String, Object>> views = healthService.patientViews(tenant, pageable);

        assertEquals("A B", views.getContent().get(0).get("personName"));
        assertEquals("C D", views.getContent().get(1).get("personName"));
        // Preuve anti-N+1 : une seule findAllById groupé pour toute la page.
        verify(userRepository, times(1)).findAllById(anyCollection());
    }

    @Test
    void patientViews_ignoresForeignTenantUserNames() {
        UUID otherTenant = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        PatientRecord record = PatientRecord.builder().id(UUID.randomUUID()).tenantId(tenant)
                .person(user(personId, otherTenant, "X", "Y")).createdAt(LocalDateTime.now()).build();
        when(patientRecordRepository.findByTenantIdAndDeletedFalse(tenant, pageable))
                .thenReturn(new PageImpl<>(List.of(record)));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(
                user(personId, otherTenant, "X", "Y")));

        Map<String, Object> v = healthService.patientViews(tenant, pageable).getContent().get(0);

        // Isolation : le nom d'un compte hors tenant ne doit jamais être exposé.
        assertNull(v.get("personName"));
        assertEquals(personId.toString(), v.get("personId"));
    }

    // ========== CONSULTATIONS ==========

    @Test
    void consultationViews_flattensPatientAndPractitioner() {
        UUID patientId = UUID.randomUUID();
        UUID practitionerId = UUID.randomUUID();
        MedicalConsultation c = MedicalConsultation.builder()
                .id(UUID.randomUUID()).tenantId(tenant)
                .patient(user(patientId, tenant, "Pierre", "Mputu"))
                .practitioner(user(practitionerId, tenant, "Jean", "Kalala"))
                .consultationDate(LocalDate.of(2026, 10, 1))
                .typeConsultation("CONSULTATION")
                .status(MedicalConsultation.ConsultationStatus.COMPLETED)
                .createdAt(LocalDateTime.now()).build();
        when(medicalConsultationRepository.findByTenantIdAndDeletedFalse(tenant, pageable))
                .thenReturn(new PageImpl<>(List.of(c)));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(
                user(patientId, tenant, "Pierre", "Mputu"),
                user(practitionerId, tenant, "Jean", "Kalala")));

        Map<String, Object> v = healthService
                .consultationViews(tenant, null, null, null, pageable).getContent().get(0);

        assertEquals(patientId.toString(), v.get("patientId"));
        assertEquals("Pierre Mputu", v.get("patientName"));
        assertEquals(practitionerId.toString(), v.get("practitionerId"));
        assertEquals("Jean Kalala", v.get("practitionerName"));
        assertEquals("COMPLETED", v.get("status"));
        assertEquals("2026-10-01", v.get("consultationDate"));
        assertFalse(v.containsKey("patient"));
        assertFalse(v.containsKey("practitioner"));
    }

    // ========== PRESCRIPTIONS ==========

    @Test
    void prescriptionView_fallsBackOnConsultationPatient_whenOwnPatientMissing() {
        UUID consultationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        MedicalConsultation consultation = MedicalConsultation.builder()
                .id(consultationId).tenantId(tenant)
                .patient(user(patientId, tenant, "Anne", "Tshibangu")).build();
        Prescription pr = Prescription.builder()
                .id(UUID.randomUUID()).tenantId(tenant)
                .consultation(consultation).patient(null)
                .medicament("Paracétamol").status(Prescription.PrescriptionStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).build();
        when(prescriptionRepository.findByConsultationIdAndTenantId(consultationId, tenant))
                .thenReturn(List.of(pr));
        when(medicalConsultationRepository.findAllById(anyCollection())).thenReturn(List.of(consultation));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(
                user(patientId, tenant, "Anne", "Tshibangu")));

        List<Map<String, Object>> views =
                healthService.consultationPrescriptionViews(tenant, consultationId);

        Map<String, Object> v = views.get(0);
        assertEquals(consultationId.toString(), v.get("consultationId"));
        assertEquals(patientId.toString(), v.get("patientId"));
        assertEquals("Anne Tshibangu", v.get("patientName"));
        assertEquals("ACTIVE", v.get("status"));
    }

    // ========== STOCK / ALERTES ==========

    @Test
    void pharmacyStockView_exposesItemName_andExpiredFlag() {
        UUID itemId = UUID.randomUUID();
        PharmacyItem item = PharmacyItem.builder().id(itemId).tenantId(tenant).nom("Amoxicilline").build();
        PharmacyStock stock = PharmacyStock.builder()
                .id(UUID.randomUUID()).tenantId(tenant).pharmacyItem(item)
                .lotNumber("LOT-1").quantite(5).seuilAlerte(10)
                .dateExpiration(LocalDate.now().minusDays(1))
                .status(PharmacyStock.StockStatus.EXPIRÉ)
                .createdAt(LocalDateTime.now()).build();
        when(pharmacyStockRepository.findByTenantIdAndDeletedFalse(eq(tenant), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stock)));

        Map<String, Object> v = healthService
                .pharmacyStockViews(tenant, null, null, pageable).getContent().get(0);

        assertEquals(itemId.toString(), v.get("itemId"));
        assertEquals("Amoxicilline", v.get("itemName"));
        assertEquals(Boolean.TRUE, v.get("isExpired"));
        assertFalse(v.containsKey("pharmacyItem"));
    }

    // ========== DASHBOARD : placeholder corrigé ==========

    @Test
    void dashboardStats_countsActivePrescriptions_forTenant_notRandomPatient() {
        when(patientRecordRepository.countByTenantIdAndDeletedFalse(tenant)).thenReturn(3L);
        when(medicalConsultationRepository.countByTenantIdAndConsultationDateBetween(eq(tenant), any(), any()))
                .thenReturn(2L);
        when(prescriptionRepository.countByTenantIdAndStatus(tenant, Prescription.PrescriptionStatus.ACTIVE))
                .thenReturn(7L);
        when(pharmacyItemRepository.findByTenantIdAndDeletedFalse(tenant)).thenReturn(List.of());
        when(pharmacyStockRepository.findLowStock(tenant)).thenReturn(List.of());
        when(pharmacyStockRepository.findExpiringBefore(eq(tenant), any(LocalDate.class))).thenReturn(List.of());
        when(healthCampaignRepository.countByTenantIdAndStatusAndDeletedFalse(eq(tenant), any()))
                .thenReturn(1L);

        Map<String, Object> stats = healthService.getDashboardStats(tenant);

        assertEquals(7L, stats.get("activePrescriptions"));
        // L'ancien comptage sur patient aléatoire est proscrit :
        verify(prescriptionRepository, never()).countByPatientIdAndTenantId(any(UUID.class), any(UUID.class));
    }

    // ========== CAMPAGNES ==========

    @Test
    void campaignView_flattensResponsible_andCountsParticipants() {
        UUID campaignId = UUID.randomUUID();
        UUID responsibleId = UUID.randomUUID();
        HealthCampaign hc = HealthCampaign.builder()
                .id(campaignId).tenantId(tenant).title("Vaccination rougeole")
                .campaignType(HealthCampaign.CampaignType.VACCINATION)
                .startDate(LocalDate.of(2026, 11, 1))
                .lieu("Campus")
                .responsible(user(responsibleId, tenant, "Sarah", "Mbala"))
                .status(HealthCampaign.CampaignStatus.PLANNED)
                .createdAt(LocalDateTime.now()).build();
        when(healthCampaignRepository.findByTenantIdAndDeletedFalse(tenant, pageable))
                .thenReturn(new PageImpl<>(List.of(hc)));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(
                user(responsibleId, tenant, "Sarah", "Mbala")));
        when(campaignParticipantRepository.findByTenantIdAndCampaignId(tenant, campaignId))
                .thenReturn(List.of(
                        CampaignParticipant.builder().id(UUID.randomUUID()).tenantId(tenant)
                                .campaignId(campaignId).userId(UUID.randomUUID())
                                .registeredAt(java.time.Instant.now()).build(),
                        CampaignParticipant.builder().id(UUID.randomUUID()).tenantId(tenant)
                                .campaignId(campaignId).userId(UUID.randomUUID())
                                .registeredAt(java.time.Instant.now()).build()));

        Map<String, Object> v = healthService.campaignViews(tenant, null, pageable).getContent().get(0);

        assertEquals(responsibleId.toString(), v.get("responsibleId"));
        assertEquals("Sarah Mbala", v.get("responsibleName"));
        assertEquals(2, v.get("participantsCount"));
        assertEquals("VACCINATION", v.get("campaignType"));
        assertFalse(v.containsKey("responsible"));
    }
}
