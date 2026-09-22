package com.discipolat.modules.dataMigration.domain;

import lombok.*;

import java.util.*;

/**
 * G4.6 — résultat d'une exécution d'analyse / d'import / de replay / de rollback.
 * DTO exposé par {@link com.discipolat.modules.dataMigration.api.DataMigrationController}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataMigrationResult {

    private UUID jobId;
    private String status;
    private boolean dryRun;
    private int totalRows;
    private int importedRows;
    private int errorRows;
    private int deleted;

    /** Log des erreurs ligne par ligne (analyse / exécution). */
    private List<String> errorsLog;

    /** Résultat d'analyse (preview des colonnes détectées). */
    private List<Map<String, String>> analysis;

    private String errorMessage;

    /** Convenience : construit un résultat d'erreur rapide. */
    public static DataMigrationResult error(UUID jobId, String message) {
        return DataMigrationResult.builder()
                .jobId(jobId)
                .status("FAILED")
                .dryRun(false)
                .errorMessage(message)
                .errorsLog(List.of(message))
                .build();
    }
}
