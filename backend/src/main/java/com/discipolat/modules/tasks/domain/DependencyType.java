package com.discipolat.modules.tasks.domain;

import com.discipolat.common.domain.BusinessRuleException;

/**
 * Types de dépendance — miroir exact de {@code DependencyType} mobile
 * (BLOCKS, IS_BLOCKED_BY, RELATES_TO, DUPLICATES, IS_DUPLICATED_BY).
 */
public enum DependencyType {
    BLOCKS, IS_BLOCKED_BY, RELATES_TO, DUPLICATES, IS_DUPLICATED_BY;

    public static DependencyType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return RELATES_TO;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Type de dépendance inconnu : " + raw, "TASK_DEPENDENCY_TYPE_UNKNOWN");
        }
    }
}
