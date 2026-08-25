package com.discipolat.common.infrastructure.config;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.members.domain.MemberDepartment;
import com.discipolat.modules.members.domain.MemberDepartmentRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEFAULT_PASSWORD = "password123";

    // Identifiants stables définis dans V2__seed_data.sql
    private static final UUID CHEF1_ID = UUID.fromString("a0000000-0000-0000-0000-000000000004");
    private static final UUID FAMILLE_TIMOTHEE_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SoulRepository soulRepository;
    private final DepartmentRepository departmentRepository;
    private final MemberDepartmentRepository memberDepartmentRepository;

    /**
     * Activation du jeu de données de démonstration (comptes connus / mot de
     * passe public). VRAI uniquement sur l'environnement bêta (profil `beta`).
     * En production, AUCUN compte de démonstration n'est créé ni activé :
     * on ne mélange jamais données réelles et données de test.
     */
    @Value("${app.beta-testing.seed-demo-accounts:false}")
    private boolean seedDemoAccounts;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           SoulRepository soulRepository,
                           DepartmentRepository departmentRepository,
                           MemberDepartmentRepository memberDepartmentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.soulRepository = soulRepository;
        this.departmentRepository = departmentRepository;
        this.memberDepartmentRepository = memberDepartmentRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);
        int updatedCount = 0;

        // Migration de données (applicable dans tous les environnements) :
        // s'assure que les utilisateurs réels ont bien leur ensemble de rôles.
        for (User user : userRepository.findAll()) {
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                user.getRoles().add(user.getRole());
                if (user.isEstChefDeFamille()) {
                    user.getRoles().add(UserRole.CHEF_DE_FAMILLE);
                }
                if (user.getActiveRole() == null) {
                    user.setActiveRole(user.getRole());
                }
                userRepository.save(user);
            }
        }

        if (!seedDemoAccounts) {
            log.info("ℹ️ Comptes de démonstration désactivés sur cet environnement (seed-demo-accounts=false).");
            return;
        }

        for (User user : userRepository.findAll()) {
            if ("PLACEHOLDER".equals(user.getPasswordHash())) {
                user.setPasswordHash(encodedPassword);
                userRepository.save(user);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            log.info("✅ Initialized {} user accounts with default password (demo only)", updatedCount);
        }

        // Admin — multi-role: ADMIN + PASTEUR
        seedUser("admin@discipolat.com", "Admin", "System",
                UserRole.ADMIN, Set.of(UserRole.ADMIN, UserRole.PASTEUR),
                UserRole.ADMIN, false);

        // Pasteur — single role
        seedUser("pasteur@discipolat.com", "Pierre", "Pasteur",
                UserRole.PASTEUR, Set.of(UserRole.PASTEUR),
                UserRole.PASTEUR, false);

        // Responsable — multi-role: RESPONSABLE + FAISEUR (manages a department + disciples)
        seedUser("responsable@discipolat.com", "Rachel", "Responsable",
                UserRole.RESPONSABLE, Set.of(UserRole.RESPONSABLE, UserRole.FAISEUR),
                UserRole.RESPONSABLE, false);

        // Chef de famille — multi-role: FAISEUR + CHEF_DE_FAMILLE
        seedUser("chef@discipolat.com", "Jean", "ChefDeFamille",
                UserRole.FAISEUR, Set.of(UserRole.FAISEUR, UserRole.CHEF_DE_FAMILLE),
                UserRole.CHEF_DE_FAMILLE, true);

        // Faiseur — single role
        seedUser("faiseur@discipolat.com", "Fabrice", "Faiseur",
                UserRole.FAISEUR, Set.of(UserRole.FAISEUR),
                UserRole.FAISEUR, false);

        // Membre — single role
        seedUser("membre@discipolat.com", "Moïse", "Membre",
                UserRole.MEMBRE, Set.of(UserRole.MEMBRE),
                UserRole.MEMBRE, false);

        // Multi-role demo: Paul — RESPONSABLE + CHEF_DE_FAMILLE + FAISEUR
        seedUser("paul@discipolat.com", "Paul", "Apôtre",
                UserRole.FAISEUR, Set.of(UserRole.RESPONSABLE, UserRole.CHEF_DE_FAMILLE, UserRole.FAISEUR),
                UserRole.RESPONSABLE, true);

        // Espace Membre : âme liée au compte membre + départements ministères
        seedMemberSpace();
    }

    /**
     * Seed de l'Espace Membre pour le compte de démonstration membre@discipolat.com :
     * crée l'âme liée au compte (famille, faiseur, date d'arrivée) et l'affecte
     * aux départements Chorale et Audiovisuel.
     */
    private void seedMemberSpace() {
        User membre = userRepository.findByEmail("membre@discipolat.com").orElse(null);
        if (membre == null) return;

        Soul soul = soulRepository.findAllByUserId(membre.getId()).stream()
                .filter(s -> !s.isDeleted())
                .findFirst()
                .orElse(null);

        if (soul == null) {
            soul = Soul.builder()
                    .nom(membre.getLastName() != null ? membre.getLastName() : "Membre")
                    .prenom(membre.getFirstName())
                    .email(membre.getEmail())
                    .telephone(membre.getPhone())
                    .dateNaissance(membre.getDateNaissance())
                    .profession("Étudiant")
                    .niveauEtude("Licence")
                    .nbEnfants(0)
                    .typeDisciple(TypeDisciple.NOUVEL_ARRIVANT)
                    .dateIntegration(LocalDate.now().minusMonths(8))
                    .statut(StatutAme.ACTIF)
                    .faiseurId(CHEF1_ID)
                    .familleId(FAMILLE_TIMOTHEE_ID)
                    .userId(membre.getId())
                    .etatSpirituel("EN_CROISSANCE")
                    .niveauCroissance(2)
                    .situationFamiliale("CELIBATAIRE")
                    .build();
            soul = soulRepository.save(soul);
            log.info("✅ Created member soul for {} / {}", membre.getEmail(), DEFAULT_PASSWORD);
        }

        final Soul memberSoul = soul;
        for (String deptNom : List.of("Chorale", "Audiovisuel")) {
            departmentRepository.findByNom(deptNom).ifPresent(dept -> {
                if (!memberDepartmentRepository.existsBySoulIdAndDepartmentId(memberSoul.getId(), dept.getId())) {
                    memberDepartmentRepository.save(MemberDepartment.builder()
                            .soulId(memberSoul.getId())
                            .departmentId(dept.getId())
                            .build());
                    log.info("✅ Membre {} affecté au département {}", membre.getEmail(), deptNom);
                }
            });
        }
    }

    private void seedUser(String email, String firstName, String lastName,
                          UserRole primaryRole, Set<UserRole> roles,
                          UserRole activeRole, boolean estChefDeFamille) {
        if (userRepository.findByEmail(email).isPresent()) return;
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                .firstName(firstName)
                .lastName(lastName)
                .role(primaryRole)
                .roles(roles)
                .activeRole(activeRole)
                .statut(UserStatus.ACTIVE)
                .estChefDeFamille(estChefDeFamille)
                .build();
        userRepository.save(user);
        log.info("✅ Created {} (roles={}) user: {}", primaryRole, roles, email);
    }
}
