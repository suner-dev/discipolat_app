package com.discipolat.modules.config.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class WorkflowTransitionId implements Serializable {

    private UUID fromStepId;
    private UUID toStepId;
    private String onEvent;

    public WorkflowTransitionId() {}

    public WorkflowTransitionId(UUID fromStepId, UUID toStepId, String onEvent) {
        this.fromStepId = fromStepId;
        this.toStepId = toStepId;
        this.onEvent = onEvent;
    }

    public UUID getFromStepId() { return fromStepId; }
    public void setFromStepId(UUID fromStepId) { this.fromStepId = fromStepId; }
    public UUID getToStepId() { return toStepId; }
    public void setToStepId(UUID toStepId) { this.toStepId = toStepId; }
    public String getOnEvent() { return onEvent; }
    public void setOnEvent(String onEvent) { this.onEvent = onEvent; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowTransitionId)) return false;
        WorkflowTransitionId that = (WorkflowTransitionId) o;
        return Objects.equals(fromStepId, that.fromStepId)
                && Objects.equals(toStepId, that.toStepId)
                && Objects.equals(onEvent, that.onEvent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromStepId, toStepId, onEvent);
    }
}