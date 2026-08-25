package com.discipolat.modules.departments.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.api.DepartmentChecklistItemRequest;
import com.discipolat.modules.departments.api.DepartmentChecklistRequest;
import com.discipolat.modules.departments.api.DepartmentDocumentRequest;
import com.discipolat.modules.departments.api.DepartmentEquipmentRequest;
import com.discipolat.modules.departments.api.DepartmentReportRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

@ExtendWith(MockitoExtension.class)
class DepartmentReportingServiceTest {

    @Mock
    private DepartmentService departmentService;
    @Mock
    private DepartmentDossierService dossierService;
    @Mock
    private DepartmentReportRepository reportRepository;
    @Mock
    private DepartmentChecklistRepository checklistRepository;
    @Mock
    private DepartmentChecklistItemRepository checklistItemRepository;
    @Mock
    private DepartmentEquipmentRepository equipmentRepository;
    @Mock
    private DepartmentDocumentRepository documentRepository;
    @Mock
    private DepartmentMemberObjectiveRepository objectiveRepository;
    @Mock
    private SecurityUtils securityUtils;

    private DepartmentReportingService service;
    private UUID deptId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        service = new DepartmentReportingService(departmentService, dossierService, reportRepository,
                checklistRepository, checklistItemRepository, equipmentRepository, documentRepository,
                objectiveRepository, securityUtils);
        deptId = UUID.randomUUID();
        userId = UUID.randomUUID();

