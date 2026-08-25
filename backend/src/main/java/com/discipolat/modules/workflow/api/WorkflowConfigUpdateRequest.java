package com.discipolat.modules.workflow.api;

import jakarta.validation.constraints.Size;

import java.util.Map;

public record WorkflowConfigUpdateRequest(
        String label,
        String description,
        Boolean enabled,
        @Size(max = 50, message = "Too many rule entries (max 50)") Map<String, Object> rules
) {}
