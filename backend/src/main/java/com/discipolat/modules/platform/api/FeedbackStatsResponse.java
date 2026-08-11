package com.discipolat.modules.platform.api;

import java.util.Map;

/**
 * Statistiques agrégées des retours testeurs (compteurs par statut et
 * par catégorie) pour le panneau d'administration.
 */
public record FeedbackStatsResponse(
        long total,
        long nouveaux,
        long enCours,
        long resolus,
        long rejetes,
        Map<String, Long> parCategorie
) {}
