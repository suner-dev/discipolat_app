package com.discipolat.modules.platform.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.alerts.domain.Alert;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.departments.domain.DepartmentTask;
import com.discipolat.modules.departments.domain.DepartmentTaskRepository;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.files.domain.FileEntity;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.platform.api.ResolvedBlock;
import com.discipolat.modules.platform.api.ResolvedPage;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.transfers.domain.TransferRequest;
import com.discipolat.modules.transfers.domain.TransferRequestRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

@ExtendWith(MockitoExtension.class)
class PageBuilderServiceTest {

    @Mock private CustomPageRepository pageRepository;
    @Mock private ConfigRevisionService revisionService;
    @Mock private AuditService auditService;
    @Mock private com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher propagationPublisher;
    @Mock private SecurityUtils securityUtils;
    @Mock private WorkspaceScopeService scopeService;
    @Mock private SoulRepository soulRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private TransferRequestRepository transferRepository;
    @Mock private FileEntityRepository fileRepository;
    @Mock private DepartmentTaskRepository taskRepository;

    private PageBuilderService service;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        service = new PageBuilderService(pageRepository, revisionService, auditService, propagationPublisher, securityUtils,
                scopeService, soulRepository, familyRepository, departmentRepository, userRepository,
                eventRepository, alertRepository, transferRepository, fileRepository, taskRepository);
    }

    private CustomPage page(String key, String slug, List<Map<String, Object>> blocks,
                            List<String> roles, boolean published) {
        return CustomPage.builder()
                .id(UUID.randomUUID()).key(key).title("Titre").description("Description")
                .slug(slug).layout("GRID_2").blocks(blocks).roles(roles)
                .enabled(true).published(published).version(published ? 2 : 1)
                .build();
    }

    private Map<String, Object> kpiBlock(String source) {
        return Map.of("type", "KPI", "config", Map.of("label", "Âmes", "source", source));
    }

    // ================================ CRUD ================================

    @Test
    void create_persistsAuditsAndRecordsRevision() {
        CustomPage request = page("APERCU", "apercu-eglise", List.of(kpiBlock("SOULS_TOTAL")),
                List.of("ADMIN"), false);
        SecurityTestHelper.loginAs(USER_ID);
        when(pageRepository.existsByKey("APERCU")).thenReturn(false);
        when(pageRepository.existsBySlug("apercu-eglise")).thenReturn(false);
        CustomPage saved = page("APERCU", "apercu-eglise", request.getBlocks(), request.getRoles(), false);
        when(pageRepository.save(any(CustomPage.class))).thenReturn(saved);

        CustomPage result = service.create(request);

        assertThat(result.getKey()).isEqualTo("APERCU");
        assertThat(result.getVersion()).isEqualTo(1);
        // Le créateur est posé sur la requête AVANT la sauvegarde.
        assertThat(request.getCreatedBy()).isEqualTo(USER_ID);
        verify(propagationPublisher).publishCreated(eq("CUSTOM_PAGE"), eq(result.getId()), any(), anyString());
        verify(revisionService).record(eq("CUSTOM_PAGE"), eq("APERCU"), eq("PAGE_CREATED"), anyMap());
    }

    @Test
    void create_rejectsDuplicateKey() {
        CustomPage request = page("APERCU", "apercu-eglise", List.of(), List.of(), false);
        when(pageRepository.existsByKey("APERCU")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(pageRepository, never()).save(any());
    }

    @Test
    void create_rejectsUnknownBlockSource() {
        CustomPage request = page("X", "x", List.of(kpiBlock("SOURCE_INCONNUE")), List.of(), false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source inconnue");
    }

    @Test
    void update_changesFieldsAndRecordsRevision() {
        CustomPage existing = page("APERCU", "apercu-eglise", List.of(kpiBlock("SOULS_TOTAL")),
                List.of("ADMIN"), false);
        when(pageRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        CustomPage request = CustomPage.builder().title("Nouveau titre").description("Nouvelle description").build();

        CustomPage result = service.update(existing.getId(), request);

        assertThat(result.getTitle()).isEqualTo("Nouveau titre");
        assertThat(result.getDescription()).isEqualTo("Nouvelle description");
        verify(propagationPublisher).publishUpdated(eq("CUSTOM_PAGE"), eq(existing.getId()), any(), any(), anyString());
        verify(revisionService).record(eq("CUSTOM_PAGE"), eq("APERCU"), eq("PAGE_UPDATED"), anyMap());
    }

    @Test
    void setPublished_incrementsVersionAndAudits() {
        CustomPage existing = page("APERCU", "apercu-eglise", List.of(), List.of(), false);
        when(pageRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        CustomPage result = service.setPublished(existing.getId(), true);

        assertThat(result.isPublished()).isTrue();
        assertThat(result.getVersion()).isEqualTo(2);
        verify(propagationPublisher).publishStatusChanged(eq("CUSTOM_PAGE"), eq(existing.getId()), anyString(), anyString(), anyString());
        verify(revisionService).record(eq("CUSTOM_PAGE"), eq("APERCU"), eq("PAGE_PUBLISHED"), anyMap());
    }

    @Test
    void setPublished_sameStateDoesNotIncrement() {
        CustomPage existing = page("APERCU", "apercu-eglise", List.of(), List.of(), true);
        when(pageRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        service.setPublished(existing.getId(), true);

        verify(revisionService, never()).record(any(), any(), any(), anyMap());
        assertThat(existing.getVersion()).isEqualTo(2);
    }

    @Test
    void delete_removesAndRecordsRevision() {
        CustomPage existing = page("APERCU", "apercu-eglise", List.of(), List.of(), false);
        when(pageRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        service.delete(existing.getId());

        verify(pageRepository).delete(existing);
        verify(revisionService).record(eq("CUSTOM_PAGE"), eq("APERCU"), eq("PAGE_DELETED"), anyMap());
    }

    // ================================ Rendu ================================

    @Test
    void resolve_returnsResolvedBlocksWithRealData() {
        CustomPage existing = page("APERCU", "apercu-eglise",
                List.of(kpiBlock("SOULS_TOTAL"), Map.of("type", "TEXTE", "config", Map.of("content", "Bienvenue"))),
                List.of(), true);
        when(pageRepository.findBySlug("apercu-eglise")).thenReturn(Optional.of(existing));
        when(scopeService.isSuperUser()).thenReturn(true);
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("PASTEUR"));
        when(soulRepository.countByDeletedFalse()).thenReturn(42L);

        ResolvedPage resolved = service.resolve("apercu-eglise");

        assertThat(resolved.page()).isEqualTo(existing);
        assertThat(resolved.blocks()).hasSize(2);
        ResolvedBlock kpi = resolved.blocks().get(0);
        assertThat(kpi.type()).isEqualTo("KPI");
        assertThat(kpi.data()).containsEntry("value", 42L);
        assertThat(resolved.blocks().get(1).data()).isNull();
    }

    @Test
    void resolve_scopesSoulCountForNonSuperUser() {
        CustomPage existing = page("APERCU", "apercu-eglise", List.of(kpiBlock("SOULS_TOTAL")), List.of(), true);
        when(pageRepository.findBySlug("apercu-eglise")).thenReturn(Optional.of(existing));
        when(scopeService.isSuperUser()).thenReturn(false);
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("FAISEUR"));
        when(scopeService.accessibleSoulIds()).thenReturn(Set.of(UUID.randomUUID(), UUID.randomUUID()));

        ResolvedPage resolved = service.resolve("apercu-eglise");

        assertThat(resolved.blocks().get(0).data()).containsEntry("value", 2L);
        verify(soulRepository, never()).countByDeletedFalse();
    }

    @Test
    void resolve_hidesSensitiveSourceForNonSuperUser() {
        CustomPage existing = page("ADMIN", "admin", List.of(kpiBlock("USERS_TOTAL")), List.of(), true);
        when(pageRepository.findBySlug("admin")).thenReturn(Optional.of(existing));
        when(scopeService.isSuperUser()).thenReturn(false);
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("FAISEUR"));
        when(scopeService.accessibleSoulIds()).thenReturn(Set.of());

        ResolvedPage resolved = service.resolve("admin");

        assertThat(resolved.blocks().get(0).data()).isNull();
    }

    @Test
    void resolve_deniedForUnauthorizedRole() {
        CustomPage existing = page("PASTEUR", "pasteur", List.of(), List.of("PASTEUR"), true);
        when(pageRepository.findBySlug("pasteur")).thenReturn(Optional.of(existing));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("FAISEUR"));
        when(securityUtils.isSuperUser()).thenReturn(false);

        assertThatThrownBy(() -> service.resolve("pasteur"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolve_allowsAdminOnPasteurOnlyPage() {
        CustomPage existing = page("PASTEUR", "pasteur", List.of(), List.of("PASTEUR"), true);
        when(pageRepository.findBySlug("pasteur")).thenReturn(Optional.of(existing));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("ADMIN"));
        when(securityUtils.isSuperUser()).thenReturn(true);

        assertThat(service.resolve("pasteur").page()).isEqualTo(existing);
    }

    @Test
    void resolve_hidesUnpublishedPage() {
        CustomPage existing = page("BROUILLON", "brouillon", List.of(), List.of(), false);
        when(pageRepository.findBySlug("brouillon")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.resolve("brouillon"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void resolve_unknownSlugThrows() {
        when(pageRepository.findBySlug("inconnu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("inconnu"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ========================== Résolution des listes ==========================

    @Test
    void resolveTable_recentAlertsResolved() {
        Alert alert = Alert.builder().id(UUID.randomUUID())
                .ameId(UUID.randomUUID()).titre("Assiduité").message("Absence répétée")
                .statut(StatutAlerte.ACTIVE).dateDeclenchement(LocalDateTime.now()).build();
        CustomPage existing = page("ALERTES", "alertes",
                List.of(Map.of("type", "LISTE", "config", Map.of("title", "Alertes", "source", "RECENT_ALERTS"))),
                List.of(), true);
        when(pageRepository.findBySlug("alertes")).thenReturn(Optional.of(existing));
        when(scopeService.isSuperUser()).thenReturn(true);
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("PASTEUR"));
        when(alertRepository.findTop10ByStatutOrderByDateDeclenchementDesc(StatutAlerte.ACTIVE))
                .thenReturn(List.of(alert));
        Soul soul = Soul.builder().id(alert.getAmeId()).nom("Kouassi").prenom("Aya").build();
        when(soulRepository.findAllById(Set.of(alert.getAmeId()))).thenReturn(List.of(soul));

        ResolvedPage resolved = service.resolve("alertes");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items =
                (List<Map<String, Object>>) resolved.blocks().get(0).data().get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).containsEntry("label", "Assiduité");
        assertThat(items.get(0)).containsEntry("value", "Aya Kouassi");
    }

    @Test
    void resolveTable_recentSoulsScopedByDepartment() {
        UUID soulId = UUID.randomUUID();
        Soul soul = Soul.builder().id(soulId).nom("Traoré").prenom("Ibrahim").statut(StatutAme.ACTIF).build();
        CustomPage existing = page("SOULS", "souls",
                List.of(Map.of("type", "TABLEAU", "config", Map.of("title", "Âmes", "source", "RECENT_SOULS"))),
                List.of(), true);
        when(pageRepository.findBySlug("souls")).thenReturn(Optional.of(existing));
        when(scopeService.isSuperUser()).thenReturn(false);
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("RESPONSABLE"));
        when(scopeService.accessibleSoulIds()).thenReturn(Set.of(soulId));
        when(soulRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(soul));

        ResolvedPage resolved = service.resolve("souls");

        @SuppressWarnings("unchecked")
        List<List<Object>> rows = (List<List<Object>>) resolved.blocks().get(0).data().get("rows");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).contains("Ibrahim Traoré");
    }

    // ================================ Divers ================================

    @Test
    void sources_catalogIsComplete() {
        List<com.discipolat.modules.platform.api.PageDataSource> sources = service.sources();

        assertThat(sources).extracting(com.discipolat.modules.platform.api.PageDataSource::key)
                .contains("SOULS_TOTAL", "SOULS_ACTIFS", "FAMILIES_TOTAL", "DEPARTMENTS_TOTAL",
                        "EVENTS_UPCOMING", "ALERTS_OPEN", "TRANSFERS_PENDING", "USERS_TOTAL",
                        "RECENT_SOULS", "UPCOMING_EVENTS", "RECENT_ALERTS", "RECENT_TRANSFERS",
                        "DEPARTMENTS_LIST", "SOULS_BY_STATUT", "EVENTS_BY_MONTH",
                        "ALERTS_BY_TYPE", "DEPARTMENTS_BY_STATUT", "CALENDAR_EVENTS", "SOULS_TIMELINE");
    }

    @Test
    void create_rejectsLinksBlockWithoutItems() {
        CustomPage request = page("LIENS", "liens",
                List.of(Map.of("type", "LIENS", "config", Map.of("title", "Accès"))), List.of(), false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LIENS");
    }

    @Test
    void create_rejectsChecklistBlockWithoutItems() {
        CustomPage request = page("CHECK", "check",
                List.of(Map.of("type", "CHECKLIST", "config", Map.of("title", "Suivi"))), List.of(), false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CHECKLIST");
    }

    @Test
    void create_rejectsUnknownChartSource() {
        CustomPage request = page("GRAPH", "graph",
                List.of(Map.of("type", "GRAPHIQUE",
                        "config", Map.of("title", "Graph", "source", "SOURCE_INCONNUE"))),
                List.of(), false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source inconnue");
    }

    // ========================== Nouveaux blocs (V66) ==========================

    @Test
    void resolveChart_soulsByStatutReturnsPieData() {
        CustomPage existing = page("GRAPH", "graph",
                List.of(Map.of("type", "GRAPHIQUE",
                        "config", Map.of("title", "Âmes", "source", "SOULS_BY_STATUT", "chartType", "PIE"))),
                List.of(), true);
        when(pageRepository.findBySlug("graph")).thenReturn(Optional.of(existing));
        when(scopeService.isSuperUser()).thenReturn(true);
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("PASTEUR"));
        when(soulRepository.countByStatut(any())).thenReturn(0L);
        when(soulRepository.countByStatut(StatutAme.ACTIF)).thenReturn(5L);
        when(soulRepository.countByStatut(StatutAme.EN_VEILLE)).thenReturn(2L);

        ResolvedPage resolved = service.resolve("graph");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resolved.blocks().get(0).data().get("data");
        assertThat(data).hasSize(2);
        assertThat(data.get(0)).containsEntry("name", "Actif").containsEntry("value", 5L);
        assertThat(data.get(1)).containsEntry("name", "En veille").containsEntry("value", 2L);
    }

    @Test
    void resolveChart_eventsByMonthReturnsSixMonthsSeries() {
        Event event = Event.builder().id(UUID.randomUUID())
                .titre("Culte").dateDebut(LocalDateTime.now().plusMonths(1).withDayOfMonth(15)).build();
        Event event2 = Event.builder().id(UUID.randomUUID())
                .titre("Étude biblique").dateDebut(LocalDateTime.now().plusMonths(1).withDayOfMonth(22)).build();
        CustomPage existing = page("EVENTS", "events-chart",
                List.of(Map.of("type", "GRAPHIQUE",
                        "config", Map.of("title", "Événements", "source", "EVENTS_BY_MONTH", "chartType", "BAR"))),
                List.of(), true);
        when(pageRepository.findBySlug("events-chart")).thenReturn(Optional.of(existing));
        when(scopeService.isSuperUser()).thenReturn(true);
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("PASTEUR"));
        when(eventRepository.findByDateDebutBetweenAndDeletedFalse(any(), any()))
                .thenReturn(List.of(event, event2));

        ResolvedPage resolved = service.resolve("events-chart");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resolved.blocks().get(0).data().get("data");
        assertThat(data).hasSize(7); // mois courant + 6 mois
        assertThat(data).anySatisfy(m -> assertThat(m.get("value")).isEqualTo(2L));
        assertThat(data).anySatisfy(m -> assertThat(m.get("value")).isEqualTo(0L));
    }

    @Test
    void resolveCalendar_eventsWithDatesResolved() {
        Event event = Event.builder().id(UUID.randomUUID())
                .titre("Veillée").lieu("Temple")
                .dateDebut(LocalDateTime.now().plusDays(10).withHour(20).withMinute(0)).build();
        CustomPage existing = page("CAL", "cal",
                List.of(Map.of("type", "CALENDRIER",
                        "config", Map.of("title", "Agenda", "source", "CALENDAR_EVENTS"))),
                List.of(), true);
        when(pageRepository.findBySlug("cal")).thenReturn(Optional.of(existing));
        when(scopeService.isSuperUser()).thenReturn(true);
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("PASTEUR"));
        when(eventRepository.findByDateDebutBetweenAndDeletedFalse(any(), any()))
                .thenReturn(List.of(event));

        ResolvedPage resolved = service.resolve("cal");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) resolved.blocks().get(0).data().get("events");
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).containsEntry("title", "Veillée").containsEntry("lieu", "Temple");
        assertThat((String) items.get(0).get("date")).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    void resolveTimeline_recentSoulsWithDates() {
        Soul soul = Soul.builder().id(UUID.randomUUID())
                .nom("Kouassi").prenom("Aya").statut(StatutAme.ACTIF)
                .createdAt(LocalDateTime.now()).build();
        CustomPage existing = page("TIMELINE", "timeline",
                List.of(Map.of("type", "TIMELINE",
                        "config", Map.of("title", "Nouvelles âmes", "source", "SOULS_TIMELINE"))),
                List.of(), true);
        when(pageRepository.findBySlug("timeline")).thenReturn(Optional.of(existing));
        when(scopeService.isSuperUser()).thenReturn(true);
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("PASTEUR"));
        when(soulRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(soul));

        ResolvedPage resolved = service.resolve("timeline");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) resolved.blocks().get(0).data().get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).containsEntry("label", "Aya Kouassi").containsEntry("value", "Actif");
        assertThat((String) items.get(0).get("date")).matches("\\d{2}/\\d{2}/\\d{4}");
    }

    @Test
    void resolveChecklist_blockHasNoServerData() {
        CustomPage existing = page("CHECK", "check",
                List.of(Map.of("type", "CHECKLIST",
                        "config", Map.of("title", "Suivi", "items", List.of("A", "B")))),
                List.of(), true);
        when(pageRepository.findBySlug("check")).thenReturn(Optional.of(existing));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("PASTEUR"));

        ResolvedPage resolved = service.resolve("check");

        assertThat(resolved.blocks().get(0).data()).isNull();
    }

    @Test
    void resolveFiles_recentDocumentsWithMetadata() {
        FileEntity file = FileEntity.builder().id(UUID.randomUUID())
                .nom("Programme du culte.pdf").typeFichier("application/pdf").taille(2048L)
                .categorie("COMPTE_RENDU").createdAt(LocalDateTime.now()).build();
        CustomPage existing = page("FILES", "files",
                List.of(Map.of("type", "FICHIERS",
                        "config", Map.of("title", "Documents", "source", "RECENT_FILES"))),
                List.of(), true);
        when(pageRepository.findBySlug("files")).thenReturn(Optional.of(existing));
        when(scopeService.isSuperUser()).thenReturn(true);
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("PASTEUR"));
        when(fileRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(file));

        ResolvedPage resolved = service.resolve("files");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) resolved.blocks().get(0).data().get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).containsEntry("nom", "Programme du culte.pdf")
                .containsEntry("categorie", "Compte Rendu").containsEntry("taille", 2048L);
        assertThat((String) items.get(0).get("date")).matches("\\d{2}/\\d{2}/\\d{4}");
    }

    @Test
    void resolveTasks_openTasksScopedWithDepartmentNames() {
        UUID deptId = UUID.randomUUID();
        Department dept = Department.builder().id(deptId).nom("Jeunesse").build();
        DepartmentTask task = DepartmentTask.builder().id(UUID.randomUUID()).departmentId(deptId)
                .titre("Préparer la répétition").priorite(DepartmentTask.TaskPriority.HAUTE)
                .statut(DepartmentTask.TaskStatus.EN_COURS)
                .echeance(java.time.LocalDate.now().plusDays(3)).build();
        CustomPage existing = page("TASKS", "tasks",
                List.of(Map.of("type", "TACHES",
                        "config", Map.of("title", "Tâches", "source", "TACHES_EN_COURS"))),
                List.of(), true);
        when(pageRepository.findBySlug("tasks")).thenReturn(Optional.of(existing));
        when(scopeService.isSuperUser()).thenReturn(false);
        when(scopeService.accessibleDepartmentIds()).thenReturn(Set.of(deptId));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("RESPONSABLE"));
        when(taskRepository.findTop10ByStatutInAndDepartmentIdInOrderByEcheanceAsc(any(), eq(Set.of(deptId))))
                .thenReturn(List.of(task));
        when(departmentRepository.findAllById(Set.of(deptId))).thenReturn(List.of(dept));

        ResolvedPage resolved = service.resolve("tasks");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) resolved.blocks().get(0).data().get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).containsEntry("titre", "Préparer la répétition")
                .containsEntry("departement", "Jeunesse").containsEntry("priorite", "HAUTE");
        assertThat((String) items.get(0).get("echeance")).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    void create_rejectsFormBlockWithoutValidCible() {
        CustomPage request = page("FORM", "form",
                List.of(Map.of("type", "FORMULAIRE",
                        "config", Map.of("title", "Contact", "type", "SUGGESTION"))),
                List.of(), false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cible");
    }

    @Test
    void create_acceptsValidFormBlock() {
        CustomPage request = page("FORM", "form",
                List.of(Map.of("type", "FORMULAIRE",
                        "config", Map.of("title", "Contact", "type", "SUGGESTION", "cible", "PASTEUR"))),
                List.of(), false);
        SecurityTestHelper.loginAs(USER_ID);
        when(pageRepository.existsByKey("FORM")).thenReturn(false);
        when(pageRepository.existsBySlug("form")).thenReturn(false);
        when(pageRepository.save(any(CustomPage.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomPage result = service.create(request);

        assertThat(result.getKey()).isEqualTo("FORM");
    }
}
