package com.discipolat.modules.imports.domain;

import com.discipolat.modules.customfields.domain.CustomFieldDefinition;
import com.discipolat.modules.customfields.domain.CustomFieldDefinitionRepository;
import com.discipolat.modules.events.repository.ChurchEventRepository;
import com.discipolat.modules.exports.domain.SpaceExportBundle;
import com.discipolat.modules.exports.domain.SpaceExportService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.spaces.domain.Space;
import com.discipolat.modules.spaces.domain.SpaceRepository;
import com.discipolat.modules.spaces.domain.SpaceStatus;
import com.discipolat.modules.spaces.domain.SpaceType;
import com.discipolat.modules.spaces.domain.VisiblePeopleScope;
import com.discipolat.modules.statuses.domain.CustomStatus;
import com.discipolat.modules.statuses.domain.CustomStatusRepository;
import com.discipolat.modules.tenants.domain.SpaceModule;
import com.discipolat.modules.tenants.domain.SpaceModuleRepository;
import com.discipolat.modules.workflow.domain.WorkflowDefinition;
import com.discipolat.modules.workflow.domain.WorkflowDefinitionRepository;
import com.discipolat.modules.workflow.domain.WorkflowStep;
import com.discipolat.modules.workflow.domain.WorkflowStepRepository;
import com.discipolat.modules.workflow.domain.WorkflowStepType;
import com.discipolat.modules.workflow.domain.WorkflowTransition;
import com.discipolat.modules.workflow.domain.WorkflowTransitionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * G4.5 — DoD « export/import round-trip ✅ · rapport d'erreur ✅ ».
 *
 * <p>Les repositories sont simulés par des magasins en mémoire : cela permet de
 * vérifier réellement le round-trip (export → JSON → import → export identique),
 * l'idempotence (seconde importation sans doublon) et l'absence d'écrasement
 * silencieux en cas de conflit d'UUID.
 */
@ExtendWith(MockitoExtension.class)
class SpaceExportImportTest {

    @Mock private SpaceRepository spaceRepository;
    @Mock private SpaceModuleRepository spaceModuleRepository;
    @Mock private CustomFieldDefinitionRepository customFieldDefinitionRepository;
    @Mock private CustomStatusRepository customStatusRepository;
    @Mock private WorkflowDefinitionRepository workflowDefinitionRepository;
    @Mock private WorkflowStepRepository workflowStepRepository;
    @Mock private WorkflowTransitionRepository workflowTransitionRepository;
    @Mock private SoulRepository soulRepository;
    @Mock private ChurchEventRepository churchEventRepository;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private SpaceExportService exportService;
    private SpaceImportService importService;

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TENANT = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final Map<UUID, Space> spaces = new LinkedHashMap<>();
    private final Map<UUID, SpaceModule> modules = new LinkedHashMap<>();
    private final Map<UUID, CustomFieldDefinition> fields = new LinkedHashMap<>();
    private final Map<UUID, CustomStatus> statuses = new LinkedHashMap<>();
    private final Map<UUID, WorkflowDefinition> workflows = new LinkedHashMap<>();
    private final Map<UUID, WorkflowStep> steps = new LinkedHashMap<>();
    private final Map<UUID, WorkflowTransition> transitions = new LinkedHashMap<>();
    private final AtomicInteger writes = new AtomicInteger();

    @BeforeEach
    void setUp() {
        exportService = new SpaceExportService(spaceRepository, spaceModuleRepository,
                customFieldDefinitionRepository, customStatusRepository, workflowDefinitionRepository,
                workflowStepRepository, workflowTransitionRepository, soulRepository,
                churchEventRepository, mapper);
        importService = new SpaceImportService(spaceRepository, spaceModuleRepository,
                customFieldDefinitionRepository, customStatusRepository, workflowDefinitionRepository,
                workflowStepRepository, workflowTransitionRepository, mapper);
        wireRepositories();
    }

    // ------------------------------------------------------------------
    // Magasins en mémoire branchés sur les repositories simulés
    // ------------------------------------------------------------------

