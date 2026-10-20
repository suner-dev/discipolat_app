package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.Payloads;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.repository.MembershipRoleQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des rôles (système + personnalisés par tenant).
 * Permet la création de rôles custom, assignation permissions, priorités.
 */
@Service
@Transactional
public class RoleManagementService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantMembershipRepository membershipRepository;
    /**
     * Lectures d'appartenances guidées par la clé d'un rôle. Déposées hors de
     * {@code tenants.domain} : le gel R1 d'ArchUnit gèle la dette du domaine PAR
     * COMPTE, et ces requêtes {@code @Query}/{@code @Param} sont du câblage de
     * persistance, pas du métier. Même contexte, donc aucune arête nouvelle
     * (règle R3). Voir le javadoc de l'interface pour la justification complète.
     */
    private final MembershipRoleQueryRepository membershipRoleQueries;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;
    /**
     * Parametrisation par eglise (Phase 9) — invalider le cache de permissions
     * servies apres toute ecriture sur la matrice. Injecte via le port publie
     * {@link PermissionCacheInvalidator} (regle R6 : le domaine tenants ne doit
     * pas connaitre une classe du contexte families).
     */
    private final PermissionCacheInvalidator permissionCache;

    public RoleManagementService(RoleRepository roleRepository,
                                 PermissionRepository permissionRepository,
                                 TenantMembershipRepository membershipRepository,
                                 MembershipRoleQueryRepository membershipRoleQueries,
                                 AuditService auditService,
                                 EntityPropagationPublisher propagationPublisher,
                                 PermissionCacheInvalidator permissionCache) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleQueries = membershipRoleQueries;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
        this.permissionCache = permissionCache;
    }

    // ==================== READ OPERATIONS ====================

    @Transactional(readOnly = true)
    public List<Role> getAllRoles(UUID tenantId) {
        // Return system roles + tenant custom roles
        List<Role> systemRoles = roleRepository.findByTenantIdIsNull();
        List<Role> tenantRoles = roleRepository.findByTenantId(tenantId);
        List<Role> all = new ArrayList<>(systemRoles);
        all.addAll(tenantRoles);
        return all.stream().sorted(Comparator.comparingInt(Role::getPriority).reversed()).toList();
    }

    @Transactional(readOnly = true)
    public List<Role> getSystemRoles() {
        return roleRepository.findByTenantIdIsNull()
                .stream().sorted(Comparator.comparingInt(Role::getPriority).reversed()).toList();
    }

    /**
     * PORT Develop1 (§G5.5-58) — projection attendue par l'editeur de roles du
     * frontend : {@code GET /api/v1/admin/roles}.
     *
     * <p>Les entites JPA ne sont pas serialisables telles quelles ici : la collection
     * {@code permissions} est une association (objets imbriques, lazy) alors que
     * l'editeur veut des CLES de permission en chaines, et le champ {@code system}
     * doit apparaitre sous le nom {@code isSystem}. Les permissions sont lues en UNE
     * requete pour tous les roles (pas de N+1).</p>
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRolesForEditor(UUID tenantId) {
        List<Role> roles = getAllRoles(tenantId);
        Map<UUID, List<String>> keysByRole = new LinkedHashMap<>();
        if (!roles.isEmpty()) {
            for (Object[] row : roleRepository.findPermissionKeysByRoleIds(
                    roles.stream().map(Role::getId).toList())) {
                UUID roleId = (UUID) row[0];
                keysByRole.computeIfAbsent(roleId, k -> new ArrayList<>()).add((String) row[1]);
            }
        }
        // Phase 9 — cles pour lesquelles l'eglise a deja une surcouche : le role
        // global homonyme ne decide plus rien (AuthorizationService fait primer le
        // local), il ne doit donc pas paraitre « actif » dans l'editeur.
        Set<String> localKeys = roles.stream()
                .filter(r -> r.getTenantId() != null)
                .map(Role::getKey)
                .collect(Collectors.toSet());
        List<Map<String, Object>> views = new ArrayList<>(roles.size());
        for (Role role : roles) {
            boolean local = role.getTenantId() != null;
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", role.getId().toString());
            view.put("key", role.getKey());
            view.put("label", role.getLabel());
            view.put("description", role.getDescription());
            view.put("priority", role.getPriority());
            view.put("isSystem", Boolean.TRUE.equals(role.getSystem()));
            view.put("permissions", keysByRole.getOrDefault(role.getId(), List.of()));
            // Quatre champs ADDITIFS (Phase 9) : un client qui ne les lit pas
            // continue de raisonner exactement comme avant.
            view.put("local", local);
            view.put("effectiveForTenant", local || !localKeys.contains(role.getKey()));
            view.put("customizable", !local);
            view.put("customized", !local && localKeys.contains(role.getKey()));
            views.add(view);
        }
        return views;
    }

    @Transactional(readOnly = true)
    public List<Role> getCustomRoles(UUID tenantId) {
        return roleRepository.findByTenantId(tenantId)
                .stream().sorted(Comparator.comparingInt(Role::getPriority).reversed()).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Role> findById(UUID id) {
        return roleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Role> findByKey(UUID tenantId, String key) {
        Optional<Role> role = roleRepository.findByTenantIdAndKey(tenantId, key.toUpperCase());
        if (role.isEmpty() && tenantId != null) {
            role = roleRepository.findByTenantIdIsNullAndKey(key.toUpperCase());
        }
        return role;
    }

    @Transactional(readOnly = true)
    public Set<String> getRolePermissions(UUID roleId) {
        Optional<Role> role = roleRepository.findById(roleId);
        return role.map(r -> r.getPermissions().stream().map(Permission::getKey).collect(Collectors.toSet())).orElse(Set.of());
    }

    // ==================== PERMISSION CATALOG ====================

    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findByTenantIdIsNull(); // System permissions
    }

    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByCategory(String category) {
        return permissionRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByScope(PermissionScope scope) {
        return permissionRepository.findByScope(scope);
    }

    @Transactional(readOnly = true)
    public Map<String, List<Permission>> getPermissionsGroupedByCategory() {
        return getAllPermissions().stream()
                .collect(Collectors.groupingBy(p -> p.getCategory() != null ? p.getCategory() : "GENERAL"));
    }

    @Transactional(readOnly = true)
    public Map<PermissionScope, List<Permission>> getPermissionsGroupedByScope() {
        return getAllPermissions().stream()
                .collect(Collectors.groupingBy(Permission::getScope));
    }

    // ==================== CREATE CUSTOM ROLE ====================

    public Role createCustomRole(UUID tenantId, CreateRoleRequest request, UUID creatorId) {
        String key = request.key().toUpperCase();

        // Check key doesn't exist in tenant or system
        if (roleRepository.findByTenantIdAndKey(tenantId, key).isPresent()) {
            throw new BusinessRuleException("Un rôle avec cette clé existe déjà: " + key, "ROLE_KEY_EXISTS");
        }
        if (roleRepository.findByTenantIdIsNullAndKey(key).isPresent()) {
            throw new BusinessRuleException("Cette clé est réservée aux rôles système: " + key, "SYSTEM_ROLE_KEY");
        }

        Role role = Role.builder()
                .tenantId(tenantId)
                .key(key)
                .label(request.label())
                .description(request.description())
                .system(false)
                .priority(request.priority() != null ? request.priority() : 0)
                .build();

        // Assign permissions if provided
        if (request.permissionKeys() != null && !request.permissionKeys().isEmpty()) {
            role.setPermissions(resolvePermissionsStrict(request.permissionKeys()));
        }

        role = roleRepository.save(role);

        auditService.log(creatorId, tenantId, "ROLE_CREATED", "ROLE", role.getId(), "SUCCESS",
                Map.of("key", key, "label", request.label(), "custom", true), null, null, null);

        return role;
    }

    // ==================== SURCOUCHE LOCALE D'UN RÔLE (PARAMÉTRISATION, Phase 9) ====================

    /**
     * Instancie la <b>surcouche locale</b> d'un rôle global pour une église : une
     * ligne {@code roles} homonyme, {@code tenant_id} posé, {@code system = false},
     * dont les droits <em>héritent tels quels</em> de la matrice globale.
     *
     * <p><strong>Le blocage que lève cette méthode.</strong> La promesse de V258
     * (« chaque église peut ajuster les permissions de ses rôles ») n'était pas
     * tenable côté écriture : les six rôles historiques sont des lignes système
     * globales, {@code createCustomRole} refusait leur clé ({@code SYSTEM_ROLE_KEY})
     * et {@code assignPermissions} refusait les rôles système
     * ({@code CANNOT_MODIFY_SYSTEM_ROLE}). Aucune opération ne permettait donc de
     * créer la ligne que le moteur de résolution <em>sait déjà</em> lire :
     * {@code AuthorizationService} (lignes 150/182) et {@code
     * PermissionResolver.addRolePermissions} résolvent {@code
     * findByTenantIdAndKey(tenant, cle)} <em>en priorité</em>, avec repli sur la
     * ligne globale — vérifié par {@code
     * AuthorizationServiceEquivalenceBridgeTest}. Constat en base : 0 ligne {@code
     * roles.tenant_id NOT NULL}, c'est-à-dire 0 église effectivement paramétrable.
     *
     * <p><strong>Pourquoi l'héritage est la seule création sûre.</strong> Partir
     * d'une copie vide aurait, à l'instant de la création, <b>retiré tous les droits</b>
     * de tous les porteurs de ce rôle dans l'église (la surcouche prime). Partir de
     * la copie exacte est un no-op fonctionnel : la création ouvre uniquement la
     * <em>possibilité</em> d'affiner, et l'éditeur n'a plus qu'à retrancher ou
     * ajouter. C'est le même principe d'additivité que pour les gardes R7.
     *
     * <p>Opération <b>idempotente</b> : relancer sur une clé déjà surchargée renvoie
     * la surcouche existante, sans dupliquer ni écraser les réglages de l'église.
     *
     * @param globalRoleId identifiant de la ligne globale ({@code tenant_id IS NULL})
     * @param tenantId     église qui personnalise
     * @param actorId      auteur de l'opération (trace d'audit)
     * @return la surcouche locale, crée ou déjà existante
     */
    public Role instantiateLocalRoleCopy(UUID globalRoleId, UUID tenantId, UUID actorId) {
        Role global = roleRepository.findById(globalRoleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", globalRoleId));

        if (global.getTenantId() != null) {
            throw new BusinessRuleException(
                    "Ce rôle appartient déjà à une église : seul un rôle global peut être copié: "
                            + global.getKey(), "ROLE_NOT_GLOBAL");
        }

        Optional<Role> existing = roleRepository.findByTenantIdAndKey(tenantId, global.getKey());
        if (existing.isPresent()) {
            return existing.get(); // idempotent : ne jamais écraser une personnalisation en cours
        }

        Role local = Role.builder()
                .tenantId(tenantId)
                .key(global.getKey())
                .label(global.getLabel())
                .description(global.getDescription())
                .system(false) // la copie est modifiable ; le modèle global ne l'est pas
                .priority(global.getPriority())
                .permissions(new HashSet<>(global.getPermissions()))
                .build();
        local = roleRepository.save(local);

        auditService.log(actorId, tenantId, "ROLE_LOCAL_COPY_CREATED", "ROLE", local.getId(), "SUCCESS",
                Map.of("key", local.getKey(), "globalRoleId", String.valueOf(global.getId()),
                        "inheritedPermissions", local.getPermissions().size()), null, null, null);

        // La résolution bascule sur la copie pour tous les porteurs de la clé.
        // Droits identiques à cet instant : le bump ne change personne de statut,
        // il aligne le cache servi (et pousse PermissionsChanged) sur la nouvelle
        // source, ce qui rend l'éditeur immédiatement cohérent avec l'UI.
        bumpHolders(local, "ROLE_LOCAL_COPY_CREATED");
        return local;
    }

    /**
     * Réinitialise la personnalisation d'un rôle : supprime la surcouche locale et
     * ré-ancre ses porteurs sur la ligne globale homonyme, donc l'église revient aux
     * droits du catalogue système.
     *
     * <p>Le ré-ancrage est indispensable : sans lui, la suppression laisserait les
     * {@code tenant_memberships.role_id} en l'air (FK), et le garde-fou {@code
     * ROLE_IN_USE} de {@link #deleteRole} rendrait toute personnalisation
     * irréversible — un aller simple sans retour, ce qui n'est pas paramétrer.
     *
     * @return la ligne globale retrouvée
     */
    public Role revertLocalRoleCopy(UUID localRoleId, UUID actorId) {
        Role local = roleRepository.findById(localRoleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", localRoleId));
        if (local.getTenantId() == null) {
            throw new BusinessRuleException(
                    "Ce rôle est global, il n'a pas de personnalisation locale à réinitialiser: "
                            + local.getKey(), "ROLE_NOT_LOCAL");
        }
        Role global = roleRepository.findByTenantIdIsNullAndKey(local.getKey())
                .orElseThrow(() -> new BusinessRuleException(
                        "Aucun rôle global « " + local.getKey() + " » sur lequel revenir : "
                                + "supprimez ce rôle personnalisé plutôt que de le réinitialiser",
                        "GLOBAL_ROLE_MISSING"));

        List<TenantMembership> holders = membershipRoleQueries.findAllByRoleId(local.getId());
        for (TenantMembership membership : holders) {
            membership.setRole(global);
            membershipRepository.save(membership);
        }
        roleRepository.delete(local);
        roleRepository.flush();

        auditService.log(actorId, local.getTenantId(), "ROLE_LOCAL_COPY_REVERTED", "ROLE", local.getId(),
                "SUCCESS", Map.of("key", local.getKey(), "globalRoleId", String.valueOf(global.getId()),
                        "reassignedMembers", holders.size()), null, null, null);

        // Les seuls comptes dont les droits changent sont ceux qui quittent la
        // surcouche pour la ligne globale : on ne bump donc qu'eux, et pas toutes
        // les organisations qui partagent pourtant la même clé globale.
        for (TenantMembership membership : holders) {
            permissionCache.bumpPermissions(membership.getTenantId(), membership.getUserId(),
                    "ROLE_LOCAL_COPY_REVERTED");
        }
        return global;
    }

    // ==================== UPDATE ROLE ====================

    public Role updateRole(UUID roleId, UpdateRoleRequest request, UUID updaterId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de modifier un rôle système", "CANNOT_MODIFY_SYSTEM_ROLE");
        }

        String oldLabel = role.getLabel();
        String oldDescription = role.getDescription();
        Integer oldPriority = role.getPriority();

        if (request.label() != null) role.setLabel(request.label());
        if (request.description() != null) role.setDescription(request.description());
        if (request.priority() != null) role.setPriority(request.priority());

        role = roleRepository.save(role);

        auditService.log(updaterId, role.getTenantId(), "ROLE_UPDATED", "ROLE", role.getId(), "SUCCESS",
                Payloads.of("oldLabel", oldLabel, "newLabel", request.label(),
                        "oldDescription", oldDescription, "newDescription", request.description(),
                        "oldPriority", oldPriority, "newPriority", request.priority()),
                null, null, null);

        return role;
    }

    // ==================== MANAGE ROLE PERMISSIONS ====================

    /**
     * Remplace la matrice d'un rôle. Le contrat d'entrée est désormais <b>strict</b> :
     * toute clé absente du catalogue canonique {@code permissions} fait un échec
     * explicite, au lieu d'être ignorée.
     *
     * <p>Avant Phase 9, la résolution se faisait par {@code findByKey(k).ifPresent(...)}
     * : une clé fantôme était silencieusement écartée, l'appel renvoyait un 200 et
     * l'éditeur affichait « enregistré ». Comme {@code TenantAdminRolesPage} construisait
     * sa liste de cases à cocher sur {@code /permissions/catalog} (l'ancien référentiel
     * {@code permission_catalog}, 56 clés dont 45 absentes du canonique, 492 clés
     * canoniques absentes de celui-là), l'administrateur pouvait cocher, valider, et ne
     * rien changer — le pire des modes de défaillance pour un produit dont c'est la
     * seule promesse.
     */
    public Role assignPermissions(UUID roleId, List<String> permissionKeys, UUID assignerId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de modifier les permissions d'un rôle système", "CANNOT_MODIFY_SYSTEM_ROLE");
        }
        if (permissionKeys == null) {
            // Un corps sans permissionKeys n'a jamais voulu dire « retirer tous les
            // droits » : auparavant c'était un 500 (NPE). Exigence explicite.
            throw new BusinessRuleException(
                    "Le champ permissionKeys est requis (envoyer [] pour vider la matrice)",
                    "PERMISSION_KEYS_REQUIRED");
        }

        Set<Permission> permissions = resolvePermissionsStrict(permissionKeys);

        role.setPermissions(permissions);
        role = roleRepository.save(role);

        auditService.log(assignerId, role.getTenantId(), "ROLE_PERMISSIONS_ASSIGNED", "ROLE", role.getId(), "SUCCESS",
                Map.of("permissionKeys", permissionKeys), null, null, null);

        // Phase 9 — sans ce bump, la matrice était juste en base mais pas servie :
        // GET /me/permissions et permission_versions restaient à l'ancien état
        // jusqu'à la reconnexion du concerné (D10 de l'audit).
        bumpHolders(role, "ROLE_PERMISSIONS_ASSIGNED");

        return role;
    }

    public Role addPermission(UUID roleId, String permissionKey, UUID assignerId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de modifier un rôle système", "CANNOT_MODIFY_SYSTEM_ROLE");
        }

        role.getPermissions().addAll(resolvePermissionsStrict(List.of(permissionKey)));
        role = roleRepository.save(role);

        bumpHolders(role, "ROLE_PERMISSION_ADDED");

        return role;
    }

    public Role removePermission(UUID roleId, String permissionKey, UUID assignerId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de modifier un rôle système", "CANNOT_MODIFY_SYSTEM_ROLE");
        }

        role.getPermissions().removeIf(p -> p.getKey().equals(permissionKey));
        role = roleRepository.save(role);

        bumpHolders(role, "ROLE_PERMISSION_REMOVED");

        return role;
    }

    // ==================== DELETE CUSTOM ROLE ====================

    public void deleteRole(UUID roleId, UUID deleterId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de supprimer un rôle système", "CANNOT_DELETE_SYSTEM_ROLE");
        }

        // Phase 9 — une surcouche locale homonyme d'un rôle global n'est pas un
        // rôle « en usage » au sens strict : ses porteurs ont un rôle global sur
        // lequel revenir. Le refus ROLE_IN_USE rendrait la personnalisation
        // définitive ; on réinitialise donc (ré-ancrage + suppression de la copie),
        // ce qui est exactement l'intention de l'administrateur.
        if (role.getTenantId() != null
                && roleRepository.findByTenantIdIsNullAndKey(role.getKey()).isPresent()) {
            revertLocalRoleCopy(roleId, deleterId);
            return;
        }

        // Check if role is assigned to any membership
        long assignedCount = membershipRepository.countByRoleId(roleId);
        if (assignedCount > 0) {
            throw new BusinessRuleException("Impossible de supprimer: rôle assigné à " + assignedCount + " utilisateur(s)", "ROLE_IN_USE");
        }

        UUID tenantId = role.getTenantId();
        roleRepository.delete(role);

        auditService.log(deleterId, tenantId, "ROLE_DELETED", "ROLE", roleId, "SUCCESS",
                Map.of("key", role.getKey(), "label", role.getLabel()), null, null, null);
    }

    // ==================== ROLE ASSIGNMENT TO USERS ====================

    public TenantMembership assignRoleToUser(UUID userId, UUID tenantId, UUID roleId, UUID scopeId, UUID assignerId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        // Check user exists in tenant.
        // F17 : lecture par liste — plusieurs périmètres ACTIVE sont possibles
        // dans la même organisation (spéc §1.3) ; un Optional lèverait
        // IncorrectResultSizeDataAccessException. On applique le rôle à la
        // ligne de portée TENANT (celle qui porte les droits d'organisation).
        List<TenantMembership> existingMemberships = membershipRepository
                .findAllByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE);
        Optional<TenantMembership> existing = existingMemberships.stream()
                .min(Comparator.comparing(
                                (TenantMembership m) -> m.getScopeType() == MembershipScopeType.TENANT ? 0 : 1)
                        .thenComparing(m -> m.getJoinedAt() == null ? java.time.Instant.EPOCH : m.getJoinedAt()));
        if (existing.isPresent()) {
            // Update existing membership
            TenantMembership membership = existing.get();
            membership.setRole(role);
            if (scopeId != null) {
                membership.setScopeId(scopeId);
                membership.setScopeType(MembershipScopeType.CHURCH); // Default, could be parameter
            }
            membershipRepository.save(membership);
            // Phase 9 — changer de rôle change la matrice effective : à servir.
            permissionCache.bumpPermissions(tenantId, userId, "ROLE_ASSIGNED");
            return membership;
        }

        // Create new membership
        TenantMembership membership = TenantMembership.builder()
                .tenantId(tenantId)
                .userId(userId)
                .role(role)
                .scopeType(MembershipScopeType.TENANT)
                .scopeId(scopeId)
                .status(MembershipStatus.ACTIVE)
                .invitedBy(assignerId)
                .build();
        membershipRepository.save(membership);

        auditService.log(assignerId, tenantId, "ROLE_ASSIGNED", "MEMBERSHIP", membership.getId(), "SUCCESS",
                Payloads.of("userId", userId.toString(), "roleKey", role.getKey(), "scopeId", scopeId != null ? scopeId.toString() : null), null, null, null);

        permissionCache.bumpPermissions(tenantId, userId, "ROLE_ASSIGNED");

        return membership;
    }

    // ==================== ROLE HIERARCHY & INHERITANCE ====================

    @Transactional(readOnly = true)
    public List<Role> getRoleHierarchy(UUID tenantId) {
        List<Role> allRoles = getAllRoles(tenantId);
        // Sort by priority (highest first) - represents hierarchy
        return allRoles.stream()
                .sorted(Comparator.comparingInt(Role::getPriority).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Role, List<Role>> getRoleChildren(UUID tenantId) {
        List<Role> allRoles = getRoleHierarchy(tenantId);
        Map<Role, List<Role>> children = new LinkedHashMap<>();

        for (int i = 0; i < allRoles.size(); i++) {
            Role parent = allRoles.get(i);
            List<Role> childRoles = allRoles.subList(i + 1, allRoles.size());
            children.put(parent, childRoles);
        }

        return children;
    }

    // ==================== VALIDATION ====================

    @Transactional(readOnly = true)
    public ValidationResult validateRolePermissions(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Check for conflicting permissions (e.g., READ + DELETE without UPDATE)
        Set<String> perms = role.getPermissions().stream().map(Permission::getKey).collect(Collectors.toSet());

        if (perms.contains("MEMBER_DELETE") && !perms.contains("MEMBER_UPDATE")) {
            warnings.add("DELETE sans UPDATE: l'utilisateur peut supprimer mais pas modifier");
        }
        if (perms.contains("TENANT_SETTINGS_UPDATE") && !perms.contains("TENANT_SETTINGS_READ")) {
            warnings.add("UPDATE settings sans READ: interface peut ne pas afficher les valeurs actuelles");
        }
        if (perms.contains("FINANCE_MANAGE") && !perms.contains("FINANCE_READ")) {
            warnings.add("Gestion finances sans lecture: risqué");
        }

        return new ValidationResult(errors.isEmpty(), warnings, errors);
    }

    // ==================== HELPERS PARAMÉTRISATION (Phase 9) ====================

    /**
     * Résout des clés de permission en exigeant qu'elles existent au catalogue
     * canonique {@code permissions}. Tolère la casse et les espaces (les clés sont
     * stockées en majuscules), élimine les doublons, et nomme les fautes :
     * l'administrateur voit <em>quelle</em> clé il vient d'envoyer.
     *
     * @throws BusinessRuleException {@code UNKNOWN_PERMISSION_KEYS} (HTTP 400) si
     *         une clé au moins est absente du catalogue
     */
    private Set<Permission> resolvePermissionsStrict(List<String> permissionKeys) {
        if (permissionKeys == null || permissionKeys.isEmpty()) {
            return new HashSet<>();
        }
        List<String> requested = permissionKeys.stream()
                .filter(Objects::nonNull)
                .map(k -> k.trim().toUpperCase(Locale.ROOT))
                .filter(k -> !k.isEmpty())
                .distinct()
                .toList();
        if (requested.isEmpty()) {
            return new HashSet<>();
        }
        Map<String, Permission> known = new LinkedHashMap<>();
        for (Permission permission : permissionRepository.findByKeyIn(requested)) {
            known.put(permission.getKey().toUpperCase(Locale.ROOT), permission);
        }
        List<String> unknown = requested.stream().filter(k -> !known.containsKey(k)).toList();
        if (!unknown.isEmpty()) {
            throw new BusinessRuleException(
                    "Clé(s) de permission inconnue(s) du catalogue: " + String.join(", ", unknown),
                    "UNKNOWN_PERMISSION_KEYS");
        }
        return new HashSet<>(known.values());
    }

    /**
     * Recalcule et publie les permissions de tous les porteurs actifs de la clé du
     * rôle, dans le ou les tenants concernés.
     *
     * <p>Deux sens à couvrir : une surcouche locale ne concerne que son église ;
     * une ligne globale (rôle non système éditable, ou retour d'une surcouche) touche
     * toutes les organisations qui l'utilisent. Le premier cas est borné à une
     * église, le second est l'opération rare et volontaire d'un administrateur
     * réseau — d'où le coût linéaire en nombre de membres, accepté.
     */
    private void bumpHolders(Role role, String changeType) {
        if (role == null || role.getKey() == null) {
            return;
        }
        if (role.getTenantId() != null) {
            for (UUID userId : membershipRoleQueries.findActiveUserIdsByTenantAndRoleKey(
                    role.getTenantId(), MembershipStatus.ACTIVE, role.getKey())) {
                permissionCache.bumpPermissions(role.getTenantId(), userId, changeType);
            }
            return;
        }
        for (Object[] row : membershipRoleQueries.findActiveHoldersByRoleKey(
                MembershipStatus.ACTIVE, role.getKey())) {
            permissionCache.bumpPermissions((UUID) row[0], (UUID) row[1], changeType);
        }
    }

    // ==================== RECORDS ====================

    public record CreateRoleRequest(
            String key,
            String label,
            String description,
            Integer priority,
            List<String> permissionKeys
    ) {}

    public record UpdateRoleRequest(
            String label,
            String description,
            Integer priority
    ) {}

    public record ValidationResult(
            boolean valid,
            List<String> warnings,
            List<String> errors
    ) {}
}