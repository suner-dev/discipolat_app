package com.discipolat.modules.authentication.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Demande publique de création d'une organisation.
 * Le compte, le tenant et le rôle ne sont créés qu'après approbation Super Admin.
 *
 * <p>RGPD : les consentements CGU / confidentialité / données religieuses
 * (art. 9) sont obligatoires à la souscription et horodatés avec la version
 * des documents acceptés (preuve conservée, art. 7).</p>
 */
public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100)
        String lastName,

        String phone,

        String plan,

        @NotNull(message = "Le consentement aux conditions générales est requis")
        @AssertTrue(message = "Vous devez accepter les conditions générales d'utilisation")
        Boolean consentCgu,

        @NotNull(message = "Le consentement à la politique de confidentialité est requis")
        @AssertTrue(message = "Vous devez accepter la politique de confidentialité")
        Boolean consentPrivacy,

        @NotNull(message = "Le consentement au traitement des données religieuses est requis (RGPD art. 9)")
        @AssertTrue(message = "Vous devez consentir au traitement des données religieuses (RGPD art. 9)")
        Boolean consentArt9,

        /** Version des documents acceptés (optionnelle : résolue côté serveur). */
        String legalVersion
) {
}