package com.discipolat.common.enums;
public enum TypeNotification {
    RAPPORT_NON_SOUMIS, ABSENCE_48H, RAPPORT_FAMILLE_NON_SOUMIS, ALERTE_ABSENCE, INFORMATION, PRIERE_EXAUCEE,
    // Transferts (workflow configurable)
    TRANSFERT_DEMANDE, TRANSFERT_VALIDATION, TRANSFERT_VALIDEE, TRANSFERT_REFUSEE,
    TRANSFERT_INFOS_DEMANDEES, TRANSFERT_CORRECTION, TRANSFERT_EXECUTEE, TRANSFERT_ANNULEE,
    TRANSFERT_DELAI_DEPASSE,
    // Department Management System
    MEMBRE_AJOUTE, MEMBRE_RETIRE, TACHE_ASSIGNEE, TACHE_EN_RETARD, MEMBRE_AFFECTE,
    // Événements
    EVENEMENT_RAPPEL,
    // G5.4 — « Demander l'accès » depuis les gardes frontend (§55-2)
    DEMANDE_ACCES,
    // G5.7 — conflit LWW détecté à l'application d'une écriture hors-ligne
    OFFLINE_CONFLIT,
    // G4.1 — rappel quotidien des suivis dus/en retard (famille)
    SUIVI_RAPPEL,
    // G4.3 — nomination/transfert pastoral d'un berger
    PASTORAT_NOMINATION
}
