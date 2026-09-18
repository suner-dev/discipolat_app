package com.discipolat.modules.imports.domain;

import com.discipolat.modules.customfields.domain.CustomFieldDefinition;
import com.discipolat.modules.customfields.domain.CustomFieldDefinitionRepository;
import com.discipolat.modules.exports.domain.SpaceExportBundle;
import com.discipolat.modules.exports.domain.SpaceExportBundle.CustomFieldConfig;
import com.discipolat.modules.exports.domain.SpaceExportBundle.ModuleConfig;
import com.discipolat.modules.exports.domain.SpaceExportBundle.SpaceConfig;
import com.discipolat.modules.exports.domain.SpaceExportBundle.StatusConfig;
import com.discipolat.modules.exports.domain.SpaceExportBundle.WorkflowConfig;
import com.discipolat.modules.exports.domain.SpaceExportBundle.WorkflowStepConfig;
import com.discipolat.modules.exports.domain.SpaceExportBundle.WorkflowTransitionConfig;
import com.discipolat.modules.imports.domain.SpaceImportReport.Counters;
import com.discipolat.modules.imports.domain.SpaceImportReport.Severity;
import com.discipolat.modules.imports.domain.SpaceImportReport.SpaceImportIssue;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * G4.5 — Import d'un document canonique {@code space_export_v1}.
 *
 * <p>Garanties exigées par la DoD :
 * <ul>
 *   <li><b>Validation</b> : le document est contrôlé (format, version, champs obligatoires) et
 *       chaque anomalie est reportée avec sa section et sa référence ;</li>
 *   <li><b>Dry-run</b> : {@link #validate} n'écrit jamais (aucun {@code save} n'est appelé) ;</li>
 *   <li><b>Idempotence</b> : les identifiants du document sont préservés ; une seconde
 *       importation du même document ne crée aucun doublon (tout est « inchangé ») ;</li>
 *   <li><b>Aucun écrasement silencieux</b> : un conflit d'identifiant (même UUID, code
 *       différent) est signalé en {@code CONFLICT} et la ligne est laissée intacte.</li>
 * </ul>
 */
@Service
@Transactional
public class SpaceImportService {

    private static final Logger log = LoggerFactory.getLogger(SpaceImportService.class);

    private final SpaceRepository spaceRepository;
    private final SpaceModuleRepository spaceModuleRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final CustomStatusRepository customStatusRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final ObjectMapper objectMapper;

    public SpaceImportService(SpaceRepository spaceRepository,
                              SpaceModuleRepository spaceModuleRepository,
                              CustomFieldDefinitionRepository customFieldDefinitionRepository,
                              CustomStatusRepository customStatusRepository,
                              WorkflowDefinitionRepository workflowDefinitionRepository,
                              WorkflowStepRepository workflowStepRepository,
                              WorkflowTransitionRepository workflowTransitionRepository,
                              ObjectMapper objectMapper) {
        this.spaceRepository = spaceRepository;
        this.spaceModuleRepository = spaceModuleRepository;
        this.customFieldDefinitionRepository = customFieldDefinitionRepository;
        this.customStatusRepository = customStatusRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.objectMapper = objectMapper;
    }

    /** Décode un document JSON canonique. Toute anomalie structurelle est une erreur bloquante. */
    public SpaceExportBundle parse(byte[] json) {
        if (json == null || json.length == 0) {
            throw new IllegalArgumentException("Document d'import vide");
        }
        try {
            return objectMapper.readValue(json, SpaceExportBundle.class).normalized();
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON invalide: " + e.getMessage(), e);
        }
    }

    /** Dry-run : valide le document et produit le rapport sans écrire quoi que ce soit. */
    public SpaceImportReport validate(SpaceExportBundle bundle, UUID tenantId) {
        return process(bundle, tenantId, false);
    }

    /** Applique le document (transaction unique : tout ou rien). */
    public SpaceImportReport apply(SpaceExportBundle bundle, UUID tenantId) {
        return process(bundle, tenantId, true);
    }

