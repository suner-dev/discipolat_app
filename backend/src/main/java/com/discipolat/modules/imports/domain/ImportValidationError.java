package com.discipolat.modules.imports.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportValidationError {
    private int rowNumber;
    private String field;
    private String message;
    private String value;
}