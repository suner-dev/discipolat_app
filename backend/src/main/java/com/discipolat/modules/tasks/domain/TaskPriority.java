package com.discipolat.modules.tasks.domain;

import com.discipolat.common.domain.BusinessRuleException;

/**
 * Priorités — miroir exact de {@code TaskPriority} mobile
 * (LOW, MEDIUM, HIGH, URGENT, CRITICAL).
 */
public enum TaskPriority {
    LOW, MEDIUM, HIGH, URGENT, CRITICAL;

    public static TaskPriority from(String raw) {
        if (raw == null || raw.isBlank()) {
            return MEDIUM;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Priorité inconnue : " + raw, "TASK_PRIORITY_UNKNOWN");
        }
    }
}