    private SpaceImportReport process(SpaceExportBundle raw, UUID tenantId, boolean apply) {
        if (raw == null) {
            return SpaceImportReport.rejected("Document d'import absent");
        }
        SpaceExportBundle bundle = raw.normalized();

        if (!SpaceExportBundle.FORMAT.equals(bundle.format())) {
            return SpaceImportReport.rejected(
                    "Format inattendu: " + bundle.format() + " (attendu " + SpaceExportBundle.FORMAT + ")");
        }
        if (bundle.version() == null || bundle.version() > SpaceExportBundle.VERSION) {
            return SpaceImportReport.rejected(
                    "Version d'export non supportée: " + bundle.version()
                            + " (max " + SpaceExportBundle.VERSION + ")");
        }
        // Isolation §0.3 n°3 : un document étiqueté pour un autre tenant n'est jamais appliqué.
        // Les identifiants (UUID) sont globaux : les rejouer dans un autre tenant
        // écraserait des données d'une autre église.
        if (bundle.tenant() != null && bundle.tenant().id() != null
                && !bundle.tenant().id().equals(tenantId)) {
            return SpaceImportReport.rejected(
                    "Document exporté pour le tenant " + bundle.tenant().id()
                            + " : import inter-tenant interdit");
        }

        List<SpaceImportIssue> issues = new ArrayList<>();
        Counters counters = Counters.empty();

        SpaceContext ctx = applySpaces(bundle, tenantId, apply, issues, counters);
        counters = ctx.counters();
        Map<String, UUID> spaceIdByDocId = ctx.spaceIdByDocId();

        counters = applyModules(bundle, tenantId, spaceIdByDocId, apply, issues, counters);
        counters = applyCustomFields(bundle, tenantId, apply, issues, counters);
        counters = applyStatuses(bundle, tenantId, spaceIdByDocId, apply, issues, counters);
        counters = applyWorkflows(bundle, tenantId, spaceIdByDocId, apply, issues, counters);

        if (apply && spaceIdByDocId.isEmpty() && !bundle.allSpaces().isEmpty()) {
            log.warn("Import canonique: aucun espace exploitable pour le tenant {}", tenantId);
        }

        boolean valid = issues.stream().noneMatch(i -> i.severity() == Severity.ERROR);
        return new SpaceImportReport(valid, apply && valid, !apply, bundle.format(), bundle.version(),
                bundle.scope(), counters, issues);
    }

    /** Contexte interne : compteurs accumulés + correspondance UUID document → UUID réel. */
    private record SpaceContext(Counters counters, Map<String, UUID> spaceIdByDocId) {}

    // ------------------------------------------------------------------
    // Espaces
    // ------------------------------------------------------------------

    private SpaceContext applySpaces(SpaceExportBundle bundle, UUID tenantId, boolean apply,
                                     List<SpaceImportIssue> issues, Counters counters) {
        Map<String, UUID> idByDoc = new LinkedHashMap<>();

        for (SpaceConfig c : bundle.allSpaces()) {
            String ref = ref(c.code(), c.id());
            if (isBlank(c.code())) {
                issues.add(issue("spaces", ref, "Le code de l'espace est obligatoire"));
                continue;
            }
            if (isBlank(c.name())) {
                issues.add(issue("spaces", ref, "Le nom de l'espace est obligatoire"));
                continue;
            }
            int errBefore = issues.size();
            SpaceType type = parseEnum(SpaceType.class, c.spaceType(), "spaceType", "spaces", ref, issues);
            SpaceStatus status = parseEnum(SpaceStatus.class, c.status(), "status", "spaces", ref, issues);
            VisiblePeopleScope scope = parseEnum(VisiblePeopleScope.class, c.visiblePeopleScope(),
                    "visiblePeopleScope", "spaces", ref, issues);
            if (issues.size() > errBefore) {
                continue;
            }

            // Conflit : le même UUID document existe déjà pour un AUTRE code → jamais d'écrasement.
            if (c.id() != null) {
                Optional<Space> byId = spaceRepository.findByIdAndTenantId(c.id(), tenantId);
                if (byId.isPresent() && !Objects.equals(byId.get().getCode(), c.code())) {
                    issues.add(new SpaceImportIssue("spaces", ref, Severity.CONFLICT,
                            "L'UUID " + c.id() + " est déjà utilisé par l'espace '"
                                    + byId.get().getCode() + "' ; ligne conservée telle quelle"));
                    counters = counters.plusSpacesConflicted(1);
                    idByDoc.put(c.id().toString(), byId.get().getId());
                    continue;
                }
            }

            Optional<Space> existing = spaceRepository.findByTenantIdAndCode(tenantId, c.code());
            if (existing.isPresent()) {
                Space s = existing.get();
                idByDoc.put(docKey(c), s.getId());
                if (spaceEquals(s, c)) {
                    counters = counters.plusSpacesUnchanged(1);
                } else {
                    if (apply) {
                        mutateSpace(s, c, type, status, scope);
                        spaceRepository.save(s);
                    }
                    counters = counters.plusSpacesUpdated(1);
                }
            } else {
                UUID newId = c.id();
                if (apply) {
                    Space s = Space.builder()
                            .id(newId)
                            .tenantId(tenantId)
                            .organizationUnitId(c.organizationUnitId())
                            .spaceType(type)
                            .templateCode(c.templateCode())
                            .name(c.name())
                            .code(c.code())
                            .icon(c.icon())
                            .color(c.color())
                            .description(c.description())
                            .status(status)
                            .visiblePeopleScope(scope)
                            .configurationJson(new LinkedHashMap<>(
                                    c.configurationJson() == null ? Map.of() : c.configurationJson()))
                            .build();
                    spaceRepository.save(s);
                    newId = s.getId();
                }
                idByDoc.put(docKey(c), newId != null ? newId : syntheticSpaceId(c.code()));
                counters = counters.plusSpacesCreated(1);
            }
        }

        return new SpaceContext(counters, idByDoc);
    }