    private void wireRepositories() {
        lenient().when(spaceRepository.save(any(Space.class))).thenAnswer(inv -> {
            Space s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            writes.incrementAndGet();
            spaces.put(s.getId(), s);
            return s;
        });
        lenient().when(spaceRepository.findByIdAndTenantId(any(), any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            UUID tenant = inv.getArgument(1);
            Space s = spaces.get(id);
            return (s != null && tenant.equals(s.getTenantId())) ? Optional.of(s) : Optional.empty();
        });
        lenient().when(spaceRepository.findByTenantIdAndCode(any(), any())).thenAnswer(inv -> {
            UUID tenant = inv.getArgument(0);
            String code = inv.getArgument(1);
            return spaces.values().stream()
                    .filter(s -> tenant.equals(s.getTenantId()) && code.equals(s.getCode()))
                    .findFirst();
        });
        lenient().when(spaceRepository.findByTenantIdAndDeletedAtIsNull(any())).thenAnswer(inv -> {
            UUID tenant = inv.getArgument(0);
            return spaces.values().stream()
                    .filter(s -> tenant.equals(s.getTenantId()) && s.getDeletedAt() == null)
                    .toList();
        });

        lenient().when(spaceModuleRepository.save(any(SpaceModule.class))).thenAnswer(inv -> {
            SpaceModule m = inv.getArgument(0);
            if (m.getId() == null) m.setId(UUID.randomUUID());
            writes.incrementAndGet();
            modules.put(m.getId(), m);
            return m;
        });
        lenient().when(spaceModuleRepository.findByTenantIdAndSpaceId(any(), any())).thenAnswer(inv -> {
            UUID tenant = inv.getArgument(0);
            UUID spaceId = inv.getArgument(1);
            return modules.values().stream()
                    .filter(m -> tenant.equals(m.getTenantId()) && spaceId.equals(m.getSpaceId()))
                    .toList();
        });
        lenient().when(spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(any(), any(), any()))
                .thenAnswer(inv -> {
                    UUID tenant = inv.getArgument(0);
                    UUID spaceId = inv.getArgument(1);
                    String code = inv.getArgument(2);
                    return modules.values().stream()
                            .filter(m -> tenant.equals(m.getTenantId())
                                    && spaceId.equals(m.getSpaceId()) && code.equals(m.getModuleCode()))
                            .findFirst();
                });

        lenient().when(customFieldDefinitionRepository.findAll())
                .thenAnswer(inv -> new ArrayList<>(fields.values()));
        lenient().when(customFieldDefinitionRepository.save(any(CustomFieldDefinition.class))).thenAnswer(inv -> {
            CustomFieldDefinition f = inv.getArgument(0);
            if (f.getId() == null) f.setId(UUID.randomUUID());
            writes.incrementAndGet();
            fields.put(f.getId(), f);
            return f;
        });
        lenient().when(customFieldDefinitionRepository.findByTenantIdAndEntiteTypeAndCode(any(), any(), any()))
                .thenAnswer(inv -> {
                    UUID tenant = inv.getArgument(0);
                    String entite = inv.getArgument(1);
                    String code = inv.getArgument(2);
                    return fields.values().stream()
                            .filter(f -> tenant.equals(f.getTenantId())
                                    && entite.equals(f.getEntiteType()) && code.equals(f.getCode()))
                            .findFirst();
                });

        lenient().when(customStatusRepository.save(any(CustomStatus.class))).thenAnswer(inv -> {
            CustomStatus c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            writes.incrementAndGet();
            statuses.put(c.getId(), c);
            return c;
        });
        lenient().when(customStatusRepository.findByTenantIdAndSpaceIdAndDeletedAtIsNull(any(), any()))
                .thenAnswer(inv -> {
                    UUID tenant = inv.getArgument(0);
                    UUID spaceId = inv.getArgument(1);
                    return statuses.values().stream()
                            .filter(c -> tenant.equals(c.getTenantId()) && spaceId.equals(c.getSpaceId())
                                    && c.getDeletedAt() == null)
                            .toList();
                });
        lenient().when(customStatusRepository
                        .findByTenantIdAndDeletedAtIsNullOrderByEntityTypeAscDisplayOrderAsc(any()))
                .thenAnswer(inv -> {
                    UUID tenant = inv.getArgument(0);
                    return statuses.values().stream()
                            .filter(c -> tenant.equals(c.getTenantId()) && c.getDeletedAt() == null)
                            .toList();
                });
        lenient().when(customStatusRepository
                        .findByTenantIdAndEntityTypeAndSpaceIdAndCodeAndDeletedAtIsNull(any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    UUID tenant = inv.getArgument(0);
                    String entity = inv.getArgument(1);
                    UUID spaceId = inv.getArgument(2);
                    String code = inv.getArgument(3);
                    return statuses.values().stream()
                            .filter(c -> tenant.equals(c.getTenantId()) && entity.equals(c.getEntityType())
                                    && spaceId.equals(c.getSpaceId()) && code.equals(c.getCode())
                                    && c.getDeletedAt() == null)
                            .findFirst();
                });
        lenient().when(customStatusRepository
                        .findByTenantIdAndEntityTypeAndSpaceIdIsNullAndCodeAndDeletedAtIsNull(any(), any(), any()))
                .thenAnswer(inv -> {
                    UUID tenant = inv.getArgument(0);
                    String entity = inv.getArgument(1);
                    String code = inv.getArgument(2);
                    return statuses.values().stream()
                            .filter(c -> tenant.equals(c.getTenantId()) && entity.equals(c.getEntityType())
                                    && c.getSpaceId() == null && code.equals(c.getCode())
                                    && c.getDeletedAt() == null)
                            .findFirst();
                });

        lenient().when(workflowDefinitionRepository.save(any(WorkflowDefinition.class))).thenAnswer(inv -> {
            WorkflowDefinition w = inv.getArgument(0);
            if (w.getId() == null) w.setId(UUID.randomUUID());
            writes.incrementAndGet();
            workflows.put(w.getId(), w);
            return w;
        });
        lenient().when(workflowDefinitionRepository.findByTenantIdAndDeletedAtIsNull(any())).thenAnswer(inv -> {
            UUID tenant = inv.getArgument(0);
            return workflows.values().stream()
                    .filter(w -> tenant.equals(w.getTenantId()) && w.getDeletedAt() == null)
                    .toList();
        });
        lenient().when(workflowDefinitionRepository
                        .findFirstByTenantIdAndSpaceIdIsNullAndCodeAndDeletedAtIsNullOrderByVersionDesc(any(), any()))
                .thenAnswer(inv -> {
                    UUID tenant = inv.getArgument(0);
                    String code = inv.getArgument(1);
                    return workflows.values().stream()
                            .filter(w -> tenant.equals(w.getTenantId()) && w.getSpaceId() == null
                                    && code.equals(w.getCode()) && w.getDeletedAt() == null)
                            .findFirst();
                });
        lenient().when(workflowDefinitionRepository
                        .findFirstByTenantIdAndSpaceIdAndCodeAndDeletedAtIsNullOrderByVersionDesc(any(), any(), any()))
                .thenAnswer(inv -> {
                    UUID tenant = inv.getArgument(0);
                    UUID spaceId = inv.getArgument(1);
                    String code = inv.getArgument(2);
                    return workflows.values().stream()
                            .filter(w -> tenant.equals(w.getTenantId()) && spaceId.equals(w.getSpaceId())
                                    && code.equals(w.getCode()) && w.getDeletedAt() == null)
                            .findFirst();
                });

        lenient().when(workflowStepRepository.save(any(WorkflowStep.class))).thenAnswer(inv -> {
            WorkflowStep s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            writes.incrementAndGet();
            steps.put(s.getId(), s);
            return s;
        });
        lenient().when(workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(any())).thenAnswer(inv -> {
            UUID workflowId = inv.getArgument(0);
            return steps.values().stream()
                    .filter(s -> workflowId.equals(s.getWorkflowId()))
                    .sorted((a, b) -> Integer.compare(a.getStepOrder(), b.getStepOrder()))
                    .toList();
        });
        lenient().doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<WorkflowStep> list = inv.getArgument(0);
            list.forEach(s -> steps.remove(s.getId()));
            return null;
        }).when(workflowStepRepository).deleteAll(any(List.class));
        lenient().doNothing().when(workflowStepRepository).flush();

        lenient().when(workflowTransitionRepository.save(any(WorkflowTransition.class))).thenAnswer(inv -> {
            WorkflowTransition t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            writes.incrementAndGet();
            transitions.put(t.getId(), t);
            return t;
        });
        lenient().when(workflowTransitionRepository.findByWorkflowId(any())).thenAnswer(inv -> {
            UUID workflowId = inv.getArgument(0);
            return transitions.values().stream()
                    .filter(t -> workflowId.equals(t.getWorkflowId()))
                    .toList();
        });
        lenient().doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<WorkflowTransition> list = inv.getArgument(0);
            list.forEach(t -> transitions.remove(t.getId()));
            return null;
        }).when(workflowTransitionRepository).deleteAll(any(List.class));

        lenient().when(soulRepository.findByTenantId(any())).thenAnswer(inv -> List.<Soul>of());
        lenient().when(churchEventRepository.findByTenantIdAndDeletedAtIsNullOrderByStartAtAsc(any()))
                .thenAnswer(inv -> List.<com.discipolat.modules.events.domain.ChurchEvent>of());
    }

    // ------------------------------------------------------------------
    // Jeux de données
    // ------------------------------------------------------------------

    private UUID seedSpaceGraph() {
        UUID spaceId = UUID.randomUUID();
        Space s = Space.builder()
                .id(spaceId)
                .tenantId(TENANT)
                .organizationUnitId(UUID.randomUUID())
                .spaceType(SpaceType.DEPARTMENT)
                .templateCode("CHORALE")
                .name("Chorale")
                .code("chorale")
                .icon("music")
                .color("#112233")
                .description("Espace chorale")
                .status(SpaceStatus.ACTIVE)
                .visiblePeopleScope(VisiblePeopleScope.CHURCH)
                .configurationJson(new LinkedHashMap<>(Map.of("pages", List.of("home", "tasks"))))
                .build();
        spaces.put(s.getId(), s);

        SpaceModule module = SpaceModule.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).spaceId(spaceId)
                .moduleCode("tasks").enabled(true).displayOrder(1)
                .configurationJson(new LinkedHashMap<>()).limitsJson(new LinkedHashMap<>()).build();
        modules.put(module.getId(), module);

        CustomFieldDefinition field = CustomFieldDefinition.builder()
                .id(UUID.randomUUID()).tenantId(TENANT)
                .entiteType("SOUL").code("taille_tshirt").label("Taille T-shirt").type("SELECT")
                .obligatoire(true).ordre(1).options(new ArrayList<>(List.of("S", "M", "L")))
                .placeholder("Choisir").defaultValue("M")
                .rolesLecture(new ArrayList<>(List.of("ADMIN")))
                .rolesEcriture(new ArrayList<>(List.of("ADMIN")))
                .actif(true).build();
        fields.put(field.getId(), field);

        CustomStatus status = CustomStatus.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).spaceId(spaceId)
                .entityType("ASSET").code("VALIDE").name("Validé").color("#00FF00").icon("check")
                .displayOrder(2).initial(false).finalStatus(true)
                .allowedTransitions(new ArrayList<>()).build();
        statuses.put(status.getId(), status);

        UUID workflowId = UUID.randomUUID();
        workflows.put(workflowId, WorkflowDefinition.builder()
                .id(workflowId).tenantId(TENANT).spaceId(spaceId)
                .entityType("EXPENSE").code("achat_materiel").name("Achat matériel")
                .enabled(true).version(1).build());

        UUID step1 = UUID.randomUUID();
        UUID step2 = UUID.randomUUID();
        steps.put(step1, WorkflowStep.builder()
                .id(step1).workflowId(workflowId).stepOrder(1)
                .stepType(WorkflowStepType.APPROVAL).name("Chef de département")
                .conditionsJson(new LinkedHashMap<>()).autoActionJson(new LinkedHashMap<>())
                .assigneeRole("CHEF_DEPARTEMENT").assigneeScope("SPACE")
                .timeoutHours(24).escalationRole("PASTEUR").build());
        steps.put(step2, WorkflowStep.builder()
                .id(step2).workflowId(workflowId).stepOrder(2)
                .stepType(WorkflowStepType.APPROVAL).name("Finance")
                .conditionsJson(new LinkedHashMap<>()).autoActionJson(new LinkedHashMap<>())
                .assigneeRole("FINANCE").assigneeScope("TENANT").build());

        UUID transitionId = UUID.randomUUID();
        transitions.put(transitionId, WorkflowTransition.builder()
                .id(transitionId).workflowId(workflowId)
                .fromStepId(step1).toStepId(step2).onEvent("APPROVE").build());

        return spaceId;
    }

    private void resetStore() {
        spaces.clear();
        modules.clear();
        fields.clear();
        statuses.clear();
        workflows.clear();
        steps.clear();
        transitions.clear();
        writes.set(0);
    }

    private JsonNode canonicalView(SpaceExportBundle bundle) {
        ObjectNode node = mapper.valueToTree(bundle.normalized());
        node.remove("exportedAt");
        return node;
    }

    private SpaceExportBundle doc(String format, int version) {
        return new SpaceExportBundle(format, version, Instant.now(), SpaceExportBundle.SCOPE_SPACE,
                null, null, List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    private SpaceExportBundle docWithSpace(SpaceExportBundle.SpaceConfig space) {
        return new SpaceExportBundle(SpaceExportBundle.FORMAT, SpaceExportBundle.VERSION, Instant.now(),
                SpaceExportBundle.SCOPE_SPACE, null, space, List.of(), List.of(), List.of(),
                List.of(), List.of(), Map.of());
    }

    private SpaceExportBundle.SpaceConfig spaceConfig(UUID id, String code, String name) {
        return new SpaceExportBundle.SpaceConfig(id, UUID.randomUUID(), "FAMILY", null, name, code,
                null, null, null, "ACTIVE", "CHURCH", Map.of());
    }

    // ------------------------------------------------------------------
    // Round-trip export → import → export
    // ------------------------------------------------------------------

    @Test
    void exportPuisImportDansUnDepotVide_puisReExport_estIdentique() {
        UUID spaceId = seedSpaceGraph();
        SpaceExportBundle exported = exportService.exportSpace(TENANT, spaceId);
        byte[] json = exportService.toJson(exported);

        resetStore();

        SpaceImportReport report = importService.apply(importService.parse(json), TENANT);

        assertThat(report.valid()).isTrue();
        assertThat(report.applied()).isTrue();
        assertThat(report.counters().spacesCreated()).isEqualTo(1);
        assertThat(report.counters().modulesUpserted()).isEqualTo(1);
        // Un export d'ESPACE ne porte pas le référentiel de champs (niveau tenant) :
        // il est transporté par l'export de tenant (test dédié plus bas).
        assertThat(report.counters().customFieldsCreated()).isZero();
        assertThat(report.counters().statusesCreated()).isEqualTo(1);
        assertThat(report.counters().workflowsCreated()).isEqualTo(1);
        assertThat(report.issues()).isEmpty();

        SpaceExportBundle reExported = exportService.exportSpace(TENANT, spaceId);
        assertThat(canonicalView(reExported)).isEqualTo(canonicalView(exported));
    }

    @Test
    void exportTenant_puisImportDansUnDepotVide_puisReExport_estIdentique() {
        UUID spaceId = seedSpaceGraph();
        SpaceExportBundle exported = exportService.exportTenant(TENANT);
        byte[] json = exportService.toJson(exported);

        resetStore();

        SpaceImportReport report = importService.apply(importService.parse(json), TENANT);

        assertThat(report.valid()).isTrue();
        assertThat(report.counters().spacesCreated()).isEqualTo(1);
        assertThat(report.counters().customFieldsCreated()).isEqualTo(1);
        assertThat(report.counters().statusesCreated()).isEqualTo(1);
        assertThat(report.counters().workflowsCreated()).isEqualTo(1);
        assertThat(report.issues()).isEmpty();

        SpaceExportBundle reExported = exportService.exportTenant(TENANT);
        assertThat(canonicalView(reExported)).isEqualTo(canonicalView(exported));
        assertThat(reExported.spaces()).extracting(SpaceExportBundle.SpaceConfig::id).containsExactly(spaceId);
    }

    @Test
    void secondeImportation_estIdempotente_sansAucunDoublon() {
        UUID spaceId = seedSpaceGraph();
        byte[] json = exportService.toJson(exportService.exportSpace(TENANT, spaceId));

        SpaceImportReport first = importService.apply(importService.parse(json), TENANT);
        assertThat(first.counters().spacesUnchanged()).isEqualTo(1);
        assertThat(first.counters().spacesCreated()).isZero();

        int spacesBefore = spaces.size();
        int modulesBefore = modules.size();
        int fieldsBefore = fields.size();
        int statusesBefore = statuses.size();
        int workflowsBefore = workflows.size();
        int stepsBefore = steps.size();
        int transitionsBefore = transitions.size();

        SpaceImportReport second = importService.apply(importService.parse(json), TENANT);

        assertThat(spaces.size()).isEqualTo(spacesBefore);
        assertThat(modules.size()).isEqualTo(modulesBefore);
        assertThat(fields.size()).isEqualTo(fieldsBefore);
        assertThat(statuses.size()).isEqualTo(statusesBefore);
        assertThat(workflows.size()).isEqualTo(workflowsBefore);
        assertThat(steps.size()).isEqualTo(stepsBefore);
        assertThat(transitions.size()).isEqualTo(transitionsBefore);
        assertThat(second.counters().spacesCreated()).isZero();
        assertThat(second.counters().spacesUnchanged()).isEqualTo(1);
        assertThat(second.counters().customFieldsCreated()).isZero();
        assertThat(second.counters().statusesCreated()).isZero();
        assertThat(second.counters().workflowsCreated()).isZero();
    }

    @Test
    void dryRun_neRealiseAucuneEcriture() {
        UUID spaceId = seedSpaceGraph();
        byte[] json = exportService.toJson(exportService.exportSpace(TENANT, spaceId));

        SpaceImportReport report = importService.validate(importService.parse(json), TENANT);

        assertThat(report.valid()).isTrue();
        assertThat(report.dryRun()).isTrue();
        assertThat(report.applied()).isFalse();
        assertThat(writes.get()).isZero();
    }

    // ------------------------------------------------------------------
    // Rejet, conflits et isolation
    // ------------------------------------------------------------------

    @Test
    void documentAbsent_formatInconnu_ouVersionTropRecente_estRejete() {
        assertThat(importService.validate(null, TENANT).valid()).isFalse();
        assertThat(importService.validate(doc("space_export_v0", 1), TENANT).valid()).isFalse();
        assertThat(importService.validate(doc(SpaceExportBundle.FORMAT, 99), TENANT).valid()).isFalse();
        assertThat(writes.get()).isZero();
    }

    @Test
    void jsonMalforme_estRejeteSansEcriture() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> importService.parse("{ ceci n'est pas du json".getBytes()))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> importService.parse(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(writes.get()).isZero();
    }

    @Test
    void conflitDUuid_estSignaleEtLaLigneExistanteNestPasEcrasee() {
        UUID existingId = UUID.randomUUID();
        spaces.put(existingId, Space.builder()
                .id(existingId).tenantId(TENANT)
                .organizationUnitId(UUID.randomUUID())
                .spaceType(SpaceType.FAMILY).name("Famille A").code("famille-a")
                .status(SpaceStatus.ACTIVE).visiblePeopleScope(VisiblePeopleScope.CHURCH).build());

        // Le document prétend réutiliser cet UUID pour un AUTRE code.
        SpaceImportReport report = importService.apply(
                docWithSpace(spaceConfig(existingId, "famille-b", "Famille B")), TENANT);

        assertThat(report.valid()).isTrue();
        assertThat(report.counters().spacesConflicted()).isEqualTo(1);
        assertThat(report.counters().spacesCreated()).isZero();
        assertThat(report.issues()).anySatisfy(i ->
                assertThat(i.severity()).isEqualTo(SpaceImportReport.Severity.CONFLICT));
        assertThat(spaces).hasSize(1);
        assertThat(spaces.get(existingId).getCode()).isEqualTo("famille-a");
    }

    @Test
    void espaceSansCode_estRejeteSansEcriture() {
        SpaceImportReport report = importService.apply(
                docWithSpace(spaceConfig(UUID.randomUUID(), null, "Sans code")), TENANT);

        assertThat(report.valid()).isFalse();
        assertThat(report.applied()).isFalse();
        assertThat(spaces).isEmpty();
            assertThat(report.issues()).anySatisfy(i ->
                assertThat(i.severity()).isEqualTo(SpaceImportReport.Severity.ERROR));
    }

    @Test
    void moduleOrphelin_estRejeteEtNonEcrit() {
        SpaceExportBundle bundle = new SpaceExportBundle(
                SpaceExportBundle.FORMAT, SpaceExportBundle.VERSION, Instant.now(),
                SpaceExportBundle.SCOPE_SPACE, null, null,
                List.of(), List.of(), List.of(),
                List.of(new SpaceExportBundle.ModuleConfig(
                        UUID.randomUUID(), UUID.randomUUID(), "tasks", true, 0, Map.of(), Map.of())),
                List.of(), Map.of());

        SpaceImportReport report = importService.apply(bundle, TENANT);

        assertThat(report.valid()).isFalse();
        assertThat(modules).isEmpty();
        assertThat(report.issues()).anySatisfy(i ->
                assertThat(i.message()).contains("orphelin"));
    }

    @Test
    void documentExportePourUnAutreTenant_estRefuse() {
        SpaceExportBundle foreign = new SpaceExportBundle(
                SpaceExportBundle.FORMAT, SpaceExportBundle.VERSION, Instant.now(),
                SpaceExportBundle.SCOPE_TENANT,
                new SpaceExportBundle.TenantRef(OTHER_TENANT, null, null),
                spaceConfig(UUID.randomUUID(), "chorale", "Chorale"),
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());

        SpaceImportReport report = importService.apply(foreign, TENANT);

        assertThat(report.valid()).isFalse();
        assertThat(report.applied()).isFalse();
        assertThat(spaces).isEmpty();
        assertThat(writes.get()).isZero();
    }

    @Test
    void exportTenant_neContientQueLesDonneesDuTenantCible() {
        UUID tenantSpace = seedSpaceGraph();
        UUID otherSpaceId = UUID.randomUUID();
        spaces.put(otherSpaceId, Space.builder()
                .id(otherSpaceId).tenantId(OTHER_TENANT)
                .organizationUnitId(UUID.randomUUID()).spaceType(SpaceType.FAMILY)
                .name("Autre église").code("autre").status(SpaceStatus.ACTIVE)
                .visiblePeopleScope(VisiblePeopleScope.CHURCH).build());

        SpaceExportBundle tenantBundle = exportService.exportTenant(TENANT);

        assertThat(tenantBundle.scope()).isEqualTo(SpaceExportBundle.SCOPE_TENANT);
        assertThat(tenantBundle.tenant().id()).isEqualTo(TENANT);
        assertThat(tenantBundle.spaces()).hasSize(1);
        assertThat(tenantBundle.spaces().get(0).id()).isEqualTo(tenantSpace);
        assertThat(tenantBundle.data()).containsKeys("souls", "events");
    }
}