package com.discipolat.modules.platform.api;

/**
 * Source de données exploitable par un bloc du Page Builder. Toutes les
 * sources sont résolues côté serveur sur les entités réelles (aucune
 * statistique fictive), et scopées selon l'espace métier de l'utilisateur.
 *
 * @param key         clé technique (ex. SOULS_TOTAL)
 * @param label       libellé affiché dans l'éditeur
 * @param type        type de bloc compatible : KPI / TABLEAU / LISTE
 * @param description aide à la saisie
 * @param sensitive   vrai si la valeur n'est résolue que pour les super-utilisateurs
 */
public record PageDataSource(String key, String label, String type, String description, boolean sensitive) {
}
