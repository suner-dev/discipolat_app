package com.discipolat.modules.finances.api;

import com.discipolat.modules.finances.domain.FinanceTransaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceTransactionRequest(
        @NotNull FinanceTransaction.TransactionType type,
        String categorie,
        @NotNull @DecimalMin("0.00") BigDecimal montant,
        String description,
        LocalDate dateTransaction
) {
}
