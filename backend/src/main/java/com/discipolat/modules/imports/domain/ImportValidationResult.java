package com.discipolat.modules.imports.domain;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportValidationResult {
    private boolean success;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private List<ImportValidationError> errors;
    private List<Map<String, Object>> preview;
}