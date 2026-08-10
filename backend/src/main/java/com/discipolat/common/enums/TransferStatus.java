package com.discipolat.common.enums;

/**
 * Cycle de vie d'une demande de transfert.
 * Chaque transition d'état est enregistrée dans l'historique (TransferHistory)
 * et dans le journal d'audit.
 */
public enum TransferStatus {
    /** Brouillon : la demande est en cours de rédaction, pas encore soumise. */
    BROUILLON,
    /** Soumise : en attente d'une action du demandeur (correction, informations). */
    SOUMIS,
    /** En attente de validation : le circuit de validation a démarré. */
    EN_ATTENTE_VALIDATION,
    /** Validation partielle : au moins une validation obtenue, d'autres restent requises. */
    VALIDATION_PARTIELLE,
    /** Validée : toutes les validations requises ont été obtenues. */
    VALIDE,
    /** Refusée : un validateur a refusé la demande. */
    REFUSE,
    /** Annulée : annulée par le demandeur (ou l'admin/pasteur). */
    ANNULE,
    /** Exécutée : le transfert a été appliqué automatiquement aux données. */
    EXECUTE,
    /** Archivée : demande close, conservée pour l'historique. */
    ARCHIVE
}
