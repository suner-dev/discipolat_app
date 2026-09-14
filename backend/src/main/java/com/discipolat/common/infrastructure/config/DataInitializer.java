package com.discipolat.common.infrastructure.config;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.members.domain.MemberDepartment;
import com.discipolat.modules.members.domain.MemberDepartmentRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.tenants.domain.*;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantMembershipRepository membershipRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final SaasPlanRepository planRepository;
    private final TenantRepository tenantRepository;

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
                           MemberDepartmentRepository memberDepartmentRepository,
                           RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           TenantMembershipRepository membershipRepository,
                           OrganizationNodeRepository orgNodeRepository,
                           TenantSubscriptionRepository subscriptionRepository,
                           SaasPlanRepository planRepository,
                           TenantRepository tenantRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.soulRepository = soulRepository;
        this.departmentRepository = departmentRepository;
        this.memberDepartmentRepository = memberDepartmentRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.membershipRepository = membershipRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.tenantRepository = tenantRepository;
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

        // Rôles système & permissions par défaut (Sections 21-24 du prompt).
        // Applicable dans TOUS les environnements : idempotent (ne se lance que
        // si aucune table n'a encore été seedée, sinon les migrations l'ont fait).
        seedDefaultRolesAndPermissions();

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

        // Multi-tenant: seed organisations, subscriptions
        seedDefaultMemberships();
    }

    /**
     * Seed les roles systeme et permissions par defaut (Sections 21-24 du prompt).
     */
    private void seedDefaultRolesAndPermissions() {
        if (roleRepository.count() > 0) return;

        String[][] systemRoles = {
            {"PLATFORM_SUPER_ADMIN", "Super Admin Plateforme", "Administration complete de la plateforme"},
            {"TENANT_OWNER", "Proprietaire Tenant", "Proprietaire de l'organisation"},
            {"TENANT_ADMIN", "Admin Tenant", "Administrateur du tenant"},
            {"REGION_ADMIN", "Admin Region", "Administrateur d'une region"},
            {"CHURCH_ADMIN", "Admin Eglise", "Administrateur d'une eglise"},
            {"SUB_CHURCH_ADMIN", "Admin Sous-Eglise", "Administrateur d'une sous-eglise"},
            {"CAMPUS_ADMIN", "Admin Campus", "Administrateur d'un campus"},
            {"DEPARTMENT_ADMIN", "Admin Departement", "Administrateur d'un departement"},
            {"DEPARTMENT_LEADER", "Responsable Departement", "Responsable d'equipe"},
            {"FAMILY_LEADER", "Chef de Famille", "Gestionnaire de famille"},
            {"DISCIPLE_MAKER", "Faiseur de Disciples", "Accompagnement de disciples"},
            {"MENTOR", "Mentor", "Mentor de personnes assignees"},
            {"MEMBER", "Membre", "Membre standard"},
            {"GUEST", "Invite", "Acces limite"}
        };

        for (String[] roleData : systemRoles) {
            if (roleRepository.findByTenantIdAndKey(null, roleData[0]).isEmpty()) {
                Role role = Role.builder()
                        .tenantId(null).key(roleData[0]).label(roleData[1])
                        .description(roleData[2]).system(true)
                        .priority(getRolePriority(roleData[0])).build();
                roleRepository.save(role);
            }
        }

        seedSystemPermissionsAndLinks();
    }

    /**
     * Seed du catalogue de permissions système et des liens role-&gt;permission
     * (miroir idempotent des migrations V135/V136, utile quand Flyway est
     * désactivé, ex. profil test H2).
     */
    private void seedSystemPermissionsAndLinks() {
        // [key, label, category, scope]
        String[][] systemPermissions = {
                {"MEMBER_READ", "Lire membres", "MEMBERS", "TENANT"},
                {"MEMBER_CREATE", "Créer membres", "MEMBERS", "TENANT"},
                {"MEMBER_UPDATE", "Modifier membres", "MEMBERS", "TENANT"},
                {"MEMBER_DELETE", "Supprimer membres", "MEMBERS", "TENANT"},
                {"FAMILY_READ", "Lire familles", "FAMILIES", "TENANT"},
                {"FAMILY_CREATE", "Créer familles", "FAMILIES", "TENANT"},
                {"FAMILY_UPDATE", "Modifier familles", "FAMILIES", "TENANT"},
                {"FAMILY_DELETE", "Supprimer familles", "FAMILIES", "TENANT"},
                {"REPORT_READ", "Lire rapports", "REPORTS", "TENANT"},
                {"REPORT_CREATE", "Créer rapports", "REPORTS", "TENANT"},
                {"FINANCE_READ", "Lire finances", "FINANCE", "TENANT"},
                {"FINANCE_MANAGE", "Gérer finances", "FINANCE", "TENANT"},
                {"COURSE_CREATE", "Créer formations", "ACADEMY", "TENANT"},
                {"COURSE_MANAGE", "Gérer formations", "ACADEMY", "TENANT"},
                {"TENANT_SETTINGS_READ", "Lire config tenant", "SETTINGS", "TENANT"},
                {"TENANT_SETTINGS_UPDATE", "Modifier config tenant", "SETTINGS", "TENANT"},
                {"CHURCH_CREATE", "Créer églises", "ORGANIZATION", "TENANT"},
                {"CHURCH_MANAGE", "Gérer églises", "ORGANIZATION", "TENANT"},
                {"USER_INVITE", "Inviter utilisateurs", "USERS", "TENANT"},
                {"USER_MANAGE", "Gérer utilisateurs", "USERS", "TENANT"},
                {"BRANDING_READ", "Lire branding", "SETTINGS", "TENANT"},
                {"BRANDING_UPDATE", "Modifier branding", "SETTINGS", "TENANT"},
                {"AUDIT_READ", "Lire journaux d'audit", "AUDIT", "TENANT"},
        };

        Map<String, Permission> byKey = new LinkedHashMap<>();
        for (String[] perm : systemPermissions) {
            Permission permission = permissionRepository.findByKey(perm[0])
                    .orElseGet(() -> permissionRepository.save(Permission.builder()
                            .tenantId(null)
                            .key(perm[0])
                            .label(perm[1])
                            .description(perm[1])
                            .scope(PermissionScope.valueOf(perm[3]))
                            .category(perm[2])
                            .system(true)
                            .build()));
            byKey.put(perm[0], permission);
        }

        // Admin roles : toutes les permissions systèmes
        assignPermissions("PLATFORM_SUPER_ADMIN", byKey, byKey.keySet().toArray(new String[0]));
        assignPermissions("TENANT_OWNER", byKey, byKey.keySet().toArray(new String[0]));
        assignPermissions("TENANT_ADMIN", byKey, byKey.keySet().toArray(new String[0]));

        assignPermissions("REGION_ADMIN", byKey, "MEMBER_READ", "FAMILY_READ", "REPORT_READ", "REPORT_CREATE", "CHURCH_MANAGE");
        assignPermissions("CHURCH_ADMIN", byKey,
                "MEMBER_READ", "MEMBER_CREATE", "MEMBER_UPDATE", "FAMILY_READ", "FAMILY_CREATE", "FAMILY_UPDATE",
                "REPORT_READ", "REPORT_CREATE", "CHURCH_MANAGE", "CHURCH_CREATE");
        assignPermissions("SUB_CHURCH_ADMIN", byKey, "MEMBER_READ", "FAMILY_READ", "REPORT_READ");
        assignPermissions("CAMPUS_ADMIN", byKey, "MEMBER_READ", "FAMILY_READ", "REPORT_READ");
        assignPermissions("DEPARTMENT_ADMIN", byKey,
                "MEMBER_READ", "MEMBER_CREATE", "MEMBER_UPDATE", "FAMILY_READ", "FAMILY_CREATE", "FAMILY_UPDATE",
                "REPORT_READ", "REPORT_CREATE");
        assignPermissions("DEPARTMENT_LEADER", byKey, "MEMBER_READ", "FAMILY_READ", "REPORT_READ", "REPORT_CREATE");
        assignPermissions("FAMILY_LEADER", byKey, "MEMBER_READ", "FAMILY_READ", "REPORT_READ", "REPORT_CREATE");
        assignPermissions("DISCIPLE_MAKER", byKey, "MEMBER_READ", "REPORT_CREATE");
        assignPermissions("MENTOR", byKey, "MEMBER_READ", "REPORT_CREATE");
        assignPermissions("MEMBER", byKey, "MEMBER_READ", "REPORT_CREATE");
        assignPermissions("GUEST", byKey, "MEMBER_READ");
    }

    private void assignPermissions(String roleKey, Map<String, Permission> byKey, String... permissionKeys) {
        roleRepository.findByTenantIdAndKey(null, roleKey).ifPresent(role -> {
            for (String key : permissionKeys) {
                Permission permission = byKey.get(key);
                if (permission != null && role.getPermissions().stream().noneMatch(p -> p.getKey().equals(key))) {
                    role.getPermissions().add(permission);
                }
            }
            roleRepository.save(role);
        });
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
            log.info("✅ Created member soul for {}", membre.getEmail());
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
        
        // Get the default tenant (first active tenant)
        UUID tenantId = tenantRepository.findFirstByStatusOrderByCreatedAtAsc(TenantStatus.ACTIVE)
                .map(Tenant::getId)
                .orElseThrow(() -> new IllegalStateException("No active tenant found for seeding demo accounts"));
        
        User user = User.builder()
                .tenantId(tenantId)
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

    // ==================== MULTI-TENANT SEED ====================

    private int getRolePriority(String roleKey) {
        switch (roleKey) {
            case "PLATFORM_SUPER_ADMIN": return 1000;
            case "TENANT_OWNER": return 900;
            case "TENANT_ADMIN": return 800;
            case "REGION_ADMIN": return 700;
            case "CHURCH_ADMIN": return 600;
            case "SUB_CHURCH_ADMIN": return 550;
            case "CAMPUS_ADMIN": return 500;
            case "DEPARTMENT_ADMIN": return 400;
            case "DEPARTMENT_LEADER": return 350;
            case "FAMILY_LEADER": return 300;
            case "DISCIPLE_MAKER": return 200;
            case "MENTOR": return 150;
            case "MEMBER": return 100;
            case "GUEST": return 50;
            default: return 0;
        }
    }

    private void seedDefaultMemberships() {
        for (User user : userRepository.findAll()) {
            if (user.getTenantId() != null &&
                membershipRepository.findByUserIdAndTenantIdAndStatus(
                    user.getId(), user.getTenantId(), MembershipStatus.ACTIVE).isEmpty()) {
                try {
                    Role role = roleRepository.findByTenantIdAndKey(user.getTenantId(), user.getRole().name())
                            .orElseGet(() -> roleRepository.findByTenantIdIsNullAndKey(user.getRole().name()).orElse(null));
                    if (role == null) {
                        log.warn("No role found for user {} with role {}", user.getEmail(), user.getRole().name());
                        continue;
                    }
                    TenantMembership membership = TenantMembership.builder()
                            .tenantId(user.getTenantId())
                            .userId(user.getId())
                            .role(role)
                            .status(MembershipStatus.ACTIVE)
                            .build();
                    membershipRepository.save(membership);
                } catch (Exception e) {
                    log.warn("Membership pour {}: {}", user.getEmail(), e.getMessage());
                }
            }
        }
    }
}
