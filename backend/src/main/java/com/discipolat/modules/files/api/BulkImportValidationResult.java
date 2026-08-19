package com.discipolat.modules.files.api;

import java.util.List;
import java.util.Map;

/**
 * Résultat de la validation d'un import en lot.
 */
public record BulkImportValidationResult(
    boolean valid,
    int totalRows,
    int validRows,
    int invalidRows,
    List<RowError> errors
) {
    public record RowError(
        int rowNumber,
        String field,
        String message
    ) {}

    public static BulkImportValidationResult success(int totalRows) {
        return new BulkImportValidationResult(true, totalRows, totalRows, 0, List.of());
    }

    public static BulkImportValidationResult failure(int totalRows, List<RowError> errors) {
        return new BulkImportValidationResult(false, totalRows, totalRows - errors.stream()
            .mapToInt(RowError::rowNumber).distinct().toArray().length,
            (int) errors.stream().map(RowError::rowNumber).distinct().count(), errors);
    }
}
