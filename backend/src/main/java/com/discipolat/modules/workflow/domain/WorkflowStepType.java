package com.discipolat.modules.workflow.domain;

/** G2.5 — Nature d'une étape de workflow (annexe A §A.3). */
public enum WorkflowStepType {
    APPROVAL,
    AUTO_ACTION,
    NOTIFY,
    EXPENSE,
    ASSET_STATUS,
    FORM
}