        Department dept = new Department();
        dept.setId(deptId);
        dept.setNom("Audiovisuel");
        lenient().when(departmentService.findById(deptId)).thenReturn(dept);
        SecurityTestHelper.loginAs(userId);
    }

    // ======================= DOCUMENTATION =======================

    @Test
    void createDocument_savesAndReturns() {
        DepartmentDocumentRequest request = new DepartmentDocumentRequest(
                "Procédure d'accueil", DepartmentDocument.DocumentType.PROCEDURE,
                "Accueil des nouveaux membres", "https://docs/procedure-accueil.pdf", null);
        UUID docId = UUID.randomUUID();
        when(documentRepository.save(any(DepartmentDocument.class))).thenAnswer(inv -> {
            DepartmentDocument d = inv.getArgument(0);
            d.setId(docId);
            d.setCreatedAt(java.time.LocalDateTime.now());
            return d;
        });

        Map<String, Object> doc = service.createDocument(deptId, request);

        assertThat(doc.get("id")).isEqualTo(docId);
        assertThat(doc.get("type")).isEqualTo("PROCEDURE");
        assertThat(doc.get("statut")).isEqualTo("ACTIF");
        verify(documentRepository).save(any(DepartmentDocument.class));
    }

    @Test
    void updateDocument_refuseDocumentDunAutreDepartement() {
        UUID docId = UUID.randomUUID();
        DepartmentDocument other = DepartmentDocument.builder()
                .id(docId).departmentId(UUID.randomUUID()).titre("Autre").build();
        when(documentRepository.findById(docId)).thenReturn(Optional.of(other));

        DepartmentDocumentRequest request = new DepartmentDocumentRequest(
                "Guide", DepartmentDocument.DocumentType.GUIDE, null, null, null);

        assertThatThrownBy(() -> service.updateDocument(deptId, docId, request))
                .isInstanceOf(ResponseStatusException.class);
        verify(documentRepository, never()).save(any(DepartmentDocument.class));
    }

    @Test
    void listDocuments_retourneLesDocumentsDuDepartement() {
        DepartmentDocument doc = DepartmentDocument.builder()
                .id(UUID.randomUUID()).departmentId(deptId).titre("Guide son")
                .type(DepartmentDocument.DocumentType.GUIDE).statut(DepartmentDocument.DocumentStatus.ACTIF)
                .build();
        when(documentRepository.findByDepartmentIdOrderByCreatedAtDesc(deptId)).thenReturn(List.of(doc));

        List<Map<String, Object>> docs = service.listDocuments(deptId);

        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).get("titre")).isEqualTo("Guide son");
    }

    // ======================= RAPPORTS =======================

    @Test
    void generateReport_buildsSynthesisFromRealStatsAndSaves() {
        Map<String, Object> stats = Map.of(
                "effectif", Map.of("total", 10L, "actifs", 8L, "nouveaux30j", 2L,
                        "enIntegration", 1L, "decroches", 1L),
                "presence", Map.of("total", 20L, "presents", 18L, "absents", 2L, "taux", 90.0),
                "taches", Map.of("total", 5L, "ouvertes", 2L, "enRetard", 1L, "terminees", 3L),
                "equipes", Map.of("actives", 3L, "archivees", 0L),
                "disciplineParCategorie", Map.of("AVERTISSEMENT", 1L),
                "evenements", List.of(Map.of("titre", "Culte spécial", "date", "2026-08-20"))
        );
        when(dossierService.getDepartmentStats(deptId)).thenReturn(stats);
        when(objectiveRepository.findByDepartmentId(deptId)).thenReturn(List.of());
        when(reportRepository.save(any(DepartmentReport.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.generateReport(deptId,
                new DepartmentReportRequest("HEBDOMADAIRE", null, null, null, null, "SOUMIS"));

        assertThat(result.get("type")).isEqualTo("HEBDOMADAIRE");
        assertThat(result.get("statut")).isEqualTo("SOUMIS");
        assertThat(result.get("titre")).asString().contains("hebdomadaire");
        String contenu = (String) result.get("contenu");
        assertThat(contenu).contains("Audiovisuel");
        assertThat(contenu).contains("10 membre");
        assertThat(contenu).contains("90.0 %");
        assertThat(contenu).contains("1 tâche");
        assertThat(contenu).contains("Culte spécial");
        verify(reportRepository).save(any(DepartmentReport.class));
    }

    @Test
    void generateReport_invalidType_rejects() {
        assertThatThrownBy(() -> service.generateReport(deptId,
                new DepartmentReportRequest("INCONNU", null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Type de rapport invalide");
        verify(reportRepository, never()).save(any());
    }

    @Test
    void saveManualReport_requiresContenu() {
        assertThatThrownBy(() -> service.saveManualReport(deptId,
                new DepartmentReportRequest("ACTIVITE", "Bilan", null, null, "", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("contenu");
    }

    @Test
    void updateReport_modifieContenuEtStatut() {
        UUID reportId = UUID.randomUUID();
        DepartmentReport report = DepartmentReport.builder()
                .id(reportId).departmentId(deptId).auteurId(userId)
                .type(DepartmentReport.ReportType.ACTIVITE).titre("Bilan")
                .contenu("Ancien contenu").statut(DepartmentReport.ReportStatus.BROUILLON).build();
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

        Map<String, Object> result = service.updateReport(deptId, reportId,
                new DepartmentReportRequest("ACTIVITE", "Bilan corrigé", null, null, "Nouveau contenu", "SOUMIS"));

        assertThat(result.get("statut")).isEqualTo("SOUMIS");
        assertThat(report.getContenu()).isEqualTo("Nouveau contenu");
        verify(reportRepository).save(report);
    }

    @Test
    void updateReport_refuseRapportDunAutreDepartement() {
        UUID reportId = UUID.randomUUID();
        DepartmentReport other = DepartmentReport.builder().id(reportId)
                .departmentId(UUID.randomUUID()).contenu("x").build();
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.updateReport(deptId, reportId,
                new DepartmentReportRequest("ACTIVITE", null, null, null, "y", null)))
                .isInstanceOf(ResponseStatusException.class);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void deleteReport_onlySameDepartment() {
        UUID reportId = UUID.randomUUID();
        DepartmentReport other = DepartmentReport.builder().id(reportId)
                .departmentId(UUID.randomUUID()).contenu("x").build();
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.deleteReport(deptId, reportId))
                .isInstanceOf(ResponseStatusException.class);
        verify(reportRepository, never()).delete(any());
    }

    @Test
    void exportReportCsv_containsReportData() {
        UUID reportId = UUID.randomUUID();
        DepartmentReport report = DepartmentReport.builder()
                .id(reportId).departmentId(deptId).auteurId(userId)
                .type(DepartmentReport.ReportType.MENSUEL)
                .titre("Rapport mensuel — juillet")
                .periodeDebut(LocalDate.of(2026, 7, 1))
                .periodeFin(LocalDate.of(2026, 7, 31))
                .statut(DepartmentReport.ReportStatus.SOUMIS)
                .contenu("EFFECTIF\n- 12 membres au total")
                .build();
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

        String csv = service.exportReportCsv(deptId, reportId);
        assertThat(csv).contains("Rapport mensuel — juillet");
        assertThat(csv).contains("Audiovisuel");
        assertThat(csv).contains("2026-07-01 -> 2026-07-31");
        assertThat(csv).contains("12 membres au total");
    }

    // ======================= CHECKLISTS =======================

    @Test
    void createChecklist_savesItemsInOrder() {
        when(checklistRepository.save(any(DepartmentChecklist.class)))
                .thenAnswer(inv -> {
                    DepartmentChecklist c = inv.getArgument(0);
                    c.setId(UUID.randomUUID());
                    return c;
                });
        when(checklistItemRepository.save(any(DepartmentChecklistItem.class)))
                .thenAnswer(inv -> {
                    DepartmentChecklistItem i = inv.getArgument(0);
                    i.setId(UUID.randomUUID());
                    return i;
                });

        Map<String, Object> result = service.createChecklist(deptId,
                new DepartmentChecklistRequest("Préparation dimanche", "GENERAL", null,
                        List.of("Sono testée", "Caméras prêtes"), null));

        assertThat(result.get("titre")).isEqualTo("Préparation dimanche");
        assertThat(result.get("statut")).isEqualTo("OUVERTE");
        assertThat(result.get("progression")).isEqualTo(0L);
        List<?> items = (List<?>) result.get("items");
        assertThat(items).hasSize(2);
        verify(checklistItemRepository, times(2)).save(any(DepartmentChecklistItem.class));
    }

    @Test
    void toggleLastItem_autoClosesChecklist() {
        UUID checklistId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        DepartmentChecklist checklist = DepartmentChecklist.builder()
                .id(checklistId).departmentId(deptId).titre("Checklist")
                .cibleType(DepartmentChecklist.CibleType.GENERAL)
                .statut(DepartmentChecklist.ChecklistStatus.OUVERTE).build();
        when(checklistRepository.findByIdAndDepartmentId(checklistId, deptId))
                .thenReturn(Optional.of(checklist));
        DepartmentChecklistItem item = DepartmentChecklistItem.builder()
                .id(itemId).checklistId(checklistId).libelle("Sono testée").fait(false).ordre(0).build();
        when(checklistItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(checklistItemRepository.findByChecklistIdOrderByOrdreAsc(checklistId))
                .thenReturn(List.of(item));

        Map<String, Object> result = service.toggleChecklistItem(deptId, checklistId, itemId,
                new DepartmentChecklistItemRequest("Sono testée", true));

        assertThat(item.isFait()).isTrue();
        assertThat(result.get("statut")).isEqualTo("TERMINEE");
        assertThat(result.get("progression")).isEqualTo(100L);
    }

    @Test
    void addItemToTerminatedChecklist_rejects() {
        UUID checklistId = UUID.randomUUID();
        DepartmentChecklist checklist = DepartmentChecklist.builder()
                .id(checklistId).departmentId(deptId).titre("Checklist")
                .statut(DepartmentChecklist.ChecklistStatus.TERMINEE).build();
        when(checklistRepository.findByIdAndDepartmentId(checklistId, deptId))
                .thenReturn(Optional.of(checklist));

        assertThatThrownBy(() -> service.addChecklistItem(deptId, checklistId,
                new DepartmentChecklistItemRequest("Nouvel item", null)))
                .isInstanceOf(ResponseStatusException.class);
        verify(checklistItemRepository, never()).save(any());
    }

    // ======================= INVENTAIRE =======================

    @Test
    void createEquipment_savesWithDefaults() {
        when(equipmentRepository.save(any(DepartmentEquipment.class)))
                .thenAnswer(inv -> {
                    DepartmentEquipment e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        Map<String, Object> result = service.createEquipment(deptId,
                new DepartmentEquipmentRequest("Caméra Sony", "Caméra 4K", null, null, null, null, "Salle 3", null));

        assertThat(result.get("nom")).isEqualTo("Caméra Sony");
        assertThat(result.get("quantite")).isEqualTo(1);
        assertThat(result.get("etat")).isEqualTo("BON");
    }

    @Test
    void updateEquipment_changesStateAndAssignment() {
        UUID equipmentId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        DepartmentEquipment equipment = DepartmentEquipment.builder()
                .id(equipmentId).departmentId(deptId).nom("Caméra").etat(DepartmentEquipment.Etat.BON).build();
        when(equipmentRepository.findByIdAndDepartmentId(equipmentId, deptId))
                .thenReturn(Optional.of(equipment));

        Map<String, Object> result = service.updateEquipment(deptId, equipmentId,
                new DepartmentEquipmentRequest("Caméra Sony", "Nouvelle caméra", 2, "USAGE",
                        memberId, memberId, "Studio", LocalDate.of(2026, 8, 1)));

        assertThat(result.get("quantite")).isEqualTo(2);
        assertThat(result.get("etat")).isEqualTo("USAGE");
        assertThat(result.get("responsableId")).isEqualTo(memberId);
        assertThat(result.get("affecteAId")).isEqualTo(memberId);
        assertThat(result.get("dateAcquisition")).isEqualTo("2026-08-01");
    }

    @Test
    void deleteEquipment_unknown_rejects() {
        UUID equipmentId = UUID.randomUUID();
        when(equipmentRepository.findByIdAndDepartmentId(equipmentId, deptId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteEquipment(deptId, equipmentId))
                .isInstanceOf(ResponseStatusException.class);
        verify(equipmentRepository, never()).delete(any());
    }
}
