package com.discipolat.modules.config.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.config.domain.CustomFieldDefinition;
import com.discipolat.modules.config.domain.CustomFieldValue;
import com.discipolat.modules.config.repository.CustomFieldDefinitionRepository;
import com.discipolat.modules.config.repository.CustomFieldValueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
@Transactional
public class CustomFieldService {

    private final CustomFieldDefinitionRepository definitionRepository;
    private final CustomFieldValueRepository valueRepository;

    public CustomFieldService(CustomFieldDefinitionRepository definitionRepository,
                              CustomFieldValueRepository valueRepository) {
        this.definitionRepository = definitionRepository;
        this.valueRepository = valueRepository;
    }

    // ========== DEFINITIONS ==========

    public List<CustomFieldDefinition> getDefinitions(UUID tenantId, String entityType, UUID spaceId) {
        if (spaceId != null) {
            return definitionRepository.findByTenantIdAndEntityTypeAndSpaceIdAndDeletedAtIsNullOrderByDisplayOrderAsc(tenantId, entityType, spaceId);
        }
        return definitionRepository.findByTenantIdAndEntityTypeAndDeletedAtIsNullOrderByDisplayOrderAsc(tenantId, entityType);
    }

    public List<CustomFieldDefinition> getResolvedDefinitions(UUID tenantId, String entityType, UUID spaceId) {
        return definitionRepository.findResolvedDefinitions(tenantId, entityType, spaceId);
    }

    public CustomFieldDefinition getDefinition(UUID id) {
        return definitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CustomFieldDefinition", id));
    }

    public CustomFieldDefinition createDefinition(CustomFieldDefinition def) {
        validateDefinition(def);
        if (def.getSpaceId() != null) {
            if (definitionRepository.existsByTenantIdAndEntityTypeAndSpaceIdAndFieldCode(
                    def.getTenantId(), def.getEntityType(), def.getSpaceId(), def.getFieldCode())) {
                throw new IllegalArgumentException("Un champ avec ce code existe déjà pour cet espace");
            }
        } else {
            if (definitionRepository.existsByTenantIdAndEntityTypeAndSpaceIdAndFieldCode(
                    def.getTenantId(), def.getEntityType(), null, def.getFieldCode())) {
                throw new IllegalArgumentException("Un champ tenant avec ce code existe déjà");
            }
        }
        return definitionRepository.save(def);
    }

    public CustomFieldDefinition updateDefinition(UUID id, CustomFieldDefinition updated) {
        CustomFieldDefinition existing = getDefinition(id);
        existing.setLabel(updated.getLabel());
        existing.setFieldType(updated.getFieldType());
        existing.setRequired(updated.getRequired());
        existing.setOptionsJson(updated.getOptionsJson());
        existing.setValidationJson(updated.getValidationJson());
        existing.setVisibilityScope(updated.getVisibilityScope());
        existing.setDisplayOrder(updated.getDisplayOrder());
        validateDefinition(existing);
        return definitionRepository.save(existing);
    }

    public void deleteDefinition(UUID id) {
        CustomFieldDefinition def = getDefinition(id);
        def.setDeletedAt(java.time.OffsetDateTime.now());
        definitionRepository.save(def);
        // Also delete associated values
        valueRepository.deleteByFieldId(id);
    }

    // ========== VALUES ==========

    public Map<UUID, Object> getValuesForEntity(UUID tenantId, UUID entityId) {
        List<CustomFieldValue> values = valueRepository.findByEntityId(entityId);
        Map<UUID, Object> result = new LinkedHashMap<>();
        for (CustomFieldValue v : values) {
            result.put(v.getFieldId(), v.getValueJson());
        }
        return result;
    }

    public List<CustomFieldValue> getValuesForEntityWithDefinitions(UUID tenantId, UUID entityId) {
        return valueRepository.findByEntityId(entityId);
    }

    public CustomFieldValue setValue(UUID tenantId, UUID fieldId, UUID entityId, Object value, UUID updatedBy) {
        CustomFieldDefinition def = getDefinition(fieldId);
        if (!def.getTenantId().equals(tenantId)) {
            throw new SecurityException("Champ n'appartient pas au tenant");
        }

        // Validate value against definition
        Object validated = validateValue(def, value);

        Optional<CustomFieldValue> existing = valueRepository.findByFieldIdAndEntityId(fieldId, entityId);
        CustomFieldValue cfv = existing.orElseGet(() -> {
            CustomFieldValue n = new CustomFieldValue();
            n.setTenantId(tenantId);
            n.setFieldId(fieldId);
            n.setEntityId(entityId);
            return n;
        });
        cfv.setValueJson(Map.of("value", validated));
        cfv.setUpdatedBy(updatedBy);
        cfv.setUpdatedAt(java.time.OffsetDateTime.now());
        return valueRepository.save(cfv);
    }

    public void deleteValue(UUID fieldId, UUID entityId) {
        valueRepository.deleteByFieldIdAndEntityId(fieldId, entityId);
    }

    // ========== VALIDATION ==========

    private void validateDefinition(CustomFieldDefinition def) {
        // Validate field_type
        Set<String> validTypes = Set.of(
                "TEXT", "LONG_TEXT", "NUMBER", "DECIMAL", "BOOLEAN", "DATE", "DATETIME", "TIME",
                "SELECT", "MULTI_SELECT", "USER", "PERSON", "DEPARTMENT", "SPACE", "TEAM",
                "FILE", "IMAGE", "URL", "PHONE", "EMAIL", "CURRENCY"
        );
        if (!validTypes.contains(def.getFieldType())) {
            throw new IllegalArgumentException("Type de champ invalide: " + def.getFieldType());
        }

        // Validate options for SELECT/MULTI_SELECT
        if (Set.of("SELECT", "MULTI_SELECT").contains(def.getFieldType())) {
            if (def.getOptionsJson() == null || def.getOptionsJson().isEmpty()) {
                throw new IllegalArgumentException("SELECT/MULTI_SELECT nécessite des options");
            }
            for (Map<String, Object> opt : def.getOptionsJson()) {
                if (!opt.containsKey("value") || !opt.containsKey("label")) {
                    throw new IllegalArgumentException("Chaque option doit avoir 'value' et 'label'");
                }
            }
        }

        // Validate validation_json structure
        if (def.getValidationJson() != null) {
            Map<String, Object> val = def.getValidationJson();
            if (val.containsKey("minLength") && val.get("minLength") instanceof Number n && n.intValue() < 0) {
                throw new IllegalArgumentException("minLength doit être >= 0");
            }
            if (val.containsKey("maxLength") && val.get("maxLength") instanceof Number n && n.intValue() < 0) {
                throw new IllegalArgumentException("maxLength doit être >= 0");
            }
            if (val.containsKey("min") && val.containsKey("max")) {
                Number min = (Number) val.get("min");
                Number max = (Number) val.get("max");
                if (min.doubleValue() > max.doubleValue()) {
                    throw new IllegalArgumentException("min ne peut pas être > max");
                }
            }
            if (val.containsKey("pattern")) {
                try {
                    Pattern.compile((String) val.get("pattern"));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Regex invalide: " + e.getMessage());
                }
            }
        }
    }

    private Object validateValue(CustomFieldDefinition def, Object value) {
        if (value == null) {
            if (Boolean.TRUE.equals(def.getRequired())) {
                throw new IllegalArgumentException("Champ requis: " + def.getLabel());
            }
            return null;
        }

        String type = def.getFieldType();
        Map<String, Object> validation = def.getValidationJson();

        return switch (type) {
            case "TEXT", "LONG_TEXT" -> validateString((String) value, validation, type);
            case "NUMBER" -> validateNumber(value, validation, false);
            case "DECIMAL" -> validateNumber(value, validation, true);
            case "BOOLEAN" -> validateBoolean(value);
            case "DATE" -> validateDate((String) value);
            case "DATETIME" -> validateDateTime((String) value);
            case "TIME" -> validateTime((String) value);
            case "SELECT" -> validateSelect((String) value, def.getOptionsJson());
            case "MULTI_SELECT" -> validateMultiSelect((List<?>) value, def.getOptionsJson());
            case "USER", "PERSON", "DEPARTMENT", "SPACE", "TEAM" -> validateReference((String) value);
            case "FILE", "IMAGE" -> validateFile((Map<String, Object>) value);
            case "URL" -> validateUrl((String) value);
            case "PHONE" -> validatePhone((String) value);
            case "EMAIL" -> validateEmail((String) value);
            case "CURRENCY" -> validateCurrency(value, validation);
            default -> throw new IllegalArgumentException("Type non supporté: " + type);
        };
    }

    private String validateString(String value, Map<String, Object> validation, String fieldType) {
        if (value == null) return "";
        int minLen = getInt(validation, "minLength", 0);
        int maxLen = getInt(validation, "maxLength", fieldType.equals("LONG_TEXT") ? 10000 : 255);
        if (value.length() < minLen || value.length() > maxLen) {
            throw new IllegalArgumentException("Longueur invalide (min: " + minLen + ", max: " + maxLen + ")");
        }
        if (validation.containsKey("pattern")) {
            Pattern p = Pattern.compile((String) validation.get("pattern"));
            if (!p.matcher(value).matches()) {
                throw new IllegalArgumentException("Format invalide");
            }
        }
        return value;
    }

    private Object validateNumber(Object value, Map<String, Object> validation, boolean decimal) {
        Number n;
        if (value instanceof Number) {
            n = (Number) value;
        } else if (value instanceof String) {
            try {
                n = decimal ? Double.parseDouble((String) value) : Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Nombre invalide");
            }
        } else {
            throw new IllegalArgumentException("Type nombre attendu");
        }

        if (validation.containsKey("min")) {
            double min = ((Number) validation.get("min")).doubleValue();
            if (n.doubleValue() < min) throw new IllegalArgumentException("Valeur < min (" + min + ")");
        }
        if (validation.containsKey("max")) {
            double max = ((Number) validation.get("max")).doubleValue();
            if (n.doubleValue() > max) throw new IllegalArgumentException("Valeur > max (" + max + ")");
        }
        return n;
    }

    private Boolean validateBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        throw new IllegalArgumentException("Booléen attendu");
    }

    private String validateDate(String value) {
        try {
            java.time.LocalDate.parse(value);
            return value;
        } catch (Exception e) {
            throw new IllegalArgumentException("Date invalide (format YYYY-MM-DD)");
        }
    }

    private String validateDateTime(String value) {
        try {
            java.time.OffsetDateTime.parse(value);
            return value;
        } catch (Exception e) {
            throw new IllegalArgumentException("DateTime invalide (ISO 8601)");
        }
    }

    private String validateTime(String value) {
        try {
            java.time.LocalTime.parse(value);
            return value;
        } catch (Exception e) {
            throw new IllegalArgumentException("Heure invalide (format HH:MM:SS)");
        }
    }

    private String validateSelect(String value, List<Map<String, Object>> options) {
        if (value == null) return null;
        boolean valid = options.stream().anyMatch(o -> value.equals(o.get("value")));
        if (!valid) throw new IllegalArgumentException("Valeur non dans la liste");
        return value;
    }

    private List<String> validateMultiSelect(List<?> value, List<Map<String, Object>> options) {
        if (value == null) return List.of();
        Set<String> validValues = new HashSet<>();
        for (Map<String, Object> o : options) validValues.add((String) o.get("value"));
        for (Object v : value) {
            if (!validValues.contains(v.toString())) {
                throw new IllegalArgumentException("Valeur non dans la liste: " + v);
            }
        }
        return value.stream().map(Object::toString).toList();
    }

    private String validateReference(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            UUID.fromString(value);
            return value;
        } catch (Exception e) {
            throw new IllegalArgumentException("UUID invalide");
        }
    }

    private Map<String, Object> validateFile(Map<String, Object> value) {
        if (value == null) return Map.of();
        if (!value.containsKey("url") || !value.containsKey("name")) {
            throw new IllegalArgumentException("Fichier doit avoir url et name");
        }
        return value;
    }

    private String validateUrl(String value) {
        if (value == null || value.isEmpty()) return "";
        if (!value.matches("^https?://.+")) {
            throw new IllegalArgumentException("URL invalide");
        }
        return value;
    }

    private String validatePhone(String value) {
        if (value == null || value.isEmpty()) return "";
        // Basic phone validation
        if (!value.matches("^[+]?[0-9\\s\\-()]{8,}$")) {
            throw new IllegalArgumentException("Téléphone invalide");
        }
        return value;
    }

    private String validateEmail(String value) {
        if (value == null || value.isEmpty()) return "";
        if (!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email invalide");
        }
        return value;
    }

    private Object validateCurrency(Object value, Map<String, Object> validation) {
        Number n = (Number) validateNumber(value, validation, true);
        // Currency: store as decimal, ensure 2 decimal places
        return Math.round(n.doubleValue() * 100.0) / 100.0;
    }

    private int getInt(Map<String, Object> m, String key, int def) {
        if (m == null || !m.containsKey(key)) return def;
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }
}