package com.discipolat.modules.customfields.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestion des champs personnalisés : définitions configurées par l'admin
 * et valeurs saisies par les utilisateurs (scopées par entité).
 */
@Service
@Transactional
public class CustomFieldService {

    private final CustomFieldDefinitionRepository definitionRepository;
    private final CustomFieldValueRepository valueRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    public CustomFieldService(CustomFieldDefinitionRepository definitionRepository,
                              CustomFieldValueRepository valueRepository,
                              AuditService auditService, SecurityUtils securityUtils) {
        this.definitionRepository = definitionRepository;
        this.valueRepository = valueRepository;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
    }

    /* ======================== Définitions ======================== */

    /** Définitions actives pour une entité, filtrées par rôle de l'utilisateur courant. */
    public List<CustomFieldDefinition> getDefinitions(String entiteType) {
        String currentRole = securityUtils.getCurrentUserRole();
        return definitionRepository.findByEntiteTypeAndActifTrueOrderByOrdreAsc(entiteType).stream()
                .filter(d -> d.getRolesLecture() == null || d.getRolesLecture().isEmpty()
                        || d.getRolesLecture().contains(currentRole))
                .toList();
    }

    /** Toutes les définitions (admin). */
    public List<CustomFieldDefinition> getAllDefinitions(String entiteType) {
        return definitionRepository.findByEntiteTypeOrderByOrdreAsc(entiteType);
    }

    public CustomFieldDefinition createDefinition(CustomFieldDefinition def) {
        def.setCode(def.getCode().toUpperCase().trim().replaceAll("\\s+", "_"));
        if (def.getRolesLecture() == null) def.setRolesLecture(new ArrayList<>());
        if (def.getRolesEcriture() == null) def.setRolesEcriture(new ArrayList<>());
        definitionRepository.save(def);
        auditService.logSimple("CUSTOM_FIELD_CREATED", "CUSTOM_FIELD_DEFINITION", def.getId());
        return def;
    }

    public CustomFieldDefinition updateDefinition(UUID id, CustomFieldDefinition request) {
        CustomFieldDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Définition introuvable : " + id));
        if (request.getLabel() != null && !request.getLabel().isBlank()) def.setLabel(request.getLabel());
        if (request.getType() != null) def.setType(request.getType());
        def.setObligatoire(request.isObligatoire());
        if (request.getOptions() != null) def.setOptions(new ArrayList<>(request.getOptions()));
        if (request.getPlaceholder() != null) def.setPlaceholder(request.getPlaceholder());
        if (request.getDefaultValue() != null) def.setDefaultValue(request.getDefaultValue());
        if (request.getRolesLecture() != null) def.setRolesLecture(new ArrayList<>(request.getRolesLecture()));
        if (request.getRolesEcriture() != null) def.setRolesEcriture(new ArrayList<>(request.getRolesEcriture()));
        def.setActif(request.isActif());
        definitionRepository.save(def);
        auditService.logSimple("CUSTOM_FIELD_UPDATED", "CUSTOM_FIELD_DEFINITION", def.getId());
        return def;
    }

    public void deleteDefinition(UUID id) {
        valueRepository.findByFieldId(id).forEach(v -> valueRepository.delete(v));
        definitionRepository.deleteById(id);
        auditService.logSimple("CUSTOM_FIELD_DELETED", "CUSTOM_FIELD_DEFINITION", id);
    }

    /* ======================== Valeurs ======================== */

    /** Bundle (définitions + valeurs) pour une entité. */
    public Map<String, Object> getBundle(String entiteType, UUID entiteId) {
        List<CustomFieldDefinition> defs = getDefinitions(entiteType);
        List<CustomFieldValue> values = valueRepository.findByEntiteTypeAndEntiteId(entiteType, entiteId);
        Map<UUID, String> valueMap = values.stream()
                .collect(Collectors.toMap(CustomFieldValue::getFieldId, v -> v.getValue() != null ? v.getValue() : "", (a, b) -> b));
        List<Map<String, Object>> bundleDefs = defs.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId()); m.put("code", d.getCode()); m.put("label", d.getLabel());
            m.put("type", d.getType()); m.put("obligatoire", d.isObligatoire());
            m.put("options", d.getOptions()); m.put("placeholder", d.getPlaceholder());
            m.put("defaultValue", d.getDefaultValue()); m.put("ordre", d.getOrdre());
            m.put("value", valueMap.getOrDefault(d.getId(), d.getDefaultValue() != null ? d.getDefaultValue() : ""));
            return m;
        }).toList();
        return Map.of("definitions", bundleDefs);
    }

    /** Sauvegarde les valeurs d'une entité. */
    public void saveValues(String entiteType, UUID entiteId, Map<String, String> values) {
        for (Map.Entry<String, String> e : values.entrySet()) {
            UUID fieldId = UUID.fromString(e.getKey());
            String val = e.getValue();
            Optional<CustomFieldValue> existing = valueRepository.findByEntiteTypeAndEntiteIdAndFieldId(entiteType, entiteId, fieldId);
            if (existing.isPresent()) {
                CustomFieldValue cfv = existing.get();
                cfv.setValue(val);
                valueRepository.save(cfv);
            } else {
                valueRepository.save(CustomFieldValue.builder()
                        .entiteType(entiteType).entiteId(entiteId)
                        .fieldId(fieldId).value(val).build());
            }
        }
        auditService.logSimple("CUSTOM_FIELD_VALUES_SAVED", entiteType, entiteId);
    }
}