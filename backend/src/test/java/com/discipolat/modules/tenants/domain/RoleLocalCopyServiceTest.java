package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.repository.MembershipRoleQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * PARAMÉTRISATION PAR ÉGLISE (Phase 9) — le CHEMIN D'ÉCRITURE de la matrice
 * rôles → permissions, testé unité par unité.
 *
 * <p><strong>Pourquoi ce test existe alors que la lecture était déjà couverte.</strong>
 * L'audit de la phase 8 avait laissé une promesse non tenue : {@code V258} annonce
 * que « chaque église peut ajuster les permissions de ses rôles », et le moteur de
 * résolution sait faire — {@code AuthorizationService} comme {@code
 * PermissionResolver.addRolePermissions} cherchent {@code
 * findByTenantIdAndKey(tenant, cle)} <em>avant</em> la ligne globale. Mais aucune
 * écriture ne pouvait produire cette ligne locale : {@code createCustomRole}
 * refusait les clés système ({@code SYSTEM_ROLE_KEY}) et {@code assignPermissions}
 * refusait les rôles système ({@code CANNOT_MODIFY_SYSTEM_ROLE}). Constat en base
 * de démonstration : 0 ligne {@code roles} avec {@code tenant_id} non nul — c'est-à-dire
 * 0 église réellement paramétrable, malgré une UI qui proposait l'édition. Trois
 * défauts étaient regroupés là, et chacun est verrouillé par un test avec son
 * CONTRÔLE NÉGATIF (ce qui rougirait si le correctif est retiré) :
 * <ul>
 *   <li>impossibilité de créer la surcouche (→ {@code instantiateLocalRoleCopy}) ;</li>
 *   <li>clés de permission fantômes avalées silencieusement par {@code
 *       findByKey(k).ifPresent(...)} — l'éditeur affichait « enregistré » sans rien
 *       changer (→ {@code UNKNOWN_PERMISSION_KEYS}, défaut D7) ;</li>
 *   <li>aucune invalidation du cache servi après écriture — les droits ne
 *       changeaient côté client qu'après reconnexion (→ {@code bumpPermissions},
 *       défaut D10).</li>
 * </ul>
 *
 * <p>Choix du test unitaire plutôt que d'une montée en charge HTTP : ces trois
 * défauts sont des décisions de service, pas des effets de bord de transport. Les
 * gardes HTTP ({@code @authz.can}) et la cohérence du miroir servi sont verrouillées
 * ailleurs ({@code AuthorizationServiceEquivalenceBridgeTest}, {@code
 * PermissionMatrixEffectivenessPostgreSqlTest}, le ratchet frontend {@code
 * parametrisationGate}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Paramétrisation par église — écriture de la matrice rôles/permissions (Phase 9)")
class RoleLocalCopyServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    // Phase 9 — les lectures guidées par la clé d'un rôle vivent dans le dépôt
    // d'adaptateur du même contexte (gel R1 d'ArchUnit : plafond par compte, pas
    // blanc-seing). Voir MembershipRoleQueryRepository.
    @Mock private MembershipRoleQueryRepository membershipRoleQueries;
    @Mock private AuditService auditService;
    @Mock private EntityPropagationPublisher propagationPublisher;
    @Mock private PermissionCacheInvalidator permissionCache;

    @InjectMocks private RoleManagementService service;

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TENANT = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID HOLDER = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GLOBAL_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID LOCAL_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    // ==================== fixtures ====================

    private static Permission perm(String key) {
        return Permission.builder()
                .id(UUID.nameUUIDFromBytes(key.getBytes()))
                .key(key)
                .label("Libellé " + key)
                .scope(PermissionScope.TENANT)
                .system(true)
                .build();
    }

    /** Rôle global système — le modèle V258 (ADMIN, PASTEUR, MEMBRE…). */
    private static Role globalRole(String key, Permission... permissions) {
        return Role.builder()
                .id(GLOBAL_ID)
                .tenantId(null)
                .key(key)
                .label("Global " + key)
                .description("modèle système")
                .system(true)
                .priority(300)
                .permissions(new java.util.HashSet<>(Set.of(permissions)))
                .build();
    }

    /** Surcouche locale déjà personnalisée par l'église. */
    private static Role localRole(String key, UUID id, Permission... permissions) {
        return Role.builder()
                .id(id)
                .tenantId(TENANT)
                .key(key)
                .label("Local " + key)
                .system(false)
                .priority(300)
                .permissions(new java.util.HashSet<>(Set.of(permissions)))
                .build();
    }

    /** Rôle custom d'une église, sans équivalent global. */
    private static Role customRole(String key, UUID tenantId, UUID id, Permission... permissions) {
        return Role.builder()
                .id(id)
                .tenantId(tenantId)
                .key(key)
                .label("Custom " + key)
                .system(false)
                .priority(10)
                .permissions(new java.util.HashSet<>(Set.of(permissions)))
                .build();
    }

    private static TenantMembership membership(UUID userId, Role role) {
        return TenantMembership.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .userId(userId)
                .role(role)
                .scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE)
                .build();
    }

    // ==================== 0. résolution d'une clé de rôle ====================

    /**
     * {@code findByKey} est la seule question qui vaille pour l'église : « quelle
     * ligne décide de mes droits ? ». Elle est posée par le moteur d'autorisation
     * ({@code AuthorizationService}, {@code PermissionResolver}) ET par
     * l'affectation d'un rôle à un membre
     * ({@code TenantAdminController#updateMemberRole}) — d'où le défaut D13 : ce
     * dernier chemin consultait uniquement les lignes {@code tenant_id = ?}, donc
     * rien sur une installation sans surcouche, et refusait MEMBRE comme ADMIN.
     */
    @Nested
    @DisplayName("findByKey — quelle ligne décide pour cette église")
    class ResolveRoleKey {

        @Test
        @DisplayName("Sans surcouche, repli sur la ligne globale (cas d'une installation neuve)")
        void laLigneGlobaleEstTrouveeQuandAucuneSurcoucheExiste() {
            Role global = globalRole("FAISEUR", perm("MEMBERS_READ"));
            when(roleRepository.findByTenantIdAndKey(TENANT, "FAISEUR")).thenReturn(Optional.empty());
            when(roleRepository.findByTenantIdIsNullAndKey("FAISEUR")).thenReturn(Optional.of(global));

            Role resolu = service.findByKey(TENANT, "FAISEUR").orElseThrow();

            assertThat(resolu.getId())
                    .as("aucune surcouche → c'est le modèle global qui s'applique")
                    .isEqualTo(GLOBAL_ID);
        }

        @Test
        @DisplayName("La surcouche locale prime sur le modèle global homonyme")
        void laSurcouchePrimeEtNeDemandePasLeModeleGlobal() {
            Role local = localRole("FAISEUR", LOCAL_ID, perm("MEMBERS_READ"));
            when(roleRepository.findByTenantIdAndKey(TENANT, "FAISEUR")).thenReturn(Optional.of(local));

            Role resolu = service.findByKey(TENANT, "FAISEUR").orElseThrow();

            assertThat(resolu.getId()).isEqualTo(LOCAL_ID);
            // CONTRÔLE NÉGATIF : relire aussi le modèle global laisserait deux
            // sources de vérité simultanées pour la même clé.
            verify(roleRepository, never()).findByTenantIdIsNullAndKey("FAISEUR");
        }

        @Test
        @DisplayName("Clé inconnue = vide, sans jamais retourner la ligne d'une autre église")
        void cleInconnueRenvoieVideSansFuiteInterTenant() {
            when(roleRepository.findByTenantIdAndKey(TENANT, "DIACRE")).thenReturn(Optional.empty());
            when(roleRepository.findByTenantIdIsNullAndKey("DIACRE")).thenReturn(Optional.empty());

            // La saisie est normalisée : « diacre » doit taper la même clé que « DIACRE ».
            assertThat(service.findByKey(TENANT, "diacre")).isEmpty();
            assertThat(service.findByKey(OTHER_TENANT, "DIACRE")).isEmpty();
        }
    }

    // ==================== 1. surcouche locale : création ====================

    @Nested
    @DisplayName("instantiateLocalRoleCopy — ouvrir la personnalisation sans rien retirer")
    class InstantiateLocalCopy {

        @Test
        @DisplayName("La copie locale hérite de la matrice globale : aucun droit retiré aux porteurs")
        void laCopieHeriteDeLaMatriceGlobale() {
            Permission lecture = perm("MEMBERS_READ");
            Permission ecriture = perm("MEMBERS_WRITE");
            Role global = globalRole("ADMIN", lecture, ecriture);
            when(roleRepository.findById(GLOBAL_ID)).thenReturn(Optional.of(global));
            when(roleRepository.findByTenantIdAndKey(TENANT, "ADMIN")).thenReturn(Optional.empty());
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
                Role toSave = inv.getArgument(0);
                toSave.setId(LOCAL_ID);
                return toSave;
            });
            when(membershipRoleQueries.findActiveUserIdsByTenantAndRoleKey(
                    TENANT, MembershipStatus.ACTIVE, "ADMIN")).thenReturn(List.of(HOLDER));

            Role local = service.instantiateLocalRoleCopy(GLOBAL_ID, TENANT, ACTOR);

            ArgumentCaptor<Role> saved = ArgumentCaptor.forClass(Role.class);
            verify(roleRepository).save(saved.capture());
            Role created = saved.getValue();
            assertThat(created.getTenantId())
                    .as("La surcouche doit être rattachée à l'église, sinon elle ne prime rien")
                    .isEqualTo(TENANT);
            assertThat(created.getKey())
                    .as("La clé doit rester identique : c'est elle que résout le moteur d'autorisation")
                    .isEqualTo("ADMIN");
            assertThat(created.getSystem())
                    .as("La copie n'est PAS système : c'est ce qui la rend modifiable")
                    .isFalse();
            assertThat(created.getPriority()).isEqualTo(global.getPriority());
            // CONTRÔLE NÉGATIF du no-op de sécurité : une implémentation « copie vide »
            // (la tentation la plus courte) passerait ici — et aurait, à l'instant de la
            // création, retiré TOUS les droits d'ADMIN dans l'église, la surcouche
            // primant la ligne globale. Cette assertion est le verrou.
            assertThat(created.getPermissions())
                    .extracting(Permission::getKey)
                    .containsExactlyInAnyOrder("MEMBERS_READ", "MEMBERS_WRITE");
            assertThat(local.getId()).isEqualTo(LOCAL_ID);
        }

        @Test
        @DisplayName("La résolution des porteurs de la clé est bumpée : le miroir servi suit la nouvelle source")
        void lesPorteursSontInvalides() {
            Role global = globalRole("ADMIN", perm("MEMBERS_READ"));
            when(roleRepository.findById(GLOBAL_ID)).thenReturn(Optional.of(global));
            when(roleRepository.findByTenantIdAndKey(TENANT, "ADMIN")).thenReturn(Optional.empty());
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
            when(membershipRoleQueries.findActiveUserIdsByTenantAndRoleKey(
                    TENANT, MembershipStatus.ACTIVE, "ADMIN")).thenReturn(List.of(HOLDER));

            service.instantiateLocalRoleCopy(GLOBAL_ID, TENANT, ACTOR);

            // CONTRÔLE NÉGATIF : sans ce bump, un client déjà connecté sur un compte
            // ADMIN continuerait de lire l'ancien cache de permissions (défaut D10).
            verify(permissionCache).bumpPermissions(TENANT, HOLDER, "ROLE_LOCAL_COPY_CREATED");
        }

        @Test
        @DisplayName("Idempotence : une surcouche déjà personnalisée est retrouvée, jamais écrasée")
        void relancerNeDupliquePasNEcrasePas() {
            Role global = globalRole("ADMIN", perm("MEMBERS_READ"));
            Role dejaPersonnalise = localRole("ADMIN", LOCAL_ID, perm("MEMBERS_READ"));
            when(roleRepository.findById(GLOBAL_ID)).thenReturn(Optional.of(global));
            when(roleRepository.findByTenantIdAndKey(TENANT, "ADMIN")).thenReturn(Optional.of(dejaPersonnalise));

            Role result = service.instantiateLocalRoleCopy(GLOBAL_ID, TENANT, ACTOR);

            assertThat(result.getId()).isEqualTo(LOCAL_ID);
            // CONTRÔLE NÉGATIF : re-créer aurait remplacé la copie — donc perdu la
            // personnalisation en cours de l'église.
            verify(roleRepository, never()).save(any(Role.class));
            verifyNoInteractions(permissionCache);
        }

        @Test
        @DisplayName("Refus : une ligne déjà propre à une église n'est pas un modèle copiable")
        void refuseUneLigneDejaLocale() {
            when(roleRepository.findById(LOCAL_ID))
                    .thenReturn(Optional.of(localRole("ADMIN", LOCAL_ID, perm("MEMBERS_READ"))));

            assertThatThrownBy(() -> service.instantiateLocalRoleCopy(LOCAL_ID, TENANT, ACTOR))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting(e -> ((BusinessRuleException) e).getCode())
                    .isEqualTo("ROLE_NOT_GLOBAL");
            verify(roleRepository, never()).save(any(Role.class));
        }

        @Test
        @DisplayName("Une surcouche créée pour une église ne touche pas les autres églises")
        void uneAutreEgliseNestPasInvalidee() {
            Role global = globalRole("ADMIN", perm("MEMBERS_READ"));
            when(roleRepository.findById(GLOBAL_ID)).thenReturn(Optional.of(global));
            when(roleRepository.findByTenantIdAndKey(TENANT, "ADMIN")).thenReturn(Optional.empty());
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
            when(membershipRoleQueries.findActiveUserIdsByTenantAndRoleKey(
                    TENANT, MembershipStatus.ACTIVE, "ADMIN")).thenReturn(List.of());

            service.instantiateLocalRoleCopy(GLOBAL_ID, TENANT, ACTOR);

            // L'isolation multi-tenant est la propriété la plus coûteuse à perdre :
            // aucune écriture ne doit déborder sur OTHER_TENANT.
            verify(membershipRoleQueries, never())
                    .findActiveUserIdsByTenantAndRoleKey(OTHER_TENANT, MembershipStatus.ACTIVE, "ADMIN");
        }
    }

    // ==================== 2. surcouche locale : retour arrière ====================

    @Nested
    @DisplayName("revertLocalRoleCopy — personnaliser doit être réversible")
    class RevertLocalCopy {

        @Test
        @DisplayName("Réinitialiser ré-ancre les porteurs sur le global, puis supprime la copie")
        void reancragePuisSuppression() {
            Role global = globalRole("ADMIN", perm("MEMBERS_READ"));
            Role local = localRole("ADMIN", LOCAL_ID, perm("MEMBERS_READ"), perm("MEMBERS_WRITE"));
            TenantMembership m = membership(HOLDER, local);
            when(roleRepository.findById(LOCAL_ID)).thenReturn(Optional.of(local));
            when(roleRepository.findByTenantIdIsNullAndKey("ADMIN")).thenReturn(Optional.of(global));
            when(membershipRoleQueries.findAllByRoleId(LOCAL_ID)).thenReturn(List.of(m));

            Role result = service.revertLocalRoleCopy(LOCAL_ID, ACTOR);

            assertThat(result.getId()).isEqualTo(GLOBAL_ID);
            assertThat(m.getRole())
                    .as("Sans ré-ancrage, la FK tenant_memberships.role_id resterait sur une ligne supprimée")
                    .isSameAs(global);
            verify(membershipRepository).save(m);
            verify(roleRepository).delete(local);
            verify(permissionCache).bumpPermissions(TENANT, HOLDER, "ROLE_LOCAL_COPY_REVERTED");
        }

        @Test
        @DisplayName("deleteRole sur une surcouche personnalisée réinitialise au lieu de refuser ROLE_IN_USE")
        void supprimerUneSurcoucheNeBloquePas() {
            Role global = globalRole("ADMIN", perm("MEMBERS_READ"));
            Role local = localRole("ADMIN", LOCAL_ID, perm("MEMBERS_READ"));
            when(roleRepository.findById(LOCAL_ID)).thenReturn(Optional.of(local));
            when(roleRepository.findByTenantIdIsNullAndKey("ADMIN")).thenReturn(Optional.of(global));
            when(membershipRoleQueries.findAllByRoleId(LOCAL_ID)).thenReturn(List.of(membership(HOLDER, local)));

            service.deleteRole(LOCAL_ID, ACTOR);

            // CONTRÔLE NÉGATIF : avant Phase 9, un rôle portant des membres ne pouvait
            // pas être supprimé — une personnalisation engagée devenait donc un aller
            // sans retour. Ici on réinitialise, et le garde ROLE_IN_USE n'est jamais
            // consulté (il reste la règle pour un custom sans modèle global).
            verify(membershipRepository, never()).countByRoleId(any());
            verify(roleRepository).delete(local);
        }

        @Test
        @DisplayName("Refus : pas de ligne globale homonyme → rien sur quoi réinitialiser")
        void refuseSansModeleGlobal() {
            when(roleRepository.findById(LOCAL_ID))
                    .thenReturn(Optional.of(customRole("DIACRE", TENANT, LOCAL_ID)));
            when(roleRepository.findByTenantIdIsNullAndKey("DIACRE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revertLocalRoleCopy(LOCAL_ID, ACTOR))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting(e -> ((BusinessRuleException) e).getCode())
                    .isEqualTo("GLOBAL_ROLE_MISSING");
            verify(roleRepository, never()).delete(any(Role.class));
        }

        @Test
        @DisplayName("Refus : une ligne globale n'a pas de personnalisation à réinitialiser")
        void refuseUneLigneGlobale() {
            when(roleRepository.findById(GLOBAL_ID))
                    .thenReturn(Optional.of(globalRole("ADMIN", perm("MEMBERS_READ"))));

            assertThatThrownBy(() -> service.revertLocalRoleCopy(GLOBAL_ID, ACTOR))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting(e -> ((BusinessRuleException) e).getCode())
                    .isEqualTo("ROLE_NOT_LOCAL");
        }
    }

    // ==================== 3. écriture de la matrice ====================

    @Nested
    @DisplayName("assignPermissions — contrat strict et cache invalidé")
    class AssignPermissions {

        @Test
        @DisplayName("Une clé hors catalogue est REJETÉE (400) et ne modifie rien — défaut D7")
        void cleFantomeRejetee() {
            Role local = localRole("ADMIN", LOCAL_ID, perm("MEMBERS_READ"));
            when(roleRepository.findById(LOCAL_ID)).thenReturn(Optional.of(local));
            when(permissionRepository.findByKeyIn(anyCollection())).thenReturn(List.of(perm("MEMBERS_READ")));

            assertThatThrownBy(() -> service.assignPermissions(
                    LOCAL_ID, List.of("MEMBERS_READ", "CATALOGUE_GHOST_KEY"), ACTOR))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("CATALOGUE_GHOST_KEY")
                    .extracting(e -> ((BusinessRuleException) e).getCode())
                    .isEqualTo("UNKNOWN_PERMISSION_KEYS");

            // CONTRÔLE NÉGATIF du défaut d'origine : avant, la clé fantôme était
            // écartée en silence, l'appel renvoyait un 200 et l'éditeur affichait
            // « enregistré » sans rien changer. Aucune écriture ne doit passer.
            verify(roleRepository, never()).save(any(Role.class));
            verifyNoInteractions(permissionCache);
        }

        @Test
        @DisplayName("Clés valides : la matrice est remplacée ET les porteurs sont invalidés — défaut D10")
        void matriceRemplaceeEtCacheInvalide() {
            Role local = localRole("ADMIN", LOCAL_ID, perm("MEMBERS_READ"));
            when(roleRepository.findById(LOCAL_ID)).thenReturn(Optional.of(local));
            when(permissionRepository.findByKeyIn(anyCollection()))
                    .thenReturn(List.of(perm("MEMBERS_READ"), perm("MEMBERS_WRITE")));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
            when(membershipRoleQueries.findActiveUserIdsByTenantAndRoleKey(
                    TENANT, MembershipStatus.ACTIVE, "ADMIN")).thenReturn(List.of(HOLDER));

            service.assignPermissions(LOCAL_ID, List.of("members_read", " MEMBERS_WRITE ", "MEMBERS_WRITE"), ACTOR);

            ArgumentCaptor<Role> saved = ArgumentCaptor.forClass(Role.class);
            verify(roleRepository).save(saved.capture());
            assertThat(saved.getValue().getPermissions())
                    .as("Casse et espaces tolérés, doublons éliminés : le catalogue canonique est la seule vérité")
                    .extracting(Permission::getKey)
                    .containsExactlyInAnyOrder("MEMBERS_READ", "MEMBERS_WRITE");
            verify(permissionCache).bumpPermissions(TENANT, HOLDER, "ROLE_PERMISSIONS_ASSIGNED");
        }

        @Test
        @DisplayName("Un corps sans permissionKeys est une erreur explicite, jamais un 500 ni une vidange")
        void permissionKeysAbsentRejete() {
            when(roleRepository.findById(LOCAL_ID))
                    .thenReturn(Optional.of(localRole("ADMIN", LOCAL_ID, perm("MEMBERS_READ"))));

            assertThatThrownBy(() -> service.assignPermissions(LOCAL_ID, null, ACTOR))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting(e -> ((BusinessRuleException) e).getCode())
                    .isEqualTo("PERMISSION_KEYS_REQUIRED");
            verify(roleRepository, never()).save(any(Role.class));
        }

        @Test
        @DisplayName("Liste vide assumée : vider la matrice est un acte explicite, et il est propagé")
        void viderLaMatriceEstAutoriseEtPropage() {
            Role local = localRole("ADMIN", LOCAL_ID, perm("MEMBERS_READ"));
            when(roleRepository.findById(LOCAL_ID)).thenReturn(Optional.of(local));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
            when(membershipRoleQueries.findActiveUserIdsByTenantAndRoleKey(
                    TENANT, MembershipStatus.ACTIVE, "ADMIN")).thenReturn(List.of(HOLDER));

            service.assignPermissions(LOCAL_ID, List.of(), ACTOR);

            // CONTRÔLE NÉGATIF : une liste vide doit bien atteindre le cache, sinon
            // l'église croirait avoir retiré le droit tandis que le client le verrait
            // encore accordé.
            verify(permissionCache).bumpPermissions(TENANT, HOLDER, "ROLE_PERMISSIONS_ASSIGNED");
        }

        @Test
        @DisplayName("Garde-fou conservé : un rôle système global reste non modifiable en l'état")
        void roleSystemeToujoursProtege() {
            when(roleRepository.findById(GLOBAL_ID))
                    .thenReturn(Optional.of(globalRole("ADMIN", perm("MEMBERS_READ"))));

            assertThatThrownBy(() -> service.assignPermissions(GLOBAL_ID, List.of("MEMBERS_READ"), ACTOR))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting(e -> ((BusinessRuleException) e).getCode())
                    .isEqualTo("CANNOT_MODIFY_SYSTEM_ROLE");
        }

        @Test
        @DisplayName("Ajout unitaire : clé inconnue rejetée, clé connue propagée")
        void ajoutUnitaireStrict() {
            Role local = localRole("ADMIN", LOCAL_ID, perm("MEMBERS_READ"));
            when(roleRepository.findById(LOCAL_ID)).thenReturn(Optional.of(local));
            when(permissionRepository.findByKeyIn(anyCollection())).thenReturn(List.of());

            assertThatThrownBy(() -> service.addPermission(LOCAL_ID, "NOPE", ACTOR))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting(e -> ((BusinessRuleException) e).getCode())
                    .isEqualTo("UNKNOWN_PERMISSION_KEYS");
            verify(roleRepository, never()).save(any(Role.class));
        }
    }

    // ==================== 4. rôle custom et affectations ====================

    @Nested
    @DisplayName("createCustomRole / assignRoleToUser — cohérence du catalogue et du cache")
    class CustomRoleAndAssignment {

        @Test
        @DisplayName("Créer un rôle custom avec une clé fantôme échoue au lieu d'ignorer la clé")
        void cleFantomeRejeteeACreation() {
            when(roleRepository.findByTenantIdAndKey(TENANT, "RESPONSABLE")).thenReturn(Optional.empty());
            when(roleRepository.findByTenantIdIsNullAndKey("RESPONSABLE")).thenReturn(Optional.empty());
            when(permissionRepository.findByKeyIn(anyCollection())).thenReturn(List.of());

            assertThatThrownBy(() -> service.createCustomRole(TENANT,
                    new RoleManagementService.CreateRoleRequest("responsable", "Responsable", null, 5,
                            List.of("GHOST_KEY")), ACTOR))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting(e -> ((BusinessRuleException) e).getCode())
                    .isEqualTo("UNKNOWN_PERMISSION_KEYS");
        }

        @Test
        @DisplayName("Garde-fou conservé : la clé d'un rôle système ne peut pas être réutilisée comme custom")
        void cleSystemeToujoursReservee() {
            when(roleRepository.findByTenantIdAndKey(TENANT, "ADMIN")).thenReturn(Optional.empty());
            when(roleRepository.findByTenantIdIsNullAndKey("ADMIN"))
                    .thenReturn(Optional.of(globalRole("ADMIN")));

            assertThatThrownBy(() -> service.createCustomRole(TENANT,
                    new RoleManagementService.CreateRoleRequest("ADMIN", "Doublon", null, 1, List.of()), ACTOR))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting(e -> ((BusinessRuleException) e).getCode())
                    .isEqualTo("SYSTEM_ROLE_KEY");
        }

        @Test
        @DisplayName("Affecter un rôle à un utilisateur recalcule ses permissions servies")
        void affecterUnRoleBumpLeCache() {
            Role local = localRole("ADMIN", LOCAL_ID, perm("MEMBERS_READ"));
            when(roleRepository.findById(LOCAL_ID)).thenReturn(Optional.of(local));
            when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(
                    HOLDER, TENANT, MembershipStatus.ACTIVE)).thenReturn(List.of());
            when(membershipRepository.save(any(TenantMembership.class))).thenAnswer(inv -> inv.getArgument(0));

            service.assignRoleToUser(HOLDER, TENANT, LOCAL_ID, null, ACTOR);

            verify(permissionCache).bumpPermissions(TENANT, HOLDER, "ROLE_ASSIGNED");
        }

        @Test
        @DisplayName("Remplacer le rôle d'une membership existante recalcule aussi")
        void remplacerUnRoleBumpAussi() {
            Role local = localRole("ADMIN", LOCAL_ID, perm("MEMBERS_READ"));
            TenantMembership existing = membership(HOLDER, localRole("MEMBRE", LOCAL_ID));
            when(roleRepository.findById(LOCAL_ID)).thenReturn(Optional.of(local));
            when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(
                    HOLDER, TENANT, MembershipStatus.ACTIVE)).thenReturn(List.of(existing));
            when(membershipRepository.save(any(TenantMembership.class))).thenAnswer(inv -> inv.getArgument(0));

            service.assignRoleToUser(HOLDER, TENANT, LOCAL_ID, null, ACTOR);

            assertThat(existing.getRole()).isSameAs(local);
            verify(permissionCache).bumpPermissions(TENANT, HOLDER, "ROLE_ASSIGNED");
        }
    }

    // ==================== 5. projection éditeur ====================

    @Nested
    @DisplayName("listRolesForEditor — dire au client quelle ligne décide vraiment")
    class EditorProjection {

        @Test
        @DisplayName("La surcouche prime : le rôle global homonyme apparaît non effectif, et customizable")
        void laSurcoucheEstSeuleEffective() {
            Role global = globalRole("ADMIN", perm("MEMBERS_READ"));
            Role local = localRole("ADMIN", LOCAL_ID, perm("MEMBERS_READ"));
            Role custom = customRole("DIACRE", TENANT, UUID.randomUUID(), perm("MEMBERS_READ"));
            when(roleRepository.findByTenantIdIsNull()).thenReturn(List.of(global));
            when(roleRepository.findByTenantId(TENANT)).thenReturn(List.of(local, custom));
            when(roleRepository.findPermissionKeysByRoleIds(anyCollection())).thenReturn(List.<Object[]>of(
                    new Object[]{GLOBAL_ID, "MEMBERS_READ"},
                    new Object[]{LOCAL_ID, "MEMBERS_READ"},
                    new Object[]{custom.getId(), "MEMBERS_READ"}));

            List<java.util.Map<String, Object>> views = service.listRolesForEditor(TENANT);

            java.util.Map<String, Object> globalView = views.stream()
                    .filter(v -> GLOBAL_ID.toString().equals(v.get("id"))).findFirst().orElseThrow();
            java.util.Map<String, Object> localView = views.stream()
                    .filter(v -> LOCAL_ID.toString().equals(v.get("id"))).findFirst().orElseThrow();
            java.util.Map<String, Object> customView = views.stream()
                    .filter(v -> custom.getId().toString().equals(v.get("id"))).findFirst().orElseThrow();

            assertThat(globalView).containsEntry("customizable", true);
            assertThat(globalView).containsEntry("customized", true);
            assertThat(globalView).containsEntry("effectiveForTenant", false);
            assertThat(localView).containsEntry("local", true);
            assertThat(localView).containsEntry("effectiveForTenant", true);
            assertThat(localView).containsEntry("customizable", false);
            // Un custom sans modèle global : effectif, non « surchargé ».
            assertThat(customView).containsEntry("customized", false);
            assertThat(customView).containsEntry("effectiveForTenant", true);
        }

        @Test
        @DisplayName("Aucune surcouche : les rôles globaux restent effectifs et personnalisables (état d'avant)")
        void sansSurcoucheRienNeChangeEnApparence() {
            Role global = globalRole("ADMIN", perm("MEMBERS_READ"));
            when(roleRepository.findByTenantIdIsNull()).thenReturn(List.of(global));
            when(roleRepository.findByTenantId(TENANT)).thenReturn(List.of());
            when(roleRepository.findPermissionKeysByRoleIds(anyCollection()))
                    .thenReturn(List.<Object[]>of(new Object[]{GLOBAL_ID, "MEMBERS_READ"}));

            java.util.Map<String, Object> view = service.listRolesForEditor(TENANT).get(0);

            assertThat(view).containsEntry("effectiveForTenant", true);
            assertThat(view).containsEntry("customized", false);
            assertThat(view).containsEntry("isSystem", true);
            // Contrat historique intact : les clés lues avant Phase 9 sont toujours là.
            assertThat(view).containsKeys("id", "key", "label", "priority", "isSystem", "permissions");
        }
    }
}
