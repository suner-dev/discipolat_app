package com.discipolat.modules.objectives.domain;

/** Types d'objectifs mesurés automatiquement par l'application. */
public enum ObjectiveType {
    VISITES,          // nombre d'interactions de type VISITE
    NOUVELLES_AMES,   // âmes intégrées dans la période
    DISCIPLES_ACTIFS, // âmes actives
    EVANGELISATION,   // âmes ayant atteint l'étape BAPTEME dans la période
    SUIVIS,           // interactions de type SUIVI
    PRESENCE          // taux de présence moyen
}
