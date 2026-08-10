package com.discipolat.common.enums;

/**
 * Types de transfert gérés par le moteur de workflow configurable.
 * Chaque type a un circuit de validation propre, paramétrable par le pasteur
 * (rôles initiateurs, étapes de validation, règles d'exécution).
 */
public enum TransferType {
    // ---- Départements ----
    /** Transfert d'un membre (âme) vers un autre département. */
    MEMBRE_DEPARTEMENT_TRANSFERT,
    /** Ajout d'un membre dans un nouveau département. */
    MEMBRE_DEPARTEMENT_AJOUT,
    /** Retrait d'un membre d'un département. */
    MEMBRE_DEPARTEMENT_RETRAIT,

    // ---- Familles de disciples ----
    /** Transfert d'un disciple (âme) vers une autre famille. */
    DISCIPLE_FAMILLE_TRANSFERT,
    /** Transfert d'un faiseur de disciple vers une autre famille. */
    FAISEUR_FAMILLE_TRANSFERT,
    /** Transfert d'un chef de famille vers une autre famille. */
    CHEF_FAMILLE_TRANSFERT,

    // ---- Affectations ----
    /** Changement du faiseur responsable d'un disciple. */
    FAISEUR_DISCIPLE_CHANGEMENT,
    /** Changement du responsable principal d'un département. */
    RESPONSABLE_DEPARTEMENT_CHANGEMENT,
    /** Changement du chef adjoint d'une famille. */
    CHEF_ADJOINT_CHANGEMENT
}
