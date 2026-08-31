package com.discipolat.modules.demo.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps d'une demande de démonstration soumise depuis la landing page
 * publique (sans authentification). Validation stricte côté serveur :
 * c'est un endpoint exposé, il doit être immunisé contre le spam de données.
 */
public record CreateDemoRequestRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String churchName,
        @Size(max = 100) String role,
        @Size(max = 5000) String message,
        @Size(max = 100) String source
) {}