    // ------------------------------------------------------------------
    // Modules d'espace
    // ------------------------------------------------------------------

    private Counters applyModules(SpaceExportBundle bundle, UUID tenantId, Map<String, UUID> spaceIdByDocId,
                                  boolean apply, List<SpaceImportIssue> issues, Counters counters) {
        for (ModuleConfig m : bundle.modules()) {
            String ref = m.moduleCode();
            if (isBlank(m.moduleCode())) {
                issues.add(issue("modules", "?", "Le code du module est obligatoire"));
                continue;
            }
            UUID spaceId = resolveSpace(m.spaceId(), spaceIdByDocId);
            if (spaceId == null) {
                issues.add(new SpaceImportIssue("modules", ref, Severity.ERROR,
                        "Module orphelin : espace cible introuvable (" + m.spaceId() + ")"));
                continue;
            }
            Optional<SpaceModule> existing =
                    spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(tenantId, spaceId, m.moduleCode());
            if (existing.isPresent()) {
                if (apply) {
                    SpaceModule sm = existing.get();
                    sm.setEnabled(m.enabled() == null ? Boolean.TRUE : m.enabled());
                    sm.setDisplayOrder(m.displayOrder() == null ? 0 : m.displayOrder());
                    sm.setConfigurationJson(jsonMap(m.configurationJson()));
                    sm.setLimitsJson(jsonMap(m.limitsJson()));
                    spaceModuleRepository.save(sm);
                }
            } else if (apply) {
                SpaceModule sm = SpaceModule.builder()
                        .id(m.id())
                        .tenantId(tenantId)
                        .spaceId(spaceId)
                        .moduleCode(m.moduleCode())
                        .enabled(m.enabled() == null ? Boolean.TRUE : m.enabled())
                        .displayOrder(m.displayOrder() == null ? 0 : m.displayOrder())
                        .configurationJson(jsonMap(m.configurationJson()))
                        .limitsJson(jsonMap(m.limitsJson()))
                        .build();
                spaceModuleRepository.save(sm);
            }
            counters = counters.plusModules(1);
        }
        return counters;
    }

    // ------------------------------------------------------------------
    // Champs personnalisés
    // ------------------------------------------------------------------

