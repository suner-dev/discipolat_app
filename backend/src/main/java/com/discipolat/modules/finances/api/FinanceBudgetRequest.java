package com.discipolat.modules.finances.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FinanceBudgetRequest(
        @NotBlank String categorie,
        @NotNull @Min(2000) @Max(2100) Integer annee,
        @NotNull @DecimalMin("0.00") BigDecimal montant
) {
}
