package com.discipolat.modules.customfields.api;

import java.util.Map;

/**
 * Requête de sauvegarde des valeurs de champs personnalisés.
 */
public record SaveCustomFieldValuesRequest(
    Map<String, String> values
) {}