    private Counters applyCustomFields(SpaceExportBundle bundle, UUID tenantId,
                                       boolean apply, List<SpaceImportIssue> issues, Counters counters) {
        for (CustomFieldConfig f : bundle.customFields()) {
            String ref = f.entiteType() + "/" + f.code();
            if (isBlank(f.entiteType()) || isBlank(f.code())) {
                issues.add(issue("customFields", ref, "entiteType et code sont obligatoires"));
                continue;
            }
            if (isBlank(f.label()) || isBlank(f.type())) {
                issues.add(issue("customFields", ref, "label et type sont obligatoires"));
                continue;
            }
            Optional<CustomFieldDefinition> existing = customFieldDefinitionRepository
                    .findByTenantIdAndEntiteTypeAndCode(tenantId, f.entiteType(), f.code());
            if (existing.isPresent()) {
                CustomFieldDefinition d = existing.get();
                if (d.getId() != null && f.id() != null && !d.getId().equals(f.id())) {
                    issues.add(new SpaceImportIssue("customFields", ref, Severity.CONFLICT,
                            "Un champ identique existe déjà avec un autre UUID ; conservé tel quel"));
                    counters = counters.plusCustomFieldsConflicted(1);
                    continue;
                }
                if (apply) {
                    d.setLabel(f.label());
                    d.setType(f.type());
                    d.setObligatoire(Boolean.TRUE.equals(f.obligatoire()));
                    d.setOrdre(f.ordre() == null ? 0 : f.ordre());
                    d.setOptions(stringList(f.options()));
                    d.setPlaceholder(f.placeholder());
                    d.setDefaultValue(f.defaultValue());
                    d.setRolesLecture(stringList(f.rolesLecture()));
                    d.setRolesEcriture(stringList(f.rolesEcriture()));
                    d.setActif(f.actif() == null || f.actif());
                    customFieldDefinitionRepository.save(d);
                }
                counters = counters.plusCustomFieldsUpdated(1);
            } else {
                if (apply) {
                    CustomFieldDefinition d = CustomFieldDefinition.builder()
                            .id(f.id())
                            .tenantId(tenantId)
                            .entiteType(f.entiteType())
                            .code(f.code())
                            .label(f.label())
                            .type(f.type())
                            .obligatoire(Boolean.TRUE.equals(f.obligatoire()))
                            .ordre(f.ordre() == null ? 0 : f.ordre())
                            .options(stringList(f.options()))
                            .placeholder(f.placeholder())
                            .defaultValue(f.defaultValue())
                            .rolesLecture(stringList(f.rolesLecture()))
                            .rolesEcriture(stringList(f.rolesEcriture()))
                            .actif(f.actif() == null || f.actif())
                            .build();
                    customFieldDefinitionRepository.save(d);
                }
                counters = counters.plusCustomFieldsCreated(1);
            }
        }
        return counters;
    }

    // ------------------------------------------------------------------
    // Statuts configurables
    // ------------------------------------------------------------------

    private Counters applyStatuses(SpaceExportBundle bundle, UUID tenantId, Map<String, UUID> spaceIdByDocId,
                                   boolean apply, List<SpaceImportIssue> issues, Counters counters) {
        for (StatusConfig c : bundle.statuses()) {
            String ref = c.entityType() + "/" + c.code();
            if (isBlank(c.entityType()) || isBlank(c.code())) {
                issues.add(issue("statuses", ref, "entityType et code sont obligatoires"));
                continue;
            }
            UUID spaceId = resolveSpace(c.spaceId(), spaceIdByDocId);
            Optional<CustomStatus> existing = spaceId == null
                    ? customStatusRepository.findByTenantIdAndEntityTypeAndSpaceIdIsNullAndCodeAndDeletedAtIsNull(
                            tenantId, c.entityType(), c.code())
                    : customStatusRepository.findByTenantIdAndEntityTypeAndSpaceIdAndCodeAndDeletedAtIsNull(
                            tenantId, c.entityType(), spaceId, c.code());
            if (existing.isPresent()) {
                if (apply) {
                    CustomStatus s = existing.get();
                    s.setName(c.name() == null ? s.getName() : c.name());
                    s.setColor(c.color());
                    s.setIcon(c.icon());
                    s.setDisplayOrder(c.displayOrder() == null ? 0 : c.displayOrder());
                    s.setInitial(Boolean.TRUE.equals(c.initial()));
                    s.setFinalStatus(Boolean.TRUE.equals(c.finalStatus()));
                    s.setAllowedTransitions(stringList(c.allowedTransitions()));
                    customStatusRepository.save(s);
                }
                counters = counters.plusStatusesUpdated(1);
            } else {
                if (apply) {
                    CustomStatus s = CustomStatus.builder()
                            .id(c.id())
                            .tenantId(tenantId)
                            .entityType(c.entityType())
                            .spaceId(spaceId)
                            .code(c.code())
                            .name(isBlank(c.name()) ? c.code() : c.name())
                            .color(c.color())
                            .icon(c.icon())
                            .displayOrder(c.displayOrder() == null ? 0 : c.displayOrder())
                            .initial(Boolean.TRUE.equals(c.initial()))
                            .finalStatus(Boolean.TRUE.equals(c.finalStatus()))
                            .allowedTransitions(stringList(c.allowedTransitions()))
                            .build();
                    customStatusRepository.save(s);
                }
                counters = counters.plusStatusesCreated(1);
            }
        }
        return counters;
    }

    // ------------------------------------------------------------------
    // Workflows (définitions + étapes + transitions)
    // ------------------------------------------------------------------

    private Counters applyWorkflows(SpaceExportBundle bundle, UUID tenantId, Map<String, UUID> spaceIdByDocId,
                                   boolean apply, List<SpaceImportIssue> issues, Counters counters) {
        for (WorkflowConfig w : bundle.workflows()) {
            String ref = w.code();
            if (isBlank(w.code())) {
                issues.add(issue("workflows", String.valueOf(w.id()), "Le code du workflow est obligatoire"));
                continue;
            }
            UUID spaceId = resolveSpace(w.spaceId(), spaceIdByDocId);
            if (w.spaceId() != null && spaceId == null) {
                issues.add(new SpaceImportIssue("workflows", ref, Severity.ERROR,
                        "Workflow orphelin : espace cible introuvable (" + w.spaceId() + ")"));
                continue;
            }
            Optional<WorkflowDefinition> existing = spaceId == null
                    ? workflowDefinitionRepository
                            .findFirstByTenantIdAndSpaceIdIsNullAndCodeAndDeletedAtIsNullOrderByVersionDesc(tenantId, w.code())
                    : workflowDefinitionRepository
                            .findFirstByTenantIdAndSpaceIdAndCodeAndDeletedAtIsNullOrderByVersionDesc(tenantId, spaceId, w.code());
            if (existing.isPresent()) {
                WorkflowDefinition d = existing.get();
                if (workflowDiffers(d, w)) {
                    if (apply) {
                        replaceWorkflowContent(d, w);
                    }
                    counters = counters.plusWorkflowsUpdated(1);
                }
            } else {
                if (apply) {
                    WorkflowDefinition d = WorkflowDefinition.builder()
                            .id(w.id())
                            .tenantId(tenantId)
                            .entityType(isBlank(w.entityType()) ? "GENERIC" : w.entityType())
                            .code(w.code())
                            .name(isBlank(w.name()) ? w.code() : w.name())
                            .spaceId(spaceId)
                            .enabled(w.enabled() == null || w.enabled())
                            .version(w.version() == null ? 1 : w.version())
                            .build();
                    workflowDefinitionRepository.save(d);
                    writeWorkflowContent(d.getId(), w);
                }
                counters = counters.plusWorkflowsCreated(1);
            }
        }
        return counters;
    }

    private boolean workflowDiffers(WorkflowDefinition d, WorkflowConfig w) {
        if (!Objects.equals(d.getEntityType(), isBlank(w.entityType()) ? "GENERIC" : w.entityType())) return true;
        if (!Objects.equals(d.getName(), isBlank(w.name()) ? w.code() : w.name())) return true;
        if (!Objects.equals(d.getEnabled(), w.enabled() == null || w.enabled())) return true;
        List<WorkflowStep> steps = workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(d.getId());
        List<WorkflowStepConfig> docSteps = w.steps() == null ? List.of() : w.steps();
        if (steps.size() != docSteps.size()) return true;
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep s = steps.get(i);
            WorkflowStepConfig c = docSteps.get(i);
            if (!Objects.equals(s.getStepOrder(), c.stepOrder())) return true;
            if (!Objects.equals(s.getName(), c.name())) return true;
            if (!enumName(s.getStepType()).equals(normalize(c.stepType()))) return true;
        }
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(d.getId());
        List<WorkflowTransitionConfig> docTransitions = w.transitions() == null ? List.of() : w.transitions();
        return transitions.size() != docTransitions.size();
    }

    private void replaceWorkflowContent(WorkflowDefinition d, WorkflowConfig w) {
        d.setEntityType(isBlank(w.entityType()) ? "GENERIC" : w.entityType());
        d.setName(isBlank(w.name()) ? w.code() : w.name());
        d.setEnabled(w.enabled() == null || w.enabled());
        if (w.version() != null) d.setVersion(w.version());
        workflowDefinitionRepository.save(d);

        workflowTransitionRepository.deleteAll(workflowTransitionRepository.findByWorkflowId(d.getId()));
        workflowStepRepository.deleteAll(workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(d.getId()));
        workflowStepRepository.flush();
        writeWorkflowContent(d.getId(), w);
    }

    private void writeWorkflowContent(UUID workflowId, WorkflowConfig w) {
        List<WorkflowStepConfig> docSteps = w.steps() == null ? List.of() : w.steps();
        for (WorkflowStepConfig c : docSteps) {
            WorkflowStepType type = WorkflowStepType.valueOf(
                    c.stepType() == null ? WorkflowStepType.APPROVAL.name() : c.stepType());
            WorkflowStep s = WorkflowStep.builder()
                    .id(c.id())
                    .workflowId(workflowId)
                    .stepOrder(c.stepOrder() == null ? 1 : c.stepOrder())
                    .stepType(type)
                    .name(isBlank(c.name()) ? "Étape " + c.stepOrder() : c.name())
                    .conditionsJson(jsonMap(c.conditionsJson()))
                    .assigneeRole(c.assigneeRole())
                    .assigneeScope(c.assigneeScope())
                    .timeoutHours(c.timeoutHours())
                    .escalationRole(c.escalationRole())
                    .autoActionJson(jsonMap(c.autoActionJson()))
                    .build();
            workflowStepRepository.save(s);
        }
        List<WorkflowTransitionConfig> docTransitions =
                w.transitions() == null ? List.of() : w.transitions();
        for (WorkflowTransitionConfig t : docTransitions) {
            WorkflowTransition tr = WorkflowTransition.builder()
                    .id(t.id())
                    .workflowId(workflowId)
                    .fromStepId(t.fromStepId())
                    .toStepId(t.toStepId())
                    .onEvent(isBlank(t.onEvent()) ? "APPROVE" : t.onEvent())
                    .build();
            workflowTransitionRepository.save(tr);
        }
    }

    // ------------------------------------------------------------------
    // Helpers de comparaison et de normalisation
    // ------------------------------------------------------------------

    private boolean spaceEquals(Space s, SpaceConfig c) {
        return Objects.equals(s.getName(), c.name())
                && Objects.equals(s.getCode(), c.code())
                && Objects.equals(s.getIcon(), c.icon())
                && Objects.equals(s.getColor(), c.color())
                && Objects.equals(s.getDescription(), c.description())
                && Objects.equals(s.getTemplateCode(), c.templateCode())
                && enumName(s.getSpaceType()).equals(normalize(c.spaceType()))
                && enumName(s.getStatus()).equals(normalize(c.status()))
                && enumName(s.getVisiblePeopleScope()).equals(normalize(c.visiblePeopleScope()))
                && Objects.equals(jsonMap(s.getConfigurationJson()),
                        jsonMap(c.configurationJson()));
    }

    private void mutateSpace(Space s, SpaceConfig c, SpaceType type, SpaceStatus status, VisiblePeopleScope scope) {
        s.setName(c.name());
        s.setIcon(c.icon());
        s.setColor(c.color());
        s.setDescription(c.description());
        s.setTemplateCode(c.templateCode());
        if (type != null) s.setSpaceType(type);
        if (status != null) s.setStatus(status);
        if (scope != null) s.setVisiblePeopleScope(scope);
        if (c.organizationUnitId() != null) s.setOrganizationUnitId(c.organizationUnitId());
        s.setConfigurationJson(jsonMap(c.configurationJson()));
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String field,
                                            String section, String ref, List<SpaceImportIssue> issues) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException e) {
            issues.add(issue(section, ref, field + " inconnu: " + value));
            return null;
        }
    }

    private static SpaceImportIssue issue(String section, String ref, String message) {
        return new SpaceImportIssue(section, ref, Severity.ERROR, message);
    }

    private static UUID resolveSpace(UUID docSpaceId, Map<String, UUID> spaceIdByDocId) {
        if (docSpaceId == null) {
            return null;
        }
        return spaceIdByDocId.get(docSpaceId.toString());
    }

    private static UUID syntheticSpaceId(String code) {
        return UUID.nameUUIDFromBytes(("space:" + code).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static Map<String, Object> jsonMap(Map<String, Object> in) {
        return new LinkedHashMap<>(in == null ? Map.of() : in);
    }

    private static List<String> stringList(List<String> in) {
        return in == null ? new ArrayList<>() : new ArrayList<>(in);
    }

    private static String enumName(Enum<?> e) {
        return e == null ? "" : e.name();
    }

    private static String normalize(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String ref(String code, UUID id) {
        return isBlank(code) ? String.valueOf(id) : code;
    }

    private static String docKey(SpaceConfig c) {
        return c.id() != null ? c.id().toString() : "code:" + c.code();
    }
}