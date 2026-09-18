package com.discipolat.modules.exports.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * G4.5 — Format canonique versionné d'export/import d'un espace ou d'un tenant.
 *
 * <p>Contrat : {@code space_export_v1}. Le document est <b>auto-descriptif</b>
 * ({@code format} + {@code version}) et contient :
 * <ul>
 *   <li>la <b>configuration</b> : espace(s), modules activés, champs personnalisés,
 *       statuts configurables, workflows (étapes + transitions) ;</li>
 *   <li>les <b>données</b> filtrées selon les droits de l'appelant (âmes, événements).</li>
 * </ul>
 *
 * <p>Règle §0.3 : ce document ne contient <b>jamais</b> de code exécutable, uniquement
 * de la configuration sérialisable (JSON canonique).
 *
 * <p>Les identifiants sont ceux des entités source : l'import les <b>préserve</b>
 * (idempotence) et signale les conflits au lieu d'écraser silencieusement.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpaceExportBundle(
        String format,
        Integer version,
        Instant exportedAt,
        String scope,
        TenantRef tenant,
        SpaceConfig space,
        List<SpaceConfig> spaces,
        List<CustomFieldConfig> customFields,
        List<StatusConfig> statuses,
        List<ModuleConfig> modules,
        List<WorkflowConfig> workflows,
        Map<String, List<Map<String, Object>>> data
) {

    public static final String FORMAT = "space_export_v1";
    public static final int VERSION = 1;
    public static final String SCOPE_SPACE = "SPACE";
    public static final String SCOPE_TENANT = "TENANT";

    /** Ramène toutes les listes à des listes vides plutôt que {@code null} (robustesse import). */
    public SpaceExportBundle normalized() {
        return new SpaceExportBundle(
                format == null ? FORMAT : format,
                version == null ? VERSION : version,
                exportedAt,
                scope,
                tenant,
                space,
                spaces == null ? List.of() : spaces,
                customFields == null ? List.of() : customFields,
                statuses == null ? List.of() : statuses,
                modules == null ? List.of() : modules,
                workflows == null ? List.of() : workflows,
                data == null ? Map.of() : data);
    }

    /** Tous les espaces portés par le document, quel que soit le scope. */
    public List<SpaceConfig> allSpaces() {
        if (spaces != null && !spaces.isEmpty()) {
            return spaces;
        }
        return space == null ? List.of() : List.of(space);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TenantRef(UUID id, String name, String code) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpaceConfig(
            UUID id,
            UUID organizationUnitId,
            String spaceType,
            String templateCode,
            String name,
            String code,
            String icon,
            String color,
            String description,
            String status,
            String visiblePeopleScope,
            Map<String, Object> configurationJson
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModuleConfig(
            UUID id,
            UUID spaceId,
            String moduleCode,
            Boolean enabled,
            Integer displayOrder,
            Map<String, Object> configurationJson,
            Map<String, Object> limitsJson
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomFieldConfig(
            UUID id,
            String entiteType,
            String code,
            String label,
            String type,
            Boolean obligatoire,
            Integer ordre,
            List<String> options,
            String placeholder,
            String defaultValue,
            List<String> rolesLecture,
            List<String> rolesEcriture,
            Boolean actif
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusConfig(
            UUID id,
            UUID spaceId,
            String entityType,
            String code,
            String name,
            String color,
            String icon,
            Integer displayOrder,
            Boolean initial,
            Boolean finalStatus,
            List<String> allowedTransitions
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WorkflowConfig(
            UUID id,
            UUID spaceId,
            String entityType,
            String code,
            String name,
            Boolean enabled,
            Integer version,
            List<WorkflowStepConfig> steps,
            List<WorkflowTransitionConfig> transitions
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WorkflowStepConfig(
            UUID id,
            Integer stepOrder,
            String stepType,
            String name,
            Map<String, Object> conditionsJson,
            String assigneeRole,
            String assigneeScope,
            Integer timeoutHours,
            String escalationRole,
            Map<String, Object> autoActionJson
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WorkflowTransitionConfig(UUID id, UUID fromStepId, UUID toStepId, String onEvent) {}
}