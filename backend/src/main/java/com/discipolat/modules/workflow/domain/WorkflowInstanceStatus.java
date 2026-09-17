package com.discipolat.modules.workflow.domain;

/** G2.5 — Cycle de vie technique d'une instance de workflow. */
public enum WorkflowInstanceStatus {
    RUNNING,
    APPROVED,
    REJECTED,
    ESCALATED,
    CANCELLED,
    COMPLETED
}
