package com.discipolat.modules.files.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class BulkImportService {

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;
    private final SoulRepository soulRepository;

    public BulkImportService(FamilyRepository familyRepository,
                              UserRepository userRepository,
                              SoulRepository soulRepository) {
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
        this.soulRepository = soulRepository;
    }

    public Map<String, Object> importFamilies(List<Map<String, String>> families) {
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < families.size(); i++) {
            Map<String, String> row = families.get(i);
            int rowNum = i + 1;
            try {
                String nom = row.get("nom");
                if (nom == null || nom.isBlank()) {
                    errors.add("Ligne " + rowNum + ": le nom est requis");
                    continue;
                }

                String chefIdStr = row.get("chefFamilleId");
                if (chefIdStr == null || chefIdStr.isBlank()) {
                    errors.add("Ligne " + rowNum + ": l'ID du chef de famille est requis");
                    continue;
                }
                UUID chefFamilleId = UUID.fromString(chefIdStr);

                if (familyRepository.findByNom(nom).isPresent()) {
                    skipped++;
                    continue;
                }

                Family family = Family.builder()
                        .nom(nom)
                        .chefFamilleId(chefFamilleId)
                        .build();
                familyRepository.save(family);
                imported++;
            } catch (IllegalArgumentException e) {
                errors.add("Ligne " + rowNum + ": format invalide — " + e.getMessage());
            } catch (Exception e) {
                errors.add("Ligne " + rowNum + ": erreur inattendue — " + e.getMessage());
            }
        }

        return Map.of("imported", imported, "skipped", skipped, "errors", errors);
    }

    public Map<String, Object> importUsers(List<Map<String, String>> users) {
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            Map<String, String> row = users.get(i);
            int rowNum = i + 1;
            try {
                String email = row.get("email");
                if (email == null || email.isBlank()) {
                    errors.add("Ligne " + rowNum + ": l'email est requis");
                    continue;
                }

                if (userRepository.existsByEmail(email)) {
                    skipped++;
                    continue;
                }

                String roleStr = row.getOrDefault("role", "FAISEUR");
                UserRole role;
                try {
                    role = UserRole.valueOf(roleStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    errors.add("Ligne " + rowNum + ": rôle invalide « " + roleStr + " »");
                    continue;
                }

                User user = User.builder()
                        .email(email)
                        .firstName(row.getOrDefault("firstName", ""))
                        .lastName(row.getOrDefault("lastName", ""))
                        .phone(row.getOrDefault("phone", ""))
                        .passwordHash(UUID.randomUUID().toString())
                        .role(role)
                        .statut(UserStatus.PENDING_ACTIVATION)
                        .build();
                userRepository.save(user);
                imported++;
            } catch (Exception e) {
                errors.add("Ligne " + rowNum + ": erreur inattendue — " + e.getMessage());
            }
        }

        return Map.of("imported", imported, "skipped", skipped, "errors", errors);
    }

    public Map<String, Object> importSouls(List<Map<String, String>> souls) {
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < souls.size(); i++) {
            Map<String, String> row = souls.get(i);
            int rowNum = i + 1;
            try {
                String nom = row.get("nom");
                if (nom == null || nom.isBlank()) {
                    errors.add("Ligne " + rowNum + ": le nom est requis");
                    continue;
                }

                String faiseurIdStr = row.get("faiseurId");
                if (faiseurIdStr == null || faiseurIdStr.isBlank()) {
                    errors.add("Ligne " + rowNum + ": l'ID du faiseur est requis");
                    continue;
                }
                UUID faiseurId = UUID.fromString(faiseurIdStr);

                String prenom = row.getOrDefault("prenom", "");

                Soul soul = Soul.builder()
                        .nom(nom)
                        .prenom(prenom)
                        .email(row.get("email"))
                        .telephone(row.get("telephone"))
                        .typeDisciple(TypeDisciple.NOUVEL_ARRIVANT)
                        .statut(StatutAme.EN_INTEGRATION)
                        .dateIntegration(LocalDate.now())
                        .faiseurId(faiseurId)
                        .build();
                soulRepository.save(soul);
                imported++;
            } catch (IllegalArgumentException e) {
                errors.add("Ligne " + rowNum + ": format invalide — " + e.getMessage());
            } catch (Exception e) {
                errors.add("Ligne " + rowNum + ": erreur inattendue — " + e.getMessage());
            }
        }

        return Map.of("imported", imported, "skipped", skipped, "errors", errors);
    }
}
