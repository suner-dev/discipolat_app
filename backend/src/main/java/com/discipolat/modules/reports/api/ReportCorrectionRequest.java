package com.discipolat.modules.reports.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record ReportCorrectionRequest(
        @NotNull UUID reportId,
        @NotNull Map<String, Object> ancienneValeur,
        @NotNull Map<String, Object> nouvelleValeur,
        @NotBlank String raison
) {}
