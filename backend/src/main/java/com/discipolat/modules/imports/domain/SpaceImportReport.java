package com.discipolat.modules.imports.domain;

import java.util.List;

/**
 * G4.5 — Rapport d'import d'un document canonique {@code space_export_v1}.
 *
 * <p>Exigence : « validation + rapport précis (ligne/erreur) ; import idempotent
 * (UUID clients préservés, conflits signalés) ; aucun écrasement silencieux ».
 *
 * <p>{@code applied=false} signifie que l'import a été exécuté en <b>validation seule</b>
 * (dry-run) ou qu'il a été rejeté : dans les deux cas <b>aucune</b> écriture n'a eu lieu.
 * Une ligne signalée en {@code CONFLICT} n'est jamais écrasée : elle est comptée et listée.
 */
public record SpaceImportReport(
        boolean valid,
        boolean applied,
        boolean dryRun,
        String format,
        Integer version,
        String scope,
        Counters counters,
        List<SpaceImportIssue> issues
) {

    /** Compteurs par section (créés / mis à jour / inchangés / conflits). */
    public record Counters(
            int spacesCreated,
            int spacesUpdated,
            int spacesUnchanged,
            int spacesConflicted,
            int modulesUpserted,
            int customFieldsCreated,
            int customFieldsUpdated,
            int customFieldsConflicted,
            int statusesCreated,
            int statusesUpdated,
            int workflowsCreated,
            int workflowsUpdated
    ) {
        public static Counters empty() {
            return new Counters(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public Counters plusSpacesCreated(int n) { return new Counters(spacesCreated + n, spacesUpdated, spacesUnchanged, spacesConflicted, modulesUpserted, customFieldsCreated, customFieldsUpdated, customFieldsConflicted, statusesCreated, statusesUpdated, workflowsCreated, workflowsUpdated); }
        public Counters plusSpacesUpdated(int n) { return new Counters(spacesCreated, spacesUpdated + n, spacesUnchanged, spacesConflicted, modulesUpserted, customFieldsCreated, customFieldsUpdated, customFieldsConflicted, statusesCreated, statusesUpdated, workflowsCreated, workflowsUpdated); }
        public Counters plusSpacesUnchanged(int n) { return new Counters(spacesCreated, spacesUpdated, spacesUnchanged + n, spacesConflicted, modulesUpserted, customFieldsCreated, customFieldsUpdated, customFieldsConflicted, statusesCreated, statusesUpdated, workflowsCreated, workflowsUpdated); }
        public Counters plusSpacesConflicted(int n) { return new Counters(spacesCreated, spacesUpdated, spacesUnchanged, spacesConflicted + n, modulesUpserted, customFieldsCreated, customFieldsUpdated, customFieldsConflicted, statusesCreated, statusesUpdated, workflowsCreated, workflowsUpdated); }
        public Counters plusModules(int n) { return new Counters(spacesCreated, spacesUpdated, spacesUnchanged, spacesConflicted, modulesUpserted + n, customFieldsCreated, customFieldsUpdated, customFieldsConflicted, statusesCreated, statusesUpdated, workflowsCreated, workflowsUpdated); }
        public Counters plusCustomFieldsCreated(int n) { return new Counters(spacesCreated, spacesUpdated, spacesUnchanged, spacesConflicted, modulesUpserted, customFieldsCreated + n, customFieldsUpdated, customFieldsConflicted, statusesCreated, statusesUpdated, workflowsCreated, workflowsUpdated); }
        public Counters plusCustomFieldsUpdated(int n) { return new Counters(spacesCreated, spacesUpdated, spacesUnchanged, spacesConflicted, modulesUpserted, customFieldsCreated, customFieldsUpdated + n, customFieldsConflicted, statusesCreated, statusesUpdated, workflowsCreated, workflowsUpdated); }
        public Counters plusCustomFieldsConflicted(int n) { return new Counters(spacesCreated, spacesUpdated, spacesUnchanged, spacesConflicted, modulesUpserted, customFieldsCreated, customFieldsUpdated, customFieldsConflicted + n, statusesCreated, statusesUpdated, workflowsCreated, workflowsUpdated); }
        public Counters plusStatusesCreated(int n) { return new Counters(spacesCreated, spacesUpdated, spacesUnchanged, spacesConflicted, modulesUpserted, customFieldsCreated, customFieldsUpdated, customFieldsConflicted, statusesCreated + n, statusesUpdated, workflowsCreated, workflowsUpdated); }
        public Counters plusStatusesUpdated(int n) { return new Counters(spacesCreated, spacesUpdated, spacesUnchanged, spacesConflicted, modulesUpserted, customFieldsCreated, customFieldsUpdated, customFieldsConflicted, statusesCreated, statusesUpdated + n, workflowsCreated, workflowsUpdated); }
        public Counters plusWorkflowsCreated(int n) { return new Counters(spacesCreated, spacesUpdated, spacesUnchanged, spacesConflicted, modulesUpserted, customFieldsCreated, customFieldsUpdated, customFieldsConflicted, statusesCreated, statusesUpdated, workflowsCreated + n, workflowsUpdated); }
        public Counters plusWorkflowsUpdated(int n) { return new Counters(spacesCreated, spacesUpdated, spacesUnchanged, spacesConflicted, modulesUpserted, customFieldsCreated, customFieldsUpdated, customFieldsConflicted, statusesCreated, statusesUpdated, workflowsCreated, workflowsUpdated + n); }
    }

    /** Niveau d'une anomalie d'import. */
    public enum Severity { ERROR, WARNING, CONFLICT }

    /** Anomalie localisée : section du document, référence de la ligne, message lisible. */
    public record SpaceImportIssue(String section, String reference, Severity severity, String message) {}

    public static SpaceImportReport rejected(String message) {
        return new SpaceImportReport(false, false, false, null, null, null, Counters.empty(),
                List.of(new SpaceImportIssue("document", "-", Severity.ERROR, message)));
    }
}