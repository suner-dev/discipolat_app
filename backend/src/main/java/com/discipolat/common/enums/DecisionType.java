package com.discipolat.common.enums;

/**
 * Décisions qu'un validateur peut prendre sur une demande de transfert.
 * Toutes les décisions sont motivées et historisées.
 */
public enum DecisionType {
    /** Approbation : la demande avance dans le circuit de validation. */
    APPROBATION,
    /** Refus : la demande est rejetée, fin du circuit. */
    REFUS,
    /** Demande d'informations complémentaires : le demandeur doit compléter la demande. */
    DEMANDE_INFORMATIONS,
    /** Renvoi pour correction : le demandeur doit corriger la demande. */
    RENVOI_CORRECTION
}
