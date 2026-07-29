package com.discipolat.modules.reports.api;

import jakarta.validation.constraints.NotBlank;

public record ValidateReportRequest(
        @NotBlank String validationType
) {}
