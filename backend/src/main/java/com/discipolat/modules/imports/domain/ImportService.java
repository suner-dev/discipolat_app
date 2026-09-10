package com.discipolat.modules.imports.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final SoulRepository soulRepository;
    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    public ImportService(SoulRepository soulRepository,
                         FamilyRepository familyRepository,
                         UserRepository userRepository,
                         SecurityUtils securityUtils,
                         PasswordEncoder passwordEncoder) {
        this.soulRepository = soulRepository;
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.passwordEncoder = passwordEncoder;
    }

    public ImportValidationResult validate(ImportType type, MultipartFile file) throws IOException {
        List<Map<String, String>> rows = parseCsv(file);
        return switch (type) {
            case SOULS -> validateSouls(rows);
            case FAMILIES -> validateFamilies(rows);
            case USERS -> validateUsers(rows);
            default -> ImportValidationResult.builder()
                    .success(false)
                    .totalRows(rows.size())
                    .validRows(0)
                    .invalidRows(rows.size())
                    .errors(List.of(new ImportValidationError(0, "type", "Type d'import non supporté: " + type, "")))
                    .build();
        };
    }

    public ImportResult importData(ImportType type, MultipartFile file) throws IOException {
        List<Map<String, String>> rows = parseCsv(file);
        return switch (type) {
            case SOULS -> importSouls(rows);
            case FAMILIES -> importFamilies(rows);
            case USERS -> importUsers(rows);
            default -> ImportResult.builder()
                    .imported(0)
                    .skipped(rows.size())
                    .errors(List.of("Type d'import non supporté: " + type))
                    .build();
        };
    }

    private List<Map<String, String>> parseCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return List.of();

            String[] headers = headerLine.split(",");
            List<Map<String, String>> result = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",", -1);
                Map<String, String> row = new LinkedHashMap<>();
                for (int j = 0; j < headers.length && j < values.length; j++) {
                    row.put(headers[j].trim().toLowerCase(), values[j].trim());
                }
                result.add(row);
            }
            return result;
        }
    }

    private ImportValidationResult validateSouls(List<Map<String, String>> rows) {
        List<ImportValidationError> errors = new ArrayList<>();
        List<Map<String, Object>> preview = new ArrayList<>();
        AtomicInteger validCount = new AtomicInteger();

        for (int i = 0; i < rows.size() && i < 10; i++) {
            Map<String, String> row = rows.get(i);
            Map<String, Object> previewRow = new LinkedHashMap<>(row);
            previewRow.put("rowNumber", i + 1);
            preview.add(previewRow);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 1;
            boolean rowValid = true;

            String nom = row.get("nom");
            String prenom = row.get("prenom");
            String telephone = row.get("telephone");
            String email = row.get("email");
            String dateNaissance = row.get("datenaissance");
            String sexe = row.get("sexe");

            if (nom == null || nom.isBlank()) {
                errors.add(new ImportValidationError(rowNum, "nom", "Le nom est obligatoire", nom));
                rowValid = false;
            }
            if (prenom == null || prenom.isBlank()) {
                errors.add(new ImportValidationError(rowNum, "prenom", "Le prénom est obligatoire", prenom));
                rowValid = false;
            }
            if (telephone != null && !telephone.isBlank() && !telephone.matches("^[+]?[0-9\\s\\-]{8,}$")) {
                errors.add(new ImportValidationError(rowNum, "telephone", "Format de téléphone invalide", telephone));
                rowValid = false;
            }
            if (email != null && !email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                errors.add(new ImportValidationError(rowNum, "email", "Format d'email invalide", email));
                rowValid = false;
            }
            if (dateNaissance != null && !dateNaissance.isBlank()) {
                try {
                    parseDate(dateNaissance);
                } catch (DateTimeParseException e) {
                    errors.add(new ImportValidationError(rowNum, "datenaissance", "Format de date invalide (YYYY-MM-DD)", dateNaissance));
                    rowValid = false;
                }
            }
            if (sexe != null && !sexe.isBlank() && !List.of("M", "F", "H", "FEMME", "HOMME").contains(sexe.toUpperCase())) {
                errors.add(new ImportValidationError(rowNum, "sexe", "Sexe invalide (M/F)", sexe));
                rowValid = false;
            }

            if (rowValid) validCount.incrementAndGet();
        }

        return ImportValidationResult.builder()
                .success(errors.isEmpty())
                .totalRows(rows.size())
                .validRows(validCount.get())
                .invalidRows(rows.size() - validCount.get())
                .errors(errors)
                .preview(preview)
                .build();
    }

    private ImportValidationResult validateFamilies(List<Map<String, String>> rows) {
        List<ImportValidationError> errors = new ArrayList<>();
        List<Map<String, Object>> preview = new ArrayList<>();
        AtomicInteger validCount = new AtomicInteger();

        for (int i = 0; i < rows.size() && i < 10; i++) {
            Map<String, String> row = rows.get(i);
            Map<String, Object> previewRow = new LinkedHashMap<>(row);
            previewRow.put("rowNumber", i + 1);
            preview.add(previewRow);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 1;
            boolean rowValid = true;

            String nomFamille = row.get("nomfamille");
            String telephone = row.get("telephone");

            if (nomFamille == null || nomFamille.isBlank()) {
                errors.add(new ImportValidationError(rowNum, "nomfamille", "Le nom de famille est obligatoire", nomFamille));
                rowValid = false;
            }
            if (telephone != null && !telephone.isBlank() && !telephone.matches("^[+]?[0-9\\s\\-]{8,}$")) {
                errors.add(new ImportValidationError(rowNum, "telephone", "Format de téléphone invalide", telephone));
                rowValid = false;
            }

            if (rowValid) validCount.incrementAndGet();
        }

        return ImportValidationResult.builder()
                .success(errors.isEmpty())
                .totalRows(rows.size())
                .validRows(validCount.get())
                .invalidRows(rows.size() - validCount.get())
                .errors(errors)
                .preview(preview)
                .build();
    }

    private ImportValidationResult validateUsers(List<Map<String, String>> rows) {
        List<ImportValidationError> errors = new ArrayList<>();
        List<Map<String, Object>> preview = new ArrayList<>();
        AtomicInteger validCount = new AtomicInteger();

        for (int i = 0; i < rows.size() && i < 10; i++) {
            Map<String, String> row = rows.get(i);
            Map<String, Object> previewRow = new LinkedHashMap<>(row);
            previewRow.put("rowNumber", i + 1);
            preview.add(previewRow);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 1;
            boolean rowValid = true;

            String email = row.get("email");
            String firstName = row.get("firstname");
            String lastName = row.get("lastname");
            String role = row.get("role");

            if (email == null || email.isBlank() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                errors.add(new ImportValidationError(rowNum, "email", "Email valide obligatoire", email));
                rowValid = false;
            }
            if (firstName == null || firstName.isBlank()) {
                errors.add(new ImportValidationError(rowNum, "firstname", "Prénom obligatoire", firstName));
                rowValid = false;
            }
            if (lastName == null || lastName.isBlank()) {
                errors.add(new ImportValidationError(rowNum, "lastname", "Nom obligatoire", lastName));
                rowValid = false;
            }
            if (role != null && !role.isBlank()) {
                try {
                    UserRole.valueOf(role.toUpperCase());
                } catch (IllegalArgumentException e) {
                    errors.add(new ImportValidationError(rowNum, "role", "Rôle invalide: " + role, role));
                    rowValid = false;
                }
            }

            if (rowValid) validCount.incrementAndGet();
        }

        return ImportValidationResult.builder()
                .success(errors.isEmpty())
                .totalRows(rows.size())
                .validRows(validCount.get())
                .invalidRows(rows.size() - validCount.get())
                .errors(errors)
                .preview(preview)
                .build();
    }

    private ImportResult importSouls(List<Map<String, String>> rows) {
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        List<UUID> importedIds = new ArrayList<>();

        UUID tenantId = securityUtils.getCurrentTenantId();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 1;

            try {
                Soul soul = Soul.builder()
                        .nom(row.get("nom"))
                        .prenom(row.get("prenom"))
                        .telephone(row.get("telephone"))
                        .email(row.get("email"))
                        .adresse(row.get("adresse"))
                        .dateNaissance(parseDate(row.get("datenaissance")))
                        .typeDisciple(TypeDisciple.NOUVEAU_CONVERTI)
                        .statut(com.discipolat.common.enums.StatutAme.EN_INTEGRATION)
                        .tenantId(tenantId)
                        .etatSpirituel("NOUVEAU_CONVERTI")
                        .niveauCroissance(1)
                        .build();

                Soul saved = soulRepository.save(soul);
                imported++;
                importedIds.add(saved.getId());
            } catch (Exception e) {
                skipped++;
                errors.add("Ligne " + rowNum + ": " + e.getMessage());
            }
        }

        return ImportResult.builder()
                .imported(imported)
                .skipped(skipped)
                .errors(errors)
                .importedIds(importedIds)
                .build();
    }

    private ImportResult importFamilies(List<Map<String, String>> rows) {
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        UUID tenantId = securityUtils.getCurrentTenantId();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 1;

            try {
                Family family = Family.builder()
                        .nom(row.get("nomfamille"))
                        .tenantId(tenantId)
                        .niveauRisque(com.discipolat.common.enums.NiveauRisque.NORMAL)
                        .build();

                familyRepository.save(family);
                imported++;
            } catch (Exception e) {
                skipped++;
                errors.add("Ligne " + rowNum + ": " + e.getMessage());
            }
        }

        return ImportResult.builder()
                .imported(imported)
                .skipped(skipped)
                .errors(errors)
                .build();
    }

    private ImportResult importUsers(List<Map<String, String>> rows) {
        int imported = 0;
        int skipped = 0;
        int usersWithTempPassword = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 1;

            try {
                String email = row.get("email");
                if (userRepository.existsByEmail(email)) {
                    skipped++;
                    errors.add("Ligne " + rowNum + ": Email déjà existant");
                    continue;
                }

                // Mot de passe temporaire aléatoire, encodé en BCrypt (à réinitialiser à la première connexion)
                String tempPassword = "ChangeMe!" + UUID.randomUUID();
                User user = User.builder()
                        .email(email)
                        .firstName(row.get("firstname"))
                        .lastName(row.get("lastname"))
                        .role(UserRole.valueOf(row.getOrDefault("role", "MEMBRE").toUpperCase()))
                        .passwordHash(passwordEncoder.encode(tempPassword))
                        .build();

                userRepository.save(user);
                imported++;
                usersWithTempPassword++;
            } catch (Exception e) {
                skipped++;
                errors.add("Ligne " + rowNum + ": " + e.getMessage());
            }
        }

        log.info("Import de {} utilisateur(s) avec mot de passe temporaire — à réinitialiser à la première connexion", usersWithTempPassword);

        return ImportResult.builder()
                .imported(imported)
                .skipped(skipped)
                .errors(errors)
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        throw new DateTimeParseException("Format de date non reconnu", dateStr, 0);
    }

    private String parseSexe(String sexe) {
        if (sexe == null || sexe.isBlank()) return "M";
        String s = sexe.toUpperCase();
        return switch (s) {
            case "F", "FEMME" -> "F";
            case "H", "HOMME" -> "M";
            default -> "M";
        };
    }
}