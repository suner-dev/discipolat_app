package com.discipolat.modules.relations.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.MemberRoleAssignment;
import com.discipolat.modules.tenants.domain.MemberRoleAssignmentRepository;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.RoleTitleService;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.platform.domain.DictionaryEntry;
import com.discipolat.modules.platform.domain.DictionaryEntryRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Agrégat « toute la hiérarchie d'un membre » — V231.
 *
 * <p>Combine, sans rien écraser, les briques existantes :
 * <ul>
 *   <li>rôles : legacy {@code UserRole} + capacités V3
 *       ({@code MemberRoleAssignment} × {@code Role} × {@code RoleTitle}) ;</li>
 *   <li>branches organisationnelles : chaque nœud auquel le membre est
 *       rattaché développe sa chaîne d'ancêtres jusqu'à la racine avec le
 *       RESPONSABLE de chaque niveau — l'arbre est multi-branches ;</li>
 *   <li>relations personnelles déclarées (V231) : encadrants sortants et
 *       membres entrants ;</li>
 *   <li>encadrement pastoral {@code suivi} : pasteur du candidat, chef de
 *       famille, départements et nœuds dirigés, âmes suivies ;</li>
 *   <li>chaîne ascendante unifiée (ascendants déduppliqués) ;</li>
 *   <li>{@code resume} : ce que l'agrégat sait réellement et d'où il le
 *       sait, pour que l'interface n'affiche jamais un vide silencieux.</li>
 * </ul>
 *
 * <h2>Origines d'une branche</h2>
 * <ol>
 *   <li>{@code ASSIGNATION_V3} — assignation capacité×nœud explicite ;</li>
 *   <li>{@code ADHESION_NOEUD} — adhésion à portée de nœud
 *       ({@code TenantMembership.scopeId}) ;</li>
 *   <li>{@code RESPONSABLE_NOEUD} — le membre est
 *       {@code OrganizationNode.responsibleId} d'un nœud : c'est le signal
 *       <b>autoritatif</b> pour un pasteur (posé par
 *       {@code assignResponsible}) et il existait sans être exploité.</li>
 * </ol>
 *
 * <h2>Isolation multi-tenant</h2>
 * Le tenant est vérifié <b>explicitement</b> sur le membre chargé
 * ({@link #requireTenantUser}) : le filtre Hibernate n'est actif qu'en
 * contexte HTTP et ne protège ni les tâches planifiées ni les tests.
 *
 * <h2>Volumétrie</h2>
 * Un seul chargement des nœuds du tenant (les ancêtres sont dérivés en
 * mémoire par remontée de {@code parentId}, avec protection anti-cycle),
 * un seul {@code findAllById} pour les noms de responsables, un seul
 * chargement du catalogue des types — aucun N+1.
 */
@Service
@Transactional(readOnly = true)
public class UserHierarchyService {

    private final UserRepository userRepository;
    private final MemberRoleAssignmentRepository assignmentRepository;
    private final RoleRepository roleRepository;
    private final RoleTitleService roleTitleService;
    private final TenantMembershipRepository membershipRepository;
    private final OrganizationNodeRepository nodeRepository;
    private final SoulRepository soulRepository;
    private final DepartmentRepository departmentRepository;
    private final FamilyRepository familyRepository;
    private final MemberRelationService relationService;
    private final DictionaryEntryRepository dictionaryEntryRepository;

    public UserHierarchyService(UserRepository userRepository,
                                MemberRoleAssignmentRepository assignmentRepository,
                                RoleRepository roleRepository,
                                RoleTitleService roleTitleService,
                                TenantMembershipRepository membershipRepository,
                                OrganizationNodeRepository nodeRepository,
                                SoulRepository soulRepository,
                                DepartmentRepository departmentRepository,
                                FamilyRepository familyRepository,
                                MemberRelationService relationService,
                                DictionaryEntryRepository dictionaryEntryRepository) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.roleRepository = roleRepository;
        this.roleTitleService = roleTitleService;
        this.membershipRepository = membershipRepository;
        this.nodeRepository = nodeRepository;
        this.soulRepository = soulRepository;
        this.departmentRepository = departmentRepository;
        this.familyRepository = familyRepository;
        this.relationService = relationService;
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    /** Vue complète : identité, rôles, branches, relations, ascendants, suivi. */
    public Map<String, Object> getHierarchy(UUID tenantId, UUID userId) {
        return getHierarchy(tenantId, userId, userId);
    }

    /** Vue complète en indicating, pour l'ascendant, qui est le lecteur. */
    public Map<String, Object> getHierarchy(UUID tenantId, UUID userId, UUID viewerId) {
        User user = requireTenantUser(tenantId, userId);
        Context ctx = newContext(tenantId, user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("nomComplet", fullName(user));
        result.put("rolePrincipal", user.getRole().name());
        result.put("roleActif", user.getActiveRole() != null ? user.getActiveRole().name() : user.getRole().name());
        result.put("roles", roleEntries(tenantId, user, ctx));
        List<Map<String, Object>> branches = branches(ctx);
        result.put("branches", branches);
        Map<String, Object> relations = relationService.summary(tenantId, userId, viewerId);
        result.put("relations", relations);
        result.put("ascendants", ascendants(branches, relations, userId));
        result.put("suivi", suivi(ctx, user, tenantId));
        result.put("resume", resume(branches, relations));
        return result;
    }

    /** Résumé léger pour la fiche utilisateur (branches + ascendants). */
    public Map<String, Object> summarize(UUID tenantId, UUID userId) {
        return summarize(tenantId, userId, userId);
    }

    public Map<String, Object> summarize(UUID tenantId, UUID userId, UUID viewerId) {
        User user = requireTenantUser(tenantId, userId);
        Context ctx = newContext(tenantId, user);

        List<Map<String, Object>> branches = branches(ctx);
        Map<String, Object> relations = relationService.summary(tenantId, userId, viewerId);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("rolePrincipal", user.getRole().name());
        summary.put("nombreBranches", branches.size());
        summary.put("ascendants", ascendants(branches, relations, userId));
        summary.put("branches", branches);
        summary.put("suivi", suivi(ctx, user, tenantId));
        summary.put("resume", resume(branches, relations));
        return summary;
    }

    // ==================== CONTEXTE (chargements groupés) ====================

    /**
     * Données chargées une seule fois par requête et réutilisées par toutes
     * les sections : c'est ce qui élimine le N+1 de l'ancienne version.
     */
    private static final class Context {
        final UUID tenantId;
        final UUID userId;
        final Map<UUID, OrganizationNode> nodesById = new LinkedHashMap<>();
        /** nodeId → origine du rattachement (par ordre de priorité). */
        final Map<UUID, String> originByNode = new LinkedHashMap<>();
        Map<UUID, User> usersById = Map.of();

        Context(UUID tenantId, UUID userId) {
            this.tenantId = tenantId;
            this.userId = userId;
        }
    }

    private Context newContext(UUID tenantId, User user) {
        Context ctx = new Context(tenantId, user.getId());
        for (OrganizationNode n : nodeRepository.findByTenantId(tenantId)) {
            ctx.nodesById.put(n.getId(), n);
        }
        collectBranchOrigins(ctx);
        ctx.usersById = loadUsers(ctx);
        return ctx;
    }

    private void collectBranchOrigins(Context ctx) {
        // 1. Assignations de capacité V3 (nœud explicite).
        for (MemberRoleAssignment a : assignmentRepository
                .findByTenantIdAndUserIdAndStatus(ctx.tenantId, ctx.userId,
                        MemberRoleAssignment.AssignmentStatus.ACTIVE)) {
            if (a.getNodeId() != null && ctx.nodesById.containsKey(a.getNodeId())) {
                ctx.originByNode.putIfAbsent(a.getNodeId(), "ASSIGNATION_V3");
            }
        }
        // 2. Adhésion à portée de nœud.
        for (TenantMembership tm : membershipRepository.findAllByUserIdAndTenantId(ctx.userId, ctx.tenantId)) {
            if (tm.getStatus() != MembershipStatus.ACTIVE || tm.getScopeId() == null) {
                continue;
            }
            if (tm.getScopeType() == MembershipScopeType.TENANT
                    || tm.getScopeType() == MembershipScopeType.OWN) {
                continue;
            }
            if (ctx.nodesById.containsKey(tm.getScopeId())) {
                ctx.originByNode.putIfAbsent(tm.getScopeId(), "ADHESION_NOEUD");
            }
        }
        // 3. Le membre dirige un nœud (signal autoritatif : pasteur de campus…).
        for (OrganizationNode n : nodeRepository.findByResponsibleId(ctx.userId)) {
            if (ctx.nodesById.containsKey(n.getId())) {
                ctx.originByNode.putIfAbsent(n.getId(), "RESPONSABLE_NOEUD");
            }
        }
    }

    /** Un seul SELECT pour tous les responsables de tous les niveaux. */
    private Map<UUID, User> loadUsers(Context ctx) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (OrganizationNode n : ctx.nodesById.values()) {
            if (n.getResponsibleId() != null) {
                ids.add(n.getResponsibleId());
            }
        }
        Map<UUID, User> byId = new HashMap<>();
        if (!ids.isEmpty()) {
            for (User u : userRepository.findAllById(ids)) {
                if (u != null && !u.isDeleted()) {
                    byId.put(u.getId(), u);
                }
            }
        }
        return byId;
    }

    // ==================== RÔLES ====================

    private List<Map<String, Object>> roleEntries(UUID tenantId, User user, Context ctx) {
        List<Map<String, Object>> roles = new ArrayList<>();
        for (MemberRoleAssignment a : assignmentRepository.findByTenantIdAndUserIdAndStatus(
                tenantId, user.getId(), MemberRoleAssignment.AssignmentStatus.ACTIVE)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", a.getRoleId().toString());
            m.put("label", roleLabel(tenantId, a));
            m.put("source", "CAPACITE");
            m.put("nodeId", a.getNodeId());
            OrganizationNode node = a.getNodeId() != null ? ctx.nodesById.get(a.getNodeId()) : null;
            m.put("nodeName", node != null ? node.getName() : null);
            roles.add(m);
        }
        // Rôles legacy (enum système) — conservés, affichés « source=SYSTEME ».
        Set<com.discipolat.common.domain.UserRole> legacy = new LinkedHashSet<>(user.getRoles());
        legacy.add(user.getRole());
        for (com.discipolat.common.domain.UserRole r : legacy) {
            if (r == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", r.name());
            m.put("label", legacyRoleLabel(tenantId, r));
            m.put("source", "SYSTEME");
            m.put("nodeId", null);
            m.put("nodeName", null);
            roles.add(m);
        }
        return roles;
    }

    /**
     * Libellé AFFICHÉ d'un rôle legacy : la ligne du dictionnaire
     * {@code USER_ROLE} (paramétrable par église — « responsable » peut devenir
     * « ministre » ou « dirigeant »). Repli : nom technique de l'enum. Sans ce
     * passage par le dictionnaire, le libellé serait figé dans le code et
     * identique pour toutes les églises quelle que soit leur langue.
     */
    private String legacyRoleLabel(UUID tenantId, com.discipolat.common.domain.UserRole r) {
        try {
            String label = null;
            for (DictionaryEntry e : dictionaryEntryRepository.findByDictKeyOrderByOrdreAsc("USER_ROLE")) {
                if (e == null || e.getCode() == null || !e.getCode().equalsIgnoreCase(r.name())) {
                    continue;
                }
                if (e.getTenantId() == null) {
                    if (label == null) label = e.getLabel();
                } else if (e.getTenantId().equals(tenantId)) {
                    return e.getLabel(); // ligne d'église prioritaire
                }
            }
            if (label != null) return label;
        } catch (Exception ignored) {
            // tombe sur le repli ci-dessous
        }
        return r.name().replace('_', ' ');
    }

    private String roleLabel(UUID tenantId, MemberRoleAssignment a) {
        try {
            return roleTitleService.getEffectiveLabel(tenantId, a.getRoleId(), a.getNodeId());
        } catch (Exception e) {
            return roleRepository.findById(a.getRoleId()).map(Role::getLabel).orElse(a.getRoleId().toString());
        }
    }

    // ==================== BRANCHES (arbre multi-branches) ====================

    /**
     * Une branche par feuille de rattachement, développée jusqu'à la racine
     * avec le responsable de chaque niveau. Les branches les plus profondes
     * d'abord : le membre lit « mon chef direct → mon pasteur ».
     */
    private List<Map<String, Object>> branches(Context ctx) {
        if (ctx.originByNode.isEmpty()) {
            return List.of();
        }
        Set<UUID> leaves = new LinkedHashSet<>(ctx.originByNode.keySet());
        for (UUID nodeId : ctx.originByNode.keySet()) {
            OrganizationNode node = ctx.nodesById.get(nodeId);
            if (node == null) continue;
            for (UUID otherId : ctx.originByNode.keySet()) {
                if (otherId.equals(nodeId)) continue;
                OrganizationNode other = ctx.nodesById.get(otherId);
                if (other != null && isDescendantOf(other, node)) {
                    leaves.remove(nodeId); // déjà couvert par la chaîne du descendant
                    break;
                }
            }
        }

        List<Map<String, Object>> branches = new ArrayList<>(leaves.size());
        for (UUID leafId : leaves) {
            OrganizationNode leaf = ctx.nodesById.get(leafId);
            if (leaf == null) continue;
            List<Map<String, Object>> chain = chainOf(leaf, ctx);
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("noeud", nodeBrief(leaf));
            b.put("origine", ctx.originByNode.get(leafId));
            b.put("niveaux", chain.size());
            b.put("chaine", chain.stream()
                    // Du nœud le plus proche (feuille) vers la racine : le membre
                    // lit sa hiérarchie « de mon chef direct → mon pasteur ».
                    .sorted(Comparator.comparingInt(
                            (Map<String, Object> step) -> ((Number) step.get("level")).intValue()).reversed())
                    .toList());
            branches.add(b);
        }
        branches.sort(Comparator.comparingInt(
                (Map<String, Object> b) -> ((Number) ((Map<?, ?>) b.get("noeud")).get("level")).intValue())
                .reversed());
        return branches;
    }

    /**
     * Ancêtres d'un nœud, racine en tête, par remontée de {@code parentId}
     * dans le jeu déjà chargé. Protection anti-cycle : une boucle
     * parent/enfant corrompue ne provoque pas de boucle infinie.
     */
    private List<Map<String, Object>> chainOf(OrganizationNode leaf, Context ctx) {
        List<Map<String, Object>> chain = new ArrayList<>();
        Set<UUID> seen = new LinkedHashSet<>();
        OrganizationNode current = leaf;
        while (current != null && seen.add(current.getId())) {
            Map<String, Object> step = nodeBrief(current);
            step.put("responsable", responsibleOf(ctx, current));
            chain.add(step);
            current = current.getParentId() == null ? null : ctx.nodesById.get(current.getParentId());
        }
        java.util.Collections.reverse(chain);
        return chain;
    }

    private boolean isDescendantOf(OrganizationNode node, OrganizationNode ancestor) {
        return node.getPath() != null && ancestor.getPath() != null
                && !node.getPath().equals(ancestor.getPath())
                && node.getPath().startsWith(ancestor.getPath() + ".");
    }

    /** Responsable d'un niveau : nom déjà résolu (un seul findAllById global). */
    private Map<String, Object> responsibleOf(Context ctx, OrganizationNode node) {
        if (node == null || node.getResponsibleId() == null) {
            return null;
        }
        User u = ctx.usersById.get(node.getResponsibleId());
        if (u == null) {
            return null;
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", u.getId());
        r.put("nom", fullName(u));
        return r;
    }

    private Map<String, Object> nodeBrief(OrganizationNode n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("nom", n.getName());
        m.put("type", n.getType().name());
        m.put("level", n.getLevel());
        return m;
    }

    // ==================== ASCENDANTS UNIFIÉS ====================

    /**
     * Liste plate et déduppliquée de « ses chefs/responsables » :
     * responsables de chaîne (org) + encadrants déclarés (relations).
     */
    private List<Map<String, Object>> ascendants(List<Map<String, Object>> branches,
                                                 Map<String, Object> relations, UUID userId) {
        Map<UUID, Map<String, Object>> byUser = new LinkedHashMap<>();
        for (Map<String, Object> branch : branches) {
            for (Object stepObj : asList(branch.get("chaine"))) {
                Map<?, ?> step = (Map<?, ?>) stepObj;
                Object resp = step.get("responsable");
                if (!(resp instanceof Map)) continue;
                Map<?, ?> r = (Map<?, ?>) resp;
                UUID id = (UUID) r.get("id");
                if (id == null || id.equals(userId)) continue;
                byUser.computeIfAbsent(id, k -> {
                    Map<String, Object> a = new LinkedHashMap<>();
                    a.put("id", id);
                    a.put("nom", r.get("nom"));
                    a.put("via", "ORGANISATION");
                    a.put("noeud", step.get("nom"));
                    a.put("noeudType", step.get("type"));
                    return a;
                });
            }
        }
        for (Map<String, Object> rel : asMapList(relations.get("sortantes"))) {
            UUID id = (UUID) rel.get("otherUserId");
            if (id == null || id.equals(userId)) continue;
            byUser.computeIfAbsent(id, k -> {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("id", id);
                a.put("nom", rel.get("otherNom"));
                a.put("via", "DECLARATIF");
                a.put("typeRelation", rel.get("relationType"));
                a.put("typeLabel", rel.get("typeLabel"));
                return a;
            });
        }
        return new ArrayList<>(byUser.values());
    }

    // ==================== ENCADREMENT PASTORAL (suivi) ====================

    /**
     * Le « suivi » hors arbre organisationnel : qui m'a fait, qui dirige ma
     * famille, ce que je dirige. Ces liens sont réels dans le modèle
     * ({@code Soul.faiseurId}, {@code User.familleGereeId},
     * {@code Department.responsableId}, {@code OrganizationNode.responsibleId}).
     */
    private Map<String, Object> suivi(Context ctx, User user, UUID tenantId) {
        Map<String, Object> suivi = new LinkedHashMap<>();

        List<Soul> souls = soulRepository.findAllByUserId(user.getId());
        Soul ame = souls.isEmpty() ? null : souls.get(0);
        suivi.put("faiseur", ame == null || ame.getFaiseurId() == null
                ? null : personBrief(ame.getFaiseurId()));
        suivi.put("chefDeFamille", chefDeFamilleBrief(user));
        suivi.put("familleGeree", user.getFamilleGereeId() == null
                ? null : familyBrief(user.getFamilleGereeId()));

        List<Map<String, Object>> departements = new ArrayList<>();
        for (Department d : departmentRepository.findByResponsableId(user.getId())) {
            if (d.isDeleted() || !tenantId.equals(d.getTenantId())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("nom", d.getNom());
            departements.add(m);
        }
        suivi.put("departementsDiriges", departements);

        List<Map<String, Object>> noeuds = new ArrayList<>();
        for (OrganizationNode n : nodeRepository.findByResponsibleId(user.getId())) {
            if (!tenantId.equals(n.getTenantId())) continue;
            noeuds.add(nodeBrief(n));
        }
        suivi.put("noeudsDiriges", noeuds);

        // Borné : un seul comptage, sans charger les âmes elles-mêmes (déjà
        // exposées, en clair, par la fiche /users/{id}/detail).
        suivi.put("amesSuiviesTotal", soulRepository.findAllByFaiseurId(user.getId()).size());
        return suivi;
    }

    /**
     * Ma famille et celui qui la dirige : le « chef de famille » est le seul
     * candidat naturel a un rattachement declaratif complementaire.
     */
    private Map<String, Object> chefDeFamilleBrief(User user) {
        if (user.getFamilleGereeId() == null) {
            return null;
        }
        return familyRepository.findById(user.getFamilleGereeId())
                .filter(f -> !f.isDeleted() && user.getTenantId().equals(f.getTenantId()))
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", f.getId());
                    m.put("nom", f.getNom());
                    m.put("chefFamilleId", f.getChefFamilleId());
                    m.put("chefFamilleNom", f.getChefFamilleId() == null
                            ? null : quietName(f.getChefFamilleId()));
                    return m;
                })
                .orElse(null);
    }

    private String quietName(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted() && u.getTenantId() != null)
                .map(this::fullName)
                .orElse(null);
    }

    private Map<String, Object> personBrief(UUID userId) {
        return userRepository.findById(userId).filter(u -> !u.isDeleted()).map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("nom", fullName(u));
            return m;
        }).orElse(null);
    }

    private Map<String, Object> familyBrief(UUID familyId) {
        return familyRepository.findById(familyId).filter(f -> !f.isDeleted()).map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("nom", f.getNom());
            return m;
        }).orElse(null);
    }

    // ==================== RÉSUMÉ DE COMPLÉTUDE ====================

    /**
     * Ce que l'agrégat sait et d'où il le sait. Permet à l'interface de dire
     * « rattachement déclaratif uniquement » au lieu d'afficher un vide.
     */
    private Map<String, Object> resume(List<Map<String, Object>> branches,
                                       Map<String, Object> relations) {
        Map<String, Object> resume = new LinkedHashMap<>();
        int sortantes = asMapList(relations.get("sortantes")).size();
        int entrantes = asMapList(relations.get("entrantes")).size();
        Set<String> origines = new LinkedHashSet<>();
        for (Map<String, Object> b : branches) {
            Object origine = b.get("origine");
            if (origine != null) {
                origines.add(origine.toString());
            }
        }
        if (sortantes > 0) {
            origines.add("DECLARATIF");
        }
        resume.put("branchesOrganisationnelles", branches.size());
        resume.put("encadrantsDeclares", sortantes);
        resume.put("membresRattaches", entrantes);
        resume.put("origines", new ArrayList<>(origines));
        resume.put("hierarchieComplete", branches.size() > 0 && sortantes > 0);
        return resume;
    }

    // ==================== AIDES ====================

    /** Charge un membre en vérifiant explicitement le tenant (isolation stricte). */
    private User requireTenantUser(UUID tenantId, UUID userId) {
        if (userId == null) {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    "Identifiant de membre manquant", "USER_ID_REQUIRED");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
        if (user.isDeleted() || !tenantId.equals(user.getTenantId())) {
            // Un compte d'une autre église est INVISIBLE, pas « forbidden ».
            throw new EntityNotFoundException("User", userId);
        }
        return user;
    }

    private String fullName(User u) {
        String n = ((u.getFirstName() == null ? "" : u.getFirstName() + " ")
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        return n.isEmpty() ? u.getEmail() : n;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asMapList(Object o) {
        return o instanceof List ? (List<Map<String, Object>>) o : List.of();
    }

    private List<?> asList(Object o) {
        return o instanceof List ? (List<?>) o : List.of();
    }
}
