package com.discipolat.common.enums;

/**
 * Mode de validation d'un circuit de validation configurable.
 * Choisi par le pasteur dans l'administration du workflow.
 */
public enum ValidationMode {
    /** Les étapes sont validées dans l'ordre défini (chaîne de validation). */
    SEQUENTIEL,
    /** Toutes les étapes peuvent être validées dans n'importe quel ordre. */
    PARALLELE,
    /** Un nombre donné de validations suffit (nombreValidationsRequises). */
    N_VALIDATIONS_REQUISES
}
