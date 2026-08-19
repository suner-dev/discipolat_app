package com.discipolat.modules.files.api;

import com.discipolat.common.domain.UserRole;
import com.discipolat.modules.files.api.BulkImportValidationResult.RowError;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Valide les lignes CSV avant import en lot.
 * Règles par type : familles, users, souls.
 */
@Component
public class BulkImportValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    // ======================== FAMILLES ========================

    public List<RowError> validateFamilies(List<BulkImportRow> rows) {
        List<RowError> errors = new ArrayList<>();

        for (BulkImportRow row : rows) {
            int n = row.rowNumber();

            // nom — requis
            if (row.isBlank("nom")) {
                errors.add(new RowError(n, "nom", "Le nom est requis"));
            } else if (row.get("nom").length() > 200) {
                errors.add(new RowError(n, "nom", "Le nom ne doit pas dépasser 200 caractères"));
            }

            // chefFamilleId — requis et UUID valide
            if (row.isBlank("chefFamilleId")) {
                errors.add(new RowError(n, "chefFamilleId", "L'ID du chef de famille est requis"));
            } else if (!UUID_PATTERN.matcher(row.get("chefFamilleId")).matches()) {
                errors.add(new RowError(n, "chefFamilleId", "Format UUID invalide : " + row.get("chefFamilleId")));
            }

            // description — optionnel, max 1000
            if (!row.isBlank("description") && row.get("description").length() > 1000) {
                errors.add(new RowError(n, "description", "La description ne doit pas dépasser 1000 caractères"));
            }

            // Risque — optionnel, valeur autorisée
            if (!row.isBlank("risque")) {
                String risque = row.get("risque").toUpperCase();
                if (!Set.of("NORMAL", "A_RISQUE", "SOUS_SURVEILLANCE").contains(risque)) {
                    errors.add(new RowError(n, "risque", "Valeur invalide : " + row.get("risque") + " (NORMAL, A_RISQUE, SOUS_SURVEILLANCE)"));
                }
            }
        }

        return errors;
    }

    // ======================== USERS ========================

    public List<RowError> validateUsers(List<BulkImportRow> rows) {
        List<RowError> errors = new ArrayList<>();
        Set<String> emailsSeen = new HashSet<>();

        for (BulkImportRow row : rows) {
            int n = row.rowNumber();

            // email — requis, format valide
            if (row.isBlank("email")) {
                errors.add(new RowError(n, "email", "L'email est requis"));
            } else if (!EMAIL_PATTERN.matcher(row.get("email")).matches()) {
                errors.add(new RowError(n, "email", "Format email invalide : " + row.get("email")));
            } else if (!emailsSeen.add(row.get("email").toLowerCase())) {
                errors.add(new RowError(n, "email", "Email dupliqué dans l'import : " + row.get("email")));
            }

            // firstName — requis
            if (row.isBlank("firstName")) {
                errors.add(new RowError(n, "firstName", "Le prénom est requis"));
            } else if (row.get("firstName").length() > 100) {
                errors.add(new RowError(n, "firstName", "Le prénom ne doit pas dépasser 100 caractères"));
            }

            // lastName — requis
            if (row.isBlank("lastName")) {
                errors.add(new RowError(n, "lastName", "Le nom est requis"));
            } else if (row.get("lastName").length() > 100) {
                errors.add(new RowError(n, "lastName", "Le nom ne doit pas dépasser 100 caractères"));
            }

            // role — requis, valeur autorisée
            if (row.isBlank("role")) {
                errors.add(new RowError(n, "role", "Le rôle est requis"));
            } else {
                try {
                    UserRole.valueOf(row.get("role").toUpperCase());
                } catch (IllegalArgumentException e) {
                    errors.add(new RowError(n, "role", "Rôle invalide : " + row.get("role") +
                        " (ADMIN, PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE, FAISEUR, MEMBRE)"));
                }
            }

            // phone — optionnel, format basique
            if (!row.isBlank("phone") && row.get("phone").length() > 20) {
                errors.add(new RowError(n, "phone", "Le téléphone ne doit pas dépasser 20 caractères"));
            }

            // familyId — optionnel, UUID valide si fourni
            if (!row.isBlank("familyId") && !UUID_PATTERN.matcher(row.get("familyId")).matches()) {
                errors.add(new RowError(n, "familyId", "Format UUID invalide : " + row.get("familyId")));
            }
        }

        return errors;
    }

    // ======================== SOULS ========================

    public List<RowError> validateSouls(List<BulkImportRow> rows) {
        List<RowError> errors = new ArrayList<>();

        for (BulkImportRow row : rows) {
            int n = row.rowNumber();

            // nom — requis
            if (row.isBlank("nom")) {
                errors.add(new RowError(n, "nom", "Le nom est requis"));
            } else if (row.get("nom").length() > 100) {
                errors.add(new RowError(n, "nom", "Le nom ne doit pas dépasser 100 caractères"));
            }

            // prenom — requis
            if (row.isBlank("prenom")) {
                errors.add(new RowError(n, "prenom", "Le prénom est requis"));
            } else if (row.get("prenom").length() > 100) {
                errors.add(new RowError(n, "prenom", "Le prénom ne doit pas dépasser 100 caractères"));
            }

            // faiseurId — requis et UUID valide
            if (row.isBlank("faiseurId")) {
                errors.add(new RowError(n, "faiseurId", "L'ID du faiseur est requis"));
            } else if (!UUID_PATTERN.matcher(row.get("faiseurId")).matches()) {
                errors.add(new RowError(n, "faiseurId", "Format UUID invalide : " + row.get("faiseurId")));
            }

            // email — optionnel, format valide si fourni
            if (!row.isBlank("email") && !EMAIL_PATTERN.matcher(row.get("email")).matches()) {
                errors.add(new RowError(n, "email", "Format email invalide : " + row.get("email")));
            }

            // telephone — optionnel, max 20
            if (!row.isBlank("telephone") && row.get("telephone").length() > 20) {
                errors.add(new RowError(n, "telephone", "Le téléphone ne doit pas dépasser 20 caractères"));
            }

            // familleId — optionnel, UUID valide si fourni
            if (!row.isBlank("familleId") && !UUID_PATTERN.matcher(row.get("familleId")).matches()) {
                errors.add(new RowError(n, "familleId", "Format UUID invalide : " + row.get("familleId")));
            }

            // typeDisciple — optionnel, valeur autorisée
            if (!row.isBlank("typeDisciple")) {
                String type = row.get("typeDisciple").toUpperCase();
                if (!Set.of("NOUVEL_ARRIVANT", "NOUVEAU_CONVERTI", "DISCIPLE", "LEADER").contains(type)) {
                    errors.add(new RowError(n, "typeDisciple", "Type invalide : " + row.get("typeDisciple") +
                        " (NOUVEL_ARRIVANT, NOUVEAU_CONVERTI, DISCIPLE, LEADER)"));
                }
            }
        }

        return errors;
    }
}
