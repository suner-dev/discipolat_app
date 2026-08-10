package com.discipolat.modules.platform.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Mise à jour des paramètres d'identité & de marque (réservée à l'administrateur).
 * Tous les champs sont optionnels : seuls ceux renseignés (non null) sont appliqués.
 */
public record UpdateChurchSettingsRequest(
        @Size(max = 255) String churchName,
        @Size(max = 255) String platformName,
        @Size(max = 255) String slogan,
        @Size(max = 2000) String description,
        @Size(max = 500) String logoUrl,
        @Size(max = 500) String faviconUrl,
        @Size(max = 500) String bannerUrl,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Couleur invalide (format hexadécimal #RRGGBB attendu)")
        String primaryColor,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Couleur invalide (format hexadécimal #RRGGBB attendu)")
        String accentColor,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Couleur invalide (format hexadécimal #RRGGBB attendu)")
        String buttonColor,
        @Size(max = 100) String fontFamily,
        Boolean allowDarkMode,
        @Size(max = 500) String address,
        @Size(max = 50) String phone,
        @Email @Size(max = 255) String email,
        @Size(max = 255) String website,
        Map<String, String> socialLinks,
        @Size(max = 2000) String contactNotes
) {}
