package com.discipolat.modules.platform.api;

import java.util.Map;

/**
 * Bloc résolu pour le rendu d'une page personnalisée : le type et la
 * configuration proviennent de la page stockée, les données (data) sont
 * résolues côté serveur sur les entités réelles selon l'espace métier
 * de l'utilisateur.
 *
 * @param type   KPI / TABLEAU / LISTE / TEXTE / LIENS / RECHERCHE / IMAGES
 * @param config configuration du bloc (saisie par l'administrateur)
 * @param data   données résolues (null si le bloc n'a pas de source)
 */
public record ResolvedBlock(String type, Map<String, Object> config, Map<String, Object> data) {
}
