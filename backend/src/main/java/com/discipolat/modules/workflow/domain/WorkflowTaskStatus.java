package com.discipolat.modules.workflow.domain;

/** G2.5 — État d'une tâche d'approbation. */
public enum WorkflowTaskStatus {
    PENDING,
    APPROVED,
    REJECTED,
    ESCALATED,
    CANCELLED
}
