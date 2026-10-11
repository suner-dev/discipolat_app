package com.discipolat.common.infrastructure.config;

import com.discipolat.common.domain.UserRole;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantStatus;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.members.domain.MemberDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.tenants.domain.PermissionRepository;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.TenantSubscriptionRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Le jeu de stubs commun (roles deja seeds, aucun utilisateur) n'est pas
// consomme par les tests de refus, qui partent avant toute ecriture : la
// rigueur du test est portee par les `verify(...)` explicites, pas par le
// strictisme sur les stubs.
@MockitoSettings(strictness = Strictness.LENIENT)
class DataInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SoulRepository soulRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private MemberDepartmentRepository memberDepartmentRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private OrganizationNodeRepository orgNodeRepository;
    // PORT Develop1 — le seed initialise aussi les espaces du tenant.
    @Mock private com.discipolat.modules.spaces.domain.SpaceRepository spaceRepository;
    @Mock private TenantSubscriptionRepository subscriptionRepository;
    @Mock private SaasPlanRepository planRepository;
    @Mock private TenantRepository tenantRepository;

    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DataInitializer(userRepository, passwordEncoder, soulRepository,
                departmentRepository, memberDepartmentRepository, roleRepository, permissionRepository,
                membershipRepository, orgNodeRepository, spaceRepository,
                subscriptionRepository, planRepository, tenantRepository);
        when(roleRepository.count()).thenReturn(1L);
        when(userRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void productionDoesNotCreateDeterministicSuperAdminByDefault() {
        ReflectionTestUtils.setField(initializer, "environment", "prod");
        ReflectionTestUtils.setField(initializer, "seedDemoAccounts", false);
        ReflectionTestUtils.setField(initializer, "superadminBootstrapEnabled", false);

        initializer.run();

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void configuredProductionBootstrapCreatesOnlyANewAccount() {
        UUID tenantId = UUID.randomUUID();
        Role role = Role.builder().id(UUID.randomUUID()).key("PLATFORM_SUPER_ADMIN").build();
        when(roleRepository.findGlobalByKey("PLATFORM_SUPER_ADMIN")).thenReturn(Optional.of(role));
        when(userRepository.findGlobalByEmail("admin@example.test")).thenReturn(Optional.empty());
        when(tenantRepository.findFirstByStatusOrderByCreatedAtAsc(TenantStatus.ACTIVE))
                .thenReturn(Optional.of(Tenant.builder().id(tenantId).build()));
        when(passwordEncoder.encode("strong-secret")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(initializer, "environment", "prod");
        ReflectionTestUtils.setField(initializer, "superadminBootstrapEnabled", true);
        ReflectionTestUtils.setField(initializer, "superadminEmail", "admin@example.test");
        ReflectionTestUtils.setField(initializer, "superadminSecret", "strong-secret");

        initializer.run();

        verify(passwordEncoder).encode("strong-secret");
        verify(membershipRepository).save(any());
    }

    @Test
    void existingBootstrapEmailIsNeverElevated() {
        User existing = User.builder().id(UUID.randomUUID()).email("admin@example.test").build();
        when(roleRepository.findGlobalByKey("PLATFORM_SUPER_ADMIN"))
                .thenReturn(Optional.of(Role.builder().id(UUID.randomUUID()).key("PLATFORM_SUPER_ADMIN").build()));
        when(userRepository.findGlobalByEmail("admin@example.test")).thenReturn(Optional.of(existing));
        ReflectionTestUtils.setField(initializer, "environment", "prod");
        ReflectionTestUtils.setField(initializer, "superadminBootstrapEnabled", true);
        ReflectionTestUtils.setField(initializer, "superadminEmail", "admin@example.test");
        ReflectionTestUtils.setField(initializer, "superadminSecret", "strong-secret");

        initializer.run();

        verify(passwordEncoder, never()).encode(any());
        verify(membershipRepository, never()).save(any());
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== GARDE-FOU E3 : seed + exposition publique ====================
    //
    // Constat vérifié le 2026-10-11 : une instance de démonstration montée avec
    // `seed-demo-accounts=true` et exposée sur Internet acceptait
    // `admin@discipolat.com / password123` — mot de passe PUBLIÉ dans le dépôt —
    // et délivrait un JWT portant les rôles PASTEUR + ADMIN. Les tests de ce
    // bloc verrouillent que ce couple de conditions ne puisse plus jamais
    // demarrer, qu'une valeur non booleenne comme l'identifiant Render
    // (`srv-…`) active bien le garde-fou sans casser le demarrage, et qu'un
    // `DEMO_PASSWORD` non publie est seul injecte.

    @Test
    void refusesSeedWithPublishedPasswordWhenInstanceIsPubliclyExposed() {
        ReflectionTestUtils.setField(initializer, "environment", "beta");
        ReflectionTestUtils.setField(initializer, "seedDemoAccounts", true);
        ReflectionTestUtils.setField(initializer, "exposedPubliclyFlag", "true");
        ReflectionTestUtils.setField(initializer, "demoPassword", "password123");

        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> initializer.run());

        assertTrue(refused.getMessage().contains("DEMO_PASSWORD"),
                "le message doit dire comment débloquer, pas seulement qu'on bloque");
        // Le refus est prématuré : aucune écriture, aucun hachage.
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void renderServiceIdValueActsAsExposedWithoutBreakingBooleanBinding() {
        // Forme REELLE de la valeur sur Render : un identifiant, pas un booleen.
        // Si Spring devait le convertir en booleen, le service refuserait de
        // demarrer pour une raison de typage — le garde-fou doit fonctionner
        // avec la valeur telle qu'elle existe en production.
        for (String idRender : List.of("srv-abc123", "srv-xyz789")) {
            ReflectionTestUtils.setField(initializer, "environment", "beta");
            ReflectionTestUtils.setField(initializer, "seedDemoAccounts", true);
            ReflectionTestUtils.setField(initializer, "exposedPubliclyFlag", idRender);
            ReflectionTestUtils.setField(initializer, "demoPassword", "password123");

            IllegalStateException refused = assertThrows(IllegalStateException.class,
                    () -> initializer.run(),
                    "un identifiant de service Render doit compter comme exposition publique");
            assertTrue(refused.getMessage().contains("Refus de démarrer"),
                    "l'echec doit venir du garde-fou E3, pas d'autre chose : "
                            + refused.getMessage());
        }
    }

    @Test
    void blankAndFalseFlagsStayNonExposedSoCiAndLocalKeepWorking() {
        // Redevable inverse : toute valeur vide ou `false` doit rester consideree
        // comme NON exposee. Sans ce comportement, chaque run CI, chaque poste
        // local et chaque E2E serait refuse au demarrage.
        // Le signal est le MOTIF de l'echec : on laisse volontairement le tenant
        // absent, donc « No active tenant » ne peut apparaitre qu'APRES avoir
        // passe le garde-fou E3. Un echec « Refus de demarrer » traduirait un
        // garde-fou declenche a tort.
        ReflectionTestUtils.setField(initializer, "environment", "beta");
        ReflectionTestUtils.setField(initializer, "seedDemoAccounts", true);
        ReflectionTestUtils.setField(initializer, "demoPassword", "password123");
        for (String inactif : List.of("", "   ", "false", "FALSE")) {
            ReflectionTestUtils.setField(initializer, "exposedPubliclyFlag", inactif);
            IllegalStateException past = assertThrows(IllegalStateException.class,
                    () -> initializer.run(),
                    "valeur '" + inactif + "' doit rester consideree comme non exposee");
            assertTrue(past.getMessage().contains("tenant"),
                    "la valeur '" + inactif + "' doit avoir depasse le garde-fou E3, "
                            + "message observe: " + past.getMessage());
        }
    }

    @Test
    void passesTheGuardWhenPasswordIsNotThePublishedOne() {
        // Meme combinaison seed + exposee, mais avec un mot de passe non publie :
        // le garde-fou ne doit PAS refuser. On s'arrete volontairement avant le
        // seed reel (aucun tenant actif) : ce test porte sur le garde-fou, pas
        // sur toute la chaine d'onboarding de demonstration.
        ReflectionTestUtils.setField(initializer, "environment", "beta");
        ReflectionTestUtils.setField(initializer, "seedDemoAccounts", true);
        ReflectionTestUtils.setField(initializer, "exposedPubliclyFlag", "true");
        ReflectionTestUtils.setField(initializer, "demoPassword", "non-publie-2026");
        when(passwordEncoder.encode("non-publie-2026")).thenReturn("encoded-secret");

        assertThrows(IllegalStateException.class, () -> initializer.run());

        // Le mot de passe demande est bien celui configure, jamais le publie.
        verify(passwordEncoder, atLeastOnce()).encode("non-publie-2026");
        verify(passwordEncoder, never()).encode("password123");
    }

    @Test
    void seedUserHashesTheConfiguredPasswordNotThePublishedOne() {
        // Point d'injection verrouille isolément : le compte cree recoit le hash
        // du mot de passe configure. Passer par seedUser evite de simuler toute
        // la chaine d'onboarding demo (tenant, espaces, departements, memberships),
        // dont la couverture complete releve d'un test d'integration.
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.findFirstByStatusOrderByCreatedAtAsc(TenantStatus.ACTIVE))
                .thenReturn(Optional.of(Tenant.builder().id(tenantId).build()));
        when(userRepository.findFirstByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("non-publie-2026")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(initializer, "demoPassword", "non-publie-2026");

        ReflectionTestUtils.invokeMethod(initializer, "seedUser",
                "admin@discipolat.com", "Admin", "System",
                UserRole.ADMIN, Set.of(UserRole.ADMIN), UserRole.ADMIN, false);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        org.assertj.core.api.Assertions.assertThat(saved.getValue().getPasswordHash())
                .isEqualTo("encoded-secret");
        verify(passwordEncoder, never()).encode("password123");
    }

    @Test
    void reencodesLegacyPlaceholderHashWithConfiguredPassword() {
        // Un compte herite des migrations porte le hash « PLACEHOLDER » : le seed
        // doit le remplacer par le mot de passe configure, pas par le publie.
        User legacy = User.builder().id(UUID.randomUUID()).email("pasteur@discipolat.com")
                .passwordHash("PLACEHOLDER").build();
        when(userRepository.findAll()).thenReturn(List.of(legacy));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("non-publie-2026")).thenReturn("encoded-secret");
        ReflectionTestUtils.setField(initializer, "environment", "beta");
        ReflectionTestUtils.setField(initializer, "seedDemoAccounts", true);
        ReflectionTestUtils.setField(initializer, "exposedPubliclyFlag", "");
        ReflectionTestUtils.setField(initializer, "demoPassword", "non-publie-2026");

        // Le tenant actif est absent : on s'arrete apres la boucle de re-encodage,
        // avant les creations de comptes — comportement verrouille ici.
        assertThrows(IllegalStateException.class, () -> initializer.run());

        org.assertj.core.api.Assertions.assertThat(legacy.getPasswordHash())
                .isEqualTo("encoded-secret");
        verify(passwordEncoder, never()).encode("password123");
    }

    @Test
    void allowsSeedWithPublishedPasswordWhenNotExposed() {
        // Non expose (local, CI, E2E) : le mot de passe publie reste admis, aucun
        // garde-fou ne doit surprendre celui qui lance la stack chez lui. Meme
        // signal que ci-dessus : « Refus de demarrer » ne doit jamais apparaitre.
        ReflectionTestUtils.setField(initializer, "environment", "beta");
        ReflectionTestUtils.setField(initializer, "seedDemoAccounts", true);
        ReflectionTestUtils.setField(initializer, "exposedPubliclyFlag", "");
        ReflectionTestUtils.setField(initializer, "demoPassword", "password123");

        IllegalStateException past = assertThrows(IllegalStateException.class,
                () -> initializer.run());
        assertTrue(past.getMessage().contains("tenant"),
                "sans exposition publique le garde-fou E3 ne doit pas se declencher");
    }
}
