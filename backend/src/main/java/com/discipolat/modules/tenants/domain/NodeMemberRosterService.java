package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Roster « les membres d'une entité » — extension de la hiérarchie ecclésiastique.
 *
 * <p>Comble le manque « consulter les membres à chaque échelle » : depuis
 * n'importe quel nœud (dénomination via la racine, région, district, campus,
 * église locale, département/cellule), retrouver la LISTE paginée des membres
 * qui relèvent de ce nœud, soit <b>directement</b> (rattachés à CE nœud), soit
 * <b>indirectement</b> (rattachés à une sous-structure), sans doublons.
 *
 * <h2>Rattachements pris en compte (les mêmes origines que V231)</h2>
 * <ol>
 *   <li>{@code ASSIGNATION_V3} — {@link MemberRoleAssignment} ACTIVE portée sur
 *       un nœud du sous-arbre ;</li>
 *   <li>{@code ADHESION_NOEUD} — {@link TenantMembership} ACTIVE dont la
 *       {@code scopeId} <b>est</b> un nœud du sous-arbre ;</li>
 *   <li>{@code RESPONSABLE_NOEUD} — le membre est {@code responsibleId} d'un
 *       nœud du sous-arbre (signal autoritatif du pasteur de campus).</li>
 * </ol>
 * Les adhésions de portée {@code TENANT}/{@code OWN} ne rattachent à AUCUN
 * nœud particulier : les incrémenter partout était précisément le défaut B2
 * de {@code getMembersWithAccess} (le roster d'un campus noyait sous les
 * « membres du tenant »). Ici, une portée TENANT ne rend visible le membre
 * que sous la <b>racine</b>, jamais sous un nœud intermédiaire.
 *
 * <h2>Déduplication</h2>
 * Un membre rattaché par plusieurs chemins n'apparaît qu'une fois, au point
 * d'attachement le plus proche du nœud consulté (règle éprouvée du
 * {@code distinct()} de {@link NodeAggregateService}, appliquée à l'unité).
 *
 * <h2>Données manquantes</h2>
 * Rien n'est inventé : pas de rattachement → liste vide explicite ; responsable
 * non nommé → {@code responsibleId/null} dans la vue. Le chemin hiérarchique
 * ({@code viaChaine}) n'est fourni que si le rattachement est réel.
 *
 * <h2>Isolation multi-tenant</h2>
 * Le tenant est issu du contexte serveur (le contrôleur ne le reçoit jamais du
 * client) et le nœud est <b>revérifié</b> dans le tenant : un nœud d'une autre
 * église est INVISIBLE (404), jamais « forbidden » — même contrat que
 * {@code UserHierarchyService.requireTenantUser}.
 *
 * <h2>Volumétrie</h2>
 * Un seul chargement des nœuds du tenant (le sous-arbre est dérivé en mémoire
 * par {@code parentId}, avec protection anti-cycle), puis un {@code findAllById}
 * groupé pour les comptes — aucun N+1. La pagination est appliquée en mémoire
 * sur l'ensemble filtré, comme l'annuaire {@code /users} page le sien ; taille
 * de page bornée.
 */
@Service
@Transactional(readOnly = true)
public class NodeMemberRosterService {

    /** Portée de la consultation. */
    public enum RosterScope {
        /** Membres rattachés à CE nœud exactement. */
        DIRECT,
        /** Membres rattachés à ce nœud ou à l'une de ses sous-structures. */
        SUBTREE
    }

    private static final int MAX_PAGE_SIZE = 100;

