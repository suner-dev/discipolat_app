package com.discipolat.modules.tasks.domain;

import com.discipolat.common.domain.BusinessRuleException;

/**
 * Statuts — miroir exact de {@code TaskStatus} mobile
 * (BACKLOG, TODO, IN_PROGRESS, IN_REVIEW, BLOCKED, DONE, CANCELLED).
 *
 * <p>L'{@code order} est l'ordre de référence des colonnes Kanban
 * (dérivé de {@code TaskStatus.getOrder()} côté mobile) ; le réordonnancement
 * d'une carte s'appuie sur le champ {@code taches.ordre}.
 */
public enum TaskStatus {
    BACKLOG(0), TODO(1), IN_PROGRESS(2), IN_REVIEW(3), BLOCKED(4), DONE(5), CANCELLED(6);

    private final int order;

    TaskStatus(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }

    public static TaskStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return BACKLOG;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Statut de tâche inconnu : " + raw, "TASK_STATUS_UNKNOWN");
        }
    }
}
