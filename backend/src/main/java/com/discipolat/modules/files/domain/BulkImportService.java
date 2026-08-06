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

    public Map<String, Object> importFamilies(List<Map<String, Object>> families) {
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Map<String, Object> row : families) {
            try {
                String nom = (String) row.get("nom");
                UUID chefFamilleId = UUID.fromString((String) row.get("chefFamilleId"));

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
            } catch (Exception e) {
                errors.add("Erreur: " + e.getMessage());
            }
        }

        return Map.of("imported", imported, "skipped", skipped, "errors", errors);
    }

    public Map<String, Object> importUsers(List<Map<String, Object>> users) {
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Map<String, Object> row : users) {
            try {
                String email = (String) row.get("email");
                if (userRepository.existsByEmail(email)) {
                    skipped++;
                    continue;
                }

                User user = User.builder()
                        .email(email)
                        .firstName((String) row.getOrDefault("firstName", ""))
                        .lastName((String) row.getOrDefault("lastName", ""))
                        .phone((String) row.getOrDefault("phone", ""))
                        .passwordHash(UUID.randomUUID().toString())
                        .role(UserRole.valueOf((String) row.getOrDefault("role", "FAISEUR")))
                        .statut(UserStatus.PENDING_ACTIVATION)
                        .build();
                userRepository.save(user);
                imported++;
            } catch (Exception e) {
                errors.add("Erreur: " + e.getMessage());
            }
        }

        return Map.of("imported", imported, "skipped", skipped, "errors", errors);
    }

    public Map<String, Object> importSouls(List<Map<String, Object>> souls) {
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Map<String, Object> row : souls) {
            try {
                String nom = (String) row.get("nom");
                UUID faiseurId = UUID.fromString((String) row.get("faiseurId"));

                Soul soul = Soul.builder()
                        .nom(nom)
                        .prenom((String) row.getOrDefault("prenom", ""))
                        .email((String) row.getOrDefault("email", null))
                        .telephone((String) row.getOrDefault("telephone", null))
                        .typeDisciple(TypeDisciple.NOUVEL_ARRIVANT)
                        .statut(StatutAme.EN_INTEGRATION)
                        .dateIntegration(LocalDate.now())
                        .faiseurId(faiseurId)
                        .build();
                soulRepository.save(soul);
                imported++;
            } catch (Exception e) {
                errors.add("Erreur: " + e.getMessage());
            }
        }

        return Map.of("imported", imported, "skipped", skipped, "errors", errors);
    }
}