    private final OrganizationNodeRepository nodeRepository;
    private final TenantMembershipRepository membershipRepository;
    private final MemberRoleAssignmentRepository assignmentRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public NodeMemberRosterService(OrganizationNodeRepository nodeRepository,
                                   TenantMembershipRepository membershipRepository,
                                   MemberRoleAssignmentRepository assignmentRepository,
                                   RoleRepository roleRepository,
                                   UserRepository userRepository) {
        this.nodeRepository = nodeRepository;
        this.membershipRepository = membershipRepository;
        this.assignmentRepository = assignmentRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    /** Ligne du roster : identité + rattachement réel + rôle porté à ce nœud. */
    public record NodeMemberRow(
            UUID userId,
            String firstName,
            String lastName,
            String email,
            String photoUrl,
            String statut,
            /** Nœud le plus proche auquel le membre est rattaché. */
            UUID attachedNodeId,
            String attachedNodeName,
            String attachedNodeType,
            /** true si le rattachement est sur le nœud consulté lui-même. */
            boolean direct,
            /** ASSIGNATION_V3 | ADHESION_NOEUD | RESPONSABLE_NOEUD. */
            String origine,
            /** Libellés des rôles/capacités portés sur le sous-arbre consulté. */
            List<String> roles,
            /** Chaîne du nœud consulté jusqu'au point d'attachement (fil d'Ariane). */
            List<Map<String, Object>> viaChaine,
            java.time.Instant depuis
    ) {
    }

    /** Compteurs cohérents du roster (directs ⊆ sous-arbre, dédupliqués). */
    public record RosterCounters(long memberCountDirect, long memberCountSousArbre) {
    }

    /**
     * Roster paginé des membres d'un nœud.
     *
     * @param tenantId tenant du contexte serveur, revérifié sur le nœud
     * @param nodeId   nœud consulté (église, région, district, campus, cellule…)
     * @param scope    DIRECT = rattachés ici ; SUBTREE = ici + sous-structures
     * @param q        filtre libre (nom, prénom, email), insensible à la casse
     * @param page     page 0-based
     * @param size     taille de page, bornée à {@value #MAX_PAGE_SIZE}
     */
    public Map<String, Object> roster(UUID tenantId, UUID nodeId, RosterScope scope,
                                      String q, int page, int size) {
        OrganizationNode node = requireNode(tenantId, nodeId);
        Map<UUID, OrganizationNode> nodesById = loadTenantNodes(tenantId);
        Set<UUID> subtreeIds = scope == RosterScope.DIRECT
                ? Set.of(nodeId)
                : descendantIds(node, nodesById);

        Map<UUID, Attachment> best = collectAttachments(tenantId, node, subtreeIds, nodesById);

        // Dédup : un seul point d'attachement par membre (le plus proche du nœud),
        // arbitré dans collectAttachments — une ligne = un membre, jamais de doublon
        // quand deux chemins (adhésion + capacité) mènent au même sous-arbre.
        List<Attachment> rows = new ArrayList<>(best.values());

        String needle = q == null ? null : q.trim().toLowerCase();
        Map<UUID, User> users = loadUsers(rows);
        List<NodeMemberRow> view = new ArrayList<>();
        long countDirect = 0;
        for (Attachment a : rows) {
            User u = users.get(a.userId);
            if (u == null || u.isDeleted()) continue;
            if (needle != null && !matches(u, needle)) continue;
            if (a.direct) countDirect++;
            view.add(toRow(a, u, nodesById, node));
        }

        view.sort(Comparator.comparing(NodeMemberRow::lastName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(NodeMemberRow::firstName, Comparator.nullsLast(String::compareToIgnoreCase)));

        int clampSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int clampPage = Math.max(0, page);
        int from = Math.min(clampPage * clampSize, view.size());
        int to = Math.min(from + clampSize, view.size());
        List<NodeMemberRow> content = view.subList(from, to);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodeId", node.getId());
        result.put("nodeName", node.getName());
        result.put("nodeType", node.getType().name());
        result.put("scope", scope.name());
        result.put("content", content);
        result.put("page", clampPage);
        result.put("size", clampSize);
        result.put("totalElements", view.size());
        result.put("totalPages", (view.size() + clampSize - 1) / clampSize);
        result.put("memberCountDirect", countDirect);
        result.put("memberCountSousArbre", view.size());
        return result;
    }

    /** Compteurs pour la fiche nœud (additif de l'agrégat, mêmes règles que le roster). */
    public RosterCounters counters(UUID tenantId, UUID nodeId) {
        OrganizationNode node = requireNode(tenantId, nodeId);
        Map<UUID, OrganizationNode> nodesById = loadTenantNodes(tenantId);
        List<Attachment> rows = new ArrayList<>(
                collectAttachments(tenantId, node, descendantIds(node, nodesById), nodesById).values());
        Map<UUID, User> users = loadUsers(rows);
        long alive = rows.stream().filter(a -> {
            User u = users.get(a.userId);
            return u != null && !u.isDeleted();
        }).count();
        long direct = rows.stream().filter(a -> {
            User u = users.get(a.userId);
            return u != null && !u.isDeleted() && a.direct;
        }).count();
        return new RosterCounters(direct, alive);
    }

    // ==================== RATTACHEMENTS ====================

    /** Point d'attachement d'un membre à un nœud du sous-arbre consulté. */
    private static final class Attachment {
        final UUID userId;
        final UUID nodeId;
        final boolean direct;
        final String origine;
        final java.time.Instant since;
        String roleLabel;

        Attachment(UUID userId, UUID nodeId, boolean direct, String origine, java.time.Instant since) {
            this.userId = userId;
            this.nodeId = nodeId;
            this.direct = direct;
            this.origine = origine;
            this.since = since;
        }
    }

    /**
     * Les trois origines de rattachement (V231), bornées au sous-arbre. La
     * profondeur relative au nœud consulté départage deux attachements d'un
     * même membre : le plus profond (le plus proche du terrain) gagne.
     */
    private Map<UUID, Attachment> collectAttachments(UUID tenantId, OrganizationNode node,
                                                     Set<UUID> subtreeIds,
                                                     Map<UUID, OrganizationNode> nodesById) {
        Map<UUID, Attachment> best = new LinkedHashMap<>();
        Map<UUID, Integer> depth = depthByNode(nodesById);

        // 1. Adhésions à portée de nœud (le rattachement « appartenance »).
        for (TenantMembership tm : membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE)) {
            if (tm.getScopeId() == null || !subtreeIds.contains(tm.getScopeId())) continue;
            if (tm.getScopeType() == MembershipScopeType.TENANT
                    || tm.getScopeType() == MembershipScopeType.OWN) continue;
            keep(best, depth, new Attachment(tm.getUserId(), tm.getScopeId(),
                    tm.getScopeId().equals(node.getId()), "ADHESION_NOEUD", tm.getJoinedAt()));
        }

        // 2. Capacités V3 posées sur un nœud du sous-arbre (multi-branches).
        for (MemberRoleAssignment a : assignmentRepository.findByTenantIdAndStatus(tenantId,
                MemberRoleAssignment.AssignmentStatus.ACTIVE)) {
            if (a.getNodeId() == null || !subtreeIds.contains(a.getNodeId())) continue;
            Attachment att = new Attachment(a.getUserId(), a.getNodeId(),
                    a.getNodeId().equals(node.getId()), "ASSIGNATION_V3", a.getAssignedAt());
            att.roleLabel = roleLabel(tenantId, a);
            keep(best, depth, att);
        }

        // 3. Responsables de nœud — signal autoritatif (pasteur de campus…).
        for (OrganizationNode n : nodesById.values()) {
            if (n.getResponsibleId() == null || !subtreeIds.contains(n.getId())) continue;
            keep(best, depth, new Attachment(n.getResponsibleId(), n.getId(),
                    n.getId().equals(node.getId()), "RESPONSABLE_NOEUD", n.getUpdatedAt()));
        }
        return best;
    }

    /** Garde le rattachement le plus profond ; à profondeur égale, le plus ancien. */
    private void keep(Map<UUID, Attachment> best, Map<UUID, Integer> depth, Attachment cand) {
        if (cand.userId == null) return;
        Attachment prev = best.get(cand.userId);
        if (prev == null) {
            best.put(cand.userId, cand);
            return;
        }
        int dc = depth.getOrDefault(cand.nodeId, 0);
        int dp = depth.getOrDefault(prev.nodeId, 0);
        boolean better = dc > dp
                || (dc == dp && cand.since != null && (prev.since == null || cand.since.isBefore(prev.since)))
                || (dc == dp && cand.direct && !prev.direct);
        if (better) best.put(cand.userId, cand);
    }

    private Map<UUID, Integer> depthByNode(Map<UUID, OrganizationNode> nodesById) {
        Map<UUID, Integer> depth = new LinkedHashMap<>();
        for (OrganizationNode n : nodesById.values()) {
            int d = 0;
            OrganizationNode cur = n;
            Set<UUID> seen = new LinkedHashSet<>();
            while (cur != null && cur.getParentId() != null && seen.add(cur.getId())) {
                cur = nodesById.get(cur.getParentId());
                d++;
            }
            depth.put(n.getId(), d);
        }
        return depth;
    }

    // ==================== SOUS-ARBRE / CHARGEMENTS ====================

    private Map<UUID, OrganizationNode> loadTenantNodes(UUID tenantId) {
        Map<UUID, OrganizationNode> byId = new LinkedHashMap<>();
        for (OrganizationNode n : nodeRepository.findByTenantId(tenantId)) {
            byId.put(n.getId(), n);
        }
        return byId;
    }

    /** Sous-arbre dérivé en mémoire par parentId (pas de LIKE sur path : plus sûr, anti-cycle). */
    private Set<UUID> descendantIds(OrganizationNode node, Map<UUID, OrganizationNode> nodesById) {
        Set<UUID> out = new LinkedHashSet<>();
        out.add(node.getId());
        boolean progress = true;
        while (progress) {
            progress = false;
            for (OrganizationNode n : nodesById.values()) {
                if (n.getParentId() != null && out.contains(n.getParentId()) && !out.contains(n.getId())) {
                    out.add(n.getId());
                    progress = true;
                }
            }
        }
        return out;
    }

    private Map<UUID, User> loadUsers(List<Attachment> rows) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (Attachment a : rows) ids.add(a.userId);
        Map<UUID, User> byId = new LinkedHashMap<>();
        if (ids.isEmpty()) return byId;
        for (User u : userRepository.findAllById(ids)) {
            if (u != null && !u.isDeleted()) byId.put(u.getId(), u);
        }
        return byId;
    }

    private OrganizationNode requireNode(UUID tenantId, UUID nodeId) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));
        if (!node.getTenantId().equals(tenantId)) {
            // Nœud d'une autre église : invisible (404), pas 403 — jamais d'énumération.
            throw new EntityNotFoundException("OrganizationNode", nodeId);
        }
        return node;
    }

    // ==================== PROJECTION ====================

    private boolean matches(User u, String needle) {
        return contains(u.getFirstName(), needle) || contains(u.getLastName(), needle)
                || contains(fullName(u), needle) || contains(u.getEmail(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private static String fullName(User u) {
        return (u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName());
    }

    private NodeMemberRow toRow(Attachment a, User u, Map<UUID, OrganizationNode> nodesById,
                                OrganizationNode queried) {
        OrganizationNode attached = nodesById.get(a.nodeId);
        List<String> roles = new ArrayList<>();
        if (a.roleLabel != null) roles.add(a.roleLabel);
        return new NodeMemberRow(
                u.getId(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getPhotoUrl(),
                u.getStatut() == null ? null : u.getStatut().name(),
                a.nodeId,
                attached != null ? attached.getName() : null,
                attached != null ? attached.getType().name() : null,
                a.direct,
                a.origine,
                roles,
                chainFrom(queried, attached, nodesById),
                a.since);
    }

    /** Chaîne du nœud consulté jusqu'au point d'attachement (fil d'Ariane descendant). */
    private List<Map<String, Object>> chainFrom(OrganizationNode queried, OrganizationNode attached,
                                                Map<UUID, OrganizationNode> nodesById) {
        List<Map<String, Object>> chain = new ArrayList<>();
        if (attached == null || attached.getId().equals(queried.getId())) {
            return chain;
        }
        // Remontée de l'attachement vers le nœud consulté, puis inversion.
        List<OrganizationNode> up = new ArrayList<>();
        OrganizationNode cur = attached;
        Set<UUID> seen = new LinkedHashSet<>();
        while (cur != null && !cur.getId().equals(queried.getId()) && seen.add(cur.getId())) {
            up.add(cur);
            cur = cur.getParentId() == null ? null : nodesById.get(cur.getParentId());
        }
        if (cur == null) return List.of(); // chaîne coupée : on n'invente rien
        for (int i = up.size() - 1; i >= 0; i--) {
            OrganizationNode n = up.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("nom", n.getName());
            m.put("type", n.getType().name());
            m.put("responsibleId", n.getResponsibleId());
            chain.add(m);
        }
        return chain;
    }

    private String roleLabel(UUID tenantId, MemberRoleAssignment a) {
        try {
            return roleRepository.findById(a.getRoleId()).map(Role::getLabel).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
