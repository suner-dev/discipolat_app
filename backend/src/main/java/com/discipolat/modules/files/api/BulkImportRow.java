package com.discipolat.modules.files.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * Représente une ligne brute d'import CSV (familles, users ou souls).
 * Chaque champ est optionnel sauf les champs obligatoires par type.
 */
public record BulkImportRow(
    int rowNumber,
    Map<String, String> data
) {
    public String get(String key) {
        return data.getOrDefault(key, "").trim();
    }

    public boolean isBlank(String key) {
        String val = get(key);
        return val == null || val.isBlank();
    }
}
