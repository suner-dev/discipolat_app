package com.discipolat.modules.imports.domain;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResult {
    private int imported;
    private int skipped;
    private List<String> errors;
    private List<UUID> importedIds;
}