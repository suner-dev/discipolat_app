package com.discipolat.modules.exports.domain;

import com.discipolat.modules.customfields.domain.CustomFieldDefinition;
import com.discipolat.modules.customfields.domain.CustomFieldDefinitionRepository;
import com.discipolat.modules.events.domain.ChurchEvent;
import com.discipolat.modules.events.repository.ChurchEventRepository;
import com.discipolat.modules.exports.domain.SpaceExportBundle.CustomFieldConfig;
import com.discipolat.modules.exports.domain.SpaceExportBundle.ModuleConfig;
import com.discipolat.modules.exports.domain.SpaceExportBundle.SpaceConfig;
import com.discipolat.modules.exports.domain.SpaceExportBundle.StatusConfig;
import com.discipolat.modules.exports.domain.SpaceExportBundle.TenantRef;
import com.discipolat.modules.exports.domain.SpaceExportBundle.WorkflowConfig;
import com.discipolat.modules.exports.domain.SpaceExportBundle.WorkflowStepConfig;
import com.discipolat.modules.exports.domain.SpaceExportBundle.WorkflowTransitionConfig;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.spaces.domain.Space;
import com.discipolat.modules.spaces.domain.SpaceRepository;
import com.discipolat.modules.statuses.domain.CustomStatus;
import com.discipolat.modules.statuses.domain.CustomStatusRepository;
import com.discipolat.modules.tenants.domain.SpaceModule;
import com.discipolat.modules.tenants.domain.SpaceModuleRepository;
import com.discipolat.modules.workflow.domain.WorkflowDefinition;
import com.discipolat.modules.workflow.domain.WorkflowDefinitionRepository;
import com.discipolat.modules.workflow.domain.WorkflowStep;
import com.discipolat.modules.workflow.domain.WorkflowStepRepository;
import com.discipolat.modules.workflow.domain.WorkflowTransition;
import com.discipolat.modules.workflow.domain.WorkflowTransitionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * G4.5 — Constitution d'un document d'export canonique {@code space_export_v1}.
 *
 * <p>Deux scopes :
 * <ul>
 *   <li>{@link #exportSpace(UUID, UUID)} : un espace (config + modules + statuts + workflows de l'espace) ;</li>
 *   <li>{@link #exportTenant(UUID)} : tout un tenant (tous les espaces + référentiel de
 *       configuration + données filtrées). Réservé au super admin côté contrôleur.</li>
 * </ul>
 *
 * <p>Le service n'applique pas la règle d'autorisation lui-même : elle est du ressort de
 * l'appelant (contrôleur en {@code @PreAuthorize}).
 */
@Service
@Transactional(readOnly = true)
public class SpaceExportService {

    private final SpaceRepository spaceRepository;
    private final SpaceModuleRepository spaceModuleRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final CustomStatusRepository customStatusRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final SoulRepository soulRepository;
    private final ChurchEventRepository churchEventRepository;
    private final ObjectMapper objectMapper;

    public SpaceExportService(SpaceRepository spaceRepository,
                              SpaceModuleRepository spaceModuleRepository,
                              CustomFieldDefinitionRepository customFieldDefinitionRepository,
                              CustomStatusRepository customStatusRepository,
                              WorkflowDefinitionRepository workflowDefinitionRepository,
                              WorkflowStepRepository workflowStepRepository,
                              WorkflowTransitionRepository workflowTransitionRepository,
                              SoulRepository soulRepository,
                              ChurchEventRepository churchEventRepository,
                              ObjectMapper objectMapper) {
        this.spaceRepository = spaceRepository;
        this.spaceModuleRepository = spaceModuleRepository;
        this.customFieldDefinitionRepository = customFieldDefinitionRepository;
        this.customStatusRepository = customStatusRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.soulRepository = soulRepository;
        this.churchEventRepository = churchEventRepository;
        this.objectMapper = objectMapper;
    }

    /** Exporte la configuration d'un espace unique (aucune donnée nominative). */
    public SpaceExportBundle exportSpace(UUID tenantId, UUID spaceId) {
        Space space = spaceRepository.findByIdAndTenantId(spaceId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Espace introuvable: " + spaceId));

        List<WorkflowDefinition> workflows = workflowDefinitionRepository
                .findByTenantIdAndDeletedAtIsNull(tenantId).stream()
                .filter(w -> spaceId.equals(w.getSpaceId()))
                .toList();

        return new SpaceExportBundle(
                SpaceExportBundle.FORMAT,
                SpaceExportBundle.VERSION,
                Instant.now(),
                SpaceExportBundle.SCOPE_SPACE,
                null,
                toSpaceConfig(space),
                List.of(),
                List.of(),
                statusesFor(tenantId, spaceId),
                modulesFor(tenantId, spaceId),
                workflowsFor(workflows),
                Map.of());
    }

    /**
     * Exporte l'intégralité d'un tenant : tous les espaces, le référentiel de configuration
     * partagé et les données (âmes, événements) filtrées par le scope tenant.
     *
     * <p>Ce scope est réservé au super admin plateforme (contrôleur {@code @PreAuthorize}).
     */
    public SpaceExportBundle exportTenant(UUID tenantId) {
        List<Space> spaces = spaceRepository.findByTenantIdAndDeletedAtIsNull(tenantId);

        List<CustomFieldDefinition> fields = customFieldDefinitionRepository.findAll().stream()
                .filter(f -> tenantId.equals(f.getTenantId()))
                .sorted((a, b) -> Integer.compare(a.getOrdre(), b.getOrdre()))
                .toList();

        List<CustomStatus> tenantStatuses = customStatusRepository
                .findByTenantIdAndDeletedAtIsNullOrderByEntityTypeAscDisplayOrderAsc(tenantId);

        List<WorkflowDefinition> workflows = workflowDefinitionRepository.findByTenantIdAndDeletedAtIsNull(tenantId);

        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        data.put("souls", soulsData(tenantId));
        data.put("events", eventsData(tenantId));

        return new SpaceExportBundle(
                SpaceExportBundle.FORMAT,
                SpaceExportBundle.VERSION,
                Instant.now(),
                SpaceExportBundle.SCOPE_TENANT,
                new TenantRef(tenantId, null, null),
                null,
                spaces.stream().map(this::toSpaceConfig).toList(),
                fields.stream().map(this::toCustomFieldConfig).toList(),
                tenantStatuses.stream().map(this::toStatusConfig).toList(),
                spaces.stream().flatMap(s -> modulesFor(tenantId, s.getId()).stream()).toList(),
                workflowsFor(workflows),
                data);
    }

    /** Sérialise un bundle en JSON canonique. */
    public byte[] toJson(SpaceExportBundle bundle) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(bundle.normalized());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation de l'export impossible", e);
        }
    }

    /** Reconstruit le nom de fichier canonique d'un export. */
    public String fileName(String prefix) {
        return prefix + "_" + SpaceExportBundle.FORMAT + ".json";
    }

    private SpaceConfig toSpaceConfig(Space s) {
        return new SpaceConfig(
                s.getId(),
                s.getOrganizationUnitId(),
                s.getSpaceType() == null ? null : s.getSpaceType().name(),
                s.getTemplateCode(),
                s.getName(),
                s.getCode(),
                s.getIcon(),
                s.getColor(),
                s.getDescription(),
                s.getStatus() == null ? null : s.getStatus().name(),
                s.getVisiblePeopleScope() == null ? null : s.getVisiblePeopleScope().name(),
                s.getConfigurationJson() == null ? Map.of() : s.getConfigurationJson());
    }

    private List<ModuleConfig> modulesFor(UUID tenantId, UUID spaceId) {
        return spaceModuleRepository.findByTenantIdAndSpaceId(tenantId, spaceId).stream()
                .sorted((a, b) -> Integer.compare(
                        a.getDisplayOrder() == null ? 0 : a.getDisplayOrder(),
                        b.getDisplayOrder() == null ? 0 : b.getDisplayOrder()))
                .map(m -> new ModuleConfig(
                        m.getId(),
                        m.getSpaceId(),
                        m.getModuleCode(),
                        m.getEnabled(),
                        m.getDisplayOrder(),
                        m.getConfigurationJson() == null ? Map.of() : m.getConfigurationJson(),
                        m.getLimitsJson() == null ? Map.of() : m.getLimitsJson()))
                .toList();
    }

    private List<StatusConfig> statusesFor(UUID tenantId, UUID spaceId) {
        return customStatusRepository.findByTenantIdAndSpaceIdAndDeletedAtIsNull(tenantId, spaceId).stream()
                .map(this::toStatusConfig)
                .toList();
    }

    private StatusConfig toStatusConfig(CustomStatus c) {
        return new StatusConfig(
                c.getId(),
                c.getSpaceId(),
                c.getEntityType(),
                c.getCode(),
                c.getName(),
                c.getColor(),
                c.getIcon(),
                c.getDisplayOrder(),
                c.getInitial(),
                c.getFinalStatus(),
                c.getAllowedTransitions() == null ? List.of() : c.getAllowedTransitions());
    }

    private CustomFieldConfig toCustomFieldConfig(CustomFieldDefinition f) {
        return new CustomFieldConfig(
                f.getId(),
                f.getEntiteType(),
                f.getCode(),
                f.getLabel(),
                f.getType(),
                f.isObligatoire(),
                f.getOrdre(),
                f.getOptions() == null ? List.of() : f.getOptions(),
                f.getPlaceholder(),
                f.getDefaultValue(),
                f.getRolesLecture() == null ? List.of() : f.getRolesLecture(),
                f.getRolesEcriture() == null ? List.of() : f.getRolesEcriture(),
                f.isActif());
    }

    private List<WorkflowConfig> workflowsFor(List<WorkflowDefinition> workflows) {
        List<WorkflowConfig> result = new ArrayList<>();
        for (WorkflowDefinition w : workflows) {
            List<WorkflowStep> steps = workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(w.getId());
            List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(w.getId());
            result.add(new WorkflowConfig(
                    w.getId(),
                    w.getSpaceId(),
                    w.getEntityType(),
                    w.getCode(),
                    w.getName(),
                    w.getEnabled(),
                    w.getVersion(),
                    steps.stream().map(s -> new WorkflowStepConfig(
                            s.getId(),
                            s.getStepOrder(),
                            s.getStepType() == null ? null : s.getStepType().name(),
                            s.getName(),
                            s.getConditionsJson() == null ? Map.of() : s.getConditionsJson(),
                            s.getAssigneeRole(),
                            s.getAssigneeScope(),
                            s.getTimeoutHours(),
                            s.getEscalationRole(),
                            s.getAutoActionJson() == null ? Map.of() : s.getAutoActionJson())).toList(),
                    transitions.stream().map(t -> new WorkflowTransitionConfig(
                            t.getId(), t.getFromStepId(), t.getToStepId(), t.getOnEvent())).toList()));
        }
        return result;
    }

    private List<Map<String, Object>> soulsData(UUID tenantId) {
        List<Soul> souls = soulRepository.findByTenantId(tenantId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Soul s : souls) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId() == null ? null : s.getId().toString());
            m.put("nom", s.getNom());
            m.put("prenom", s.getPrenom());
            m.put("email", s.getEmail());
            m.put("telephone", s.getTelephone());
            m.put("typeDisciple", s.getTypeDisciple() == null ? null : s.getTypeDisciple().name());
            m.put("statut", s.getStatut() == null ? null : s.getStatut().name());
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> eventsData(UUID tenantId) {
        List<ChurchEvent> events =
                churchEventRepository.findByTenantIdAndDeletedAtIsNullOrderByStartAtAsc(tenantId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (ChurchEvent e : events) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId() == null ? null : e.getId().toString());
            m.put("title", e.getTitle());
            m.put("type", e.getType());
            m.put("status", e.getStatus());
            m.put("startAt", e.getStartAt() == null ? null : e.getStartAt().toString());
            m.put("endAt", e.getEndAt() == null ? null : e.getEndAt().toString());
            m.put("visibility", e.getVisibility());
            out.add(m);
        }
        return out;
    }
}