package com.discipolat.modules.tasks.domain;

import com.discipolat.common.domain.BusinessRuleException;

/**
 * Types de tâche — miroir exact de l'énumération mobile
 * {@code TaskType} ({@code task_model.g.dart} : TASK, SUBTASK, EPIC,
 * STORY, BUG, FEATURE, CHORES, MEETING, CALL, REVIEW).
 */
public enum TaskType {
    TASK, SUBTASK, EPIC, STORY, BUG, FEATURE, CHORES, MEETING, CALL, REVIEW;

    /** Parse tolérant à la casse, refuse toute chaîne inconnue (400). */
    public static TaskType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return TASK;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Type de tâche inconnu : " + raw, "TASK_TYPE_UNKNOWN");
        }
    }
}
