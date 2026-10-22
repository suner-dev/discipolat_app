package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
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
 *   <li>{@code ADHESION_TENANT} — adhésion ACTIVE de portée {@code TENANT}
 *       (le membre de l'église sans affectation de secteur) : visible
 *       <b>seulement</b> sous la racine, jamais sous un nœud intermédiaire ;</li>
 *   <li>{@code RESPONSABLE_NOEUD} — le membre est {@code responsibleId} d'un
 *       nœud du sous-arbre (signal autoritatif du pasteur de campus).</li>
 * </ol>
 * Les adhésions de portée {@code TENANT}/{@code OWN} ne rattachent à AUCUN
 * nœud particulier : les incrémenter partout était précisément le défaut B2
 * de {@code getMembersWithAccess} (le roster d'un campus noyait sous les
 * « membres du tenant »). Ici, une portée TENANT ne rend visible le membre
 * que sous la <b>racine</b>, jamais sous un nœud intermédiaire.
 *
 * <h2>Compteurs</h2>
 * {@code memberCountDirect} / {@code memberCountSousArbre} sont structurels :
 * ils portent sur l'ensemble dédupliqué des membres vivants rattachés, que le
 * filtre {@code q} soit présent ou non — ils décrivent l'entité. {@code totalElements}
 * et {@code totalPages} suivent le filtre et pilotent la pagination.
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
            /** ASSIGNATION_V3 | ADHESION_NOEUD | ADHESION_TENANT | RESPONSABLE_NOEUD. */
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
        RosterData data = buildRoster(tenantId, node);

        // Le DIRECT énumère les rattachements ICI ; le SUBTREE énumère l'ensemble
        // dédupliqué du sous-arbre. Les deux cartes sont déjà calculées : les
        // compteurs affichés ne changent pas selon la portée consultée.
        Collection<Attachment> selected = (scope == RosterScope.DIRECT ? data.direct : data.subtree)
                .values();

        String needle = q == null ? null : q.trim().toLowerCase();
        // Compteurs STRUCTURELS : ils décrivent l'entité consultée, pas la
        // recherche en cours. Les laisser dépendre de `q` ferait afficher « Sous-arbre :
        // 1 » sur une entité de 40 membres dès que quelqu'un tape trois lettres, et
        // contredirait l'autre échelle — deux chiffres pour un même périmètre.
        // Seuls `totalElements`/`totalPages` suivent le filtre.
        long countDirect = 0;
        long countSousArbre = 0;
        for (Attachment a : data.direct.values()) {
            User u = data.users.get(a.userId);
            if (u != null && !u.isDeleted()) countDirect++;
        }
        for (Attachment a : data.subtree.values()) {
            User u = data.users.get(a.userId);
            if (u != null && !u.isDeleted()) countSousArbre++;
        }

        List<NodeMemberRow> view = new ArrayList<>();
        for (Attachment a : selected) {
            User u = data.users.get(a.userId);
            if (u == null || u.isDeleted()) continue;
            if (needle != null && !matches(u, needle)) continue;
            view.add(toRow(a, u, data.nodesById, node));
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
        result.put("memberCountSousArbre", countSousArbre);
        return result;
    }

    /**
     * Compteurs pour la fiche nœud (additif de l'agrégat).
     *
     * <p>Mandatement : ils sortent du MÊME calcul que le roster ({@link
     * #buildRoster}) — un compteur et une liste qui se contredisent sur la même
     * échelle sont exactement le défaut B2 sous une autre forme.
     */
    public RosterCounters counters(UUID tenantId, UUID nodeId) {
        OrganizationNode node = requireNode(tenantId, nodeId);
        RosterData data = buildRoster(tenantId, node);
        long direct = data.direct.values().stream()
                .map(a -> data.users.get(a.userId))
                .filter(u -> u != null && !u.isDeleted()).count();
        long sousArbre = data.subtree.values().stream()
                .map(a -> data.users.get(a.userId))
                .filter(u -> u != null && !u.isDeleted()).count();
        return new RosterCounters(direct, sousArbre);
    }

    // ==================== SOURCES / CALCUL ====================

    /**
     * Les trois ensembles dédupliqués d'un nœud, construits en UNE lecture par
     * source : rattachements ICI ({@code direct}), rattachements du SOUS-ARBRE
     * ({@code subtree}) et les comptes correspondants ({@code users}).
     *
     * <p>{@code direct} et {@code subtree} ne sont PAS reliés par l'arbitrage de
     * déduplication : un membre qui a une adhésion ICI et une capacité dans une
     * sous-cellule est bien « direct » (il rattache à ce nœud) ET n'apparaît
     * qu'une fois dans le sous-arbre (au point le plus profond). C'est la même
     * définition de « direct » que le compteur existant de {@link
     * NodeAggregateService} — les deux écrans ne peuvent pas diverger.
     */
    private static final class RosterData {
        final Map<UUID, OrganizationNode> nodesById;
        final Map<UUID, Attachment> direct;
        final Map<UUID, Attachment> subtree;
        final Map<UUID, User> users;

        RosterData(Map<UUID, OrganizationNode> nodesById, Map<UUID, Attachment> direct,
                   Map<UUID, Attachment> subtree, Map<UUID, User> users) {
            this.nodesById = nodesById;
            this.direct = direct;
            this.subtree = subtree;
            this.users = users;
        }
    }

    /** Sources brutes, lues une seule fois pour les deux portées. */
    private static final class RosterSources {
        final List<TenantMembership> memberships;
        final List<MemberRoleAssignment> assignments;
        final Map<UUID, String> roleLabels;

        RosterSources(List<TenantMembership> memberships, List<MemberRoleAssignment> assignments,
                      Map<UUID, String> roleLabels) {
            this.memberships = memberships;
            this.assignments = assignments;
            this.roleLabels = roleLabels;
        }
    }

    private RosterData buildRoster(UUID tenantId, OrganizationNode node) {
        Map<UUID, OrganizationNode> nodesById = loadTenantNodes(tenantId);
        RosterSources sources = loadSources(tenantId);
        // Les DEUX portées sont calculées systématiquement : la liste servie dépend
        // de la portée demandée, les compteurs décrivent l'entité quelle que soit la
        // portée consultée. Coût : une passe en mémoire de plus, aucune requête de
        // plus — les sources sont lues une seule fois.
        Map<UUID, Attachment> direct = collectAttachments(node, Set.of(node.getId()), nodesById, sources);
        Map<UUID, Attachment> subtree = collectAttachments(node,
                descendantIds(node, nodesById), nodesById, sources);
        // Tout rattachement « ici » est aussi un rattachement du sous-arbre (le même
        // candidat est présenté aux deux passes) : l'ensemble des comptes à charger
        // est donc couvert par `subtree`, en une seule requête.
        return new RosterData(nodesById, direct, subtree, loadUsers(subtree.values()));
    }

    /**
     * Adhésions, capacités et libellés de rôles du tenant : TROIS lectures, une
     * seule fois chaque — quel que soit le nombre de membres, de capacités ou de
     * nœuds. Les libellés sont préchargés : les relire par affectation
     * ({@code findById} dans la boucle) reproduirait le N+1 que la mission vient
     * corriger.
     */
    private RosterSources loadSources(UUID tenantId) {
        List<TenantMembership> memberships =
                membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);
        List<MemberRoleAssignment> assignments = assignmentRepository.findByTenantIdAndStatus(tenantId,
                MemberRoleAssignment.AssignmentStatus.ACTIVE);
        Set<UUID> roleIds = new LinkedHashSet<>();
        for (MemberRoleAssignment a : assignments) {
            if (a.getRoleId() != null) roleIds.add(a.getRoleId());
        }
        Map<UUID, String> labels = new LinkedHashMap<>();
        if (!roleIds.isEmpty()) {
            for (Role r : roleRepository.findAllById(roleIds)) {
                if (r != null) labels.put(r.getId(), r.getLabel());
            }
        }
        return new RosterSources(memberships, assignments, labels);
    }

    // ==================== RATTACHEMENTS ====================

    /** Point d'attachement d'un membre au périmètre consulté. */
    private static final class Attachment {
        final UUID userId;
        final UUID nodeId;
        final boolean direct;
        final String origine;
        final java.time.Instant since;
        /** Tous les rôles portés dans le périmètre, même si l'attachement retenu est ailleurs. */
        final List<String> roles = new ArrayList<>();

        Attachment(UUID userId, UUID nodeId, boolean direct, String origine, java.time.Instant since) {
            this.userId = userId;
            this.nodeId = nodeId;
            this.direct = direct;
            this.origine = origine;
            this.since = since;
        }
    }

    /**
     * Les trois origines de rattachement (V231), bornées au périmètre passé.
     * La profondeur relative au nœud consulté départage deux attachements d'un
     * même membre : le plus profond (le plus proche du terrain) gagne.
     */
    private Map<UUID, Attachment> collectAttachments(OrganizationNode node,
                                                     Set<UUID> scopeIds,
                                                     Map<UUID, OrganizationNode> nodesById,
                                                     RosterSources sources) {
        Map<UUID, Attachment> best = new LinkedHashMap<>();
        Map<UUID, Integer> depth = depthByNode(nodesById);
        // Rôles relevés HORS arbitrage : l'arbitrage de déduplication REMPLACE
        // l'attachement retenu, donc étiqueter l'attachement au fil de l'eau
        // perdrait le rôle du niveau précédent (membre « Diacre » ici et
        // « Responsable » dans une sous-cellule n'en afficherait qu'un).
        Map<UUID, Set<String>> rolesByUser = new LinkedHashMap<>();

        // 1. Adhésions à portée de nœud (le rattachement « appartenance »).
        //    Une adhésion de portée TENANT ne rattache à AUCUNE sous-structure —
        //    l'inonder partout était précisément le défaut B2 — mais elle rattache
        //    bien à l'ÉGLISE ELLE-MÊME : elle compte donc uniquement sous la
        //    racine. Même règle que le compteur existant de
        //    {@link NodeAggregateService} (alignement BE-3 : le roster et les
        //    compteurs ne peuvent pas se contredire sur la même échelle).
        boolean isRoot = node.getParentId() == null;
        for (TenantMembership tm : sources.memberships) {
            if (tm.getScopeType() == MembershipScopeType.TENANT) {
                if (isRoot && tm.getUserId() != null) {
                    keep(best, depth, new Attachment(tm.getUserId(), node.getId(), true,
                            "ADHESION_TENANT", tm.getJoinedAt()));
                }
                continue;
            }
            if (tm.getScopeType() == MembershipScopeType.OWN) continue;
            if (tm.getScopeId() == null || !scopeIds.contains(tm.getScopeId())) continue;
            keep(best, depth, new Attachment(tm.getUserId(), tm.getScopeId(),
                    tm.getScopeId().equals(node.getId()), "ADHESION_NOEUD", tm.getJoinedAt()));
        }

        // 2. Capacités V3 posées sur un nœud du périmètre (multi-branches). Le
        //    point d'attachement retenu est le plus profond, MAIS tous les rôles
        //    du périmètre sont restitués : un membre « Responsable » dans une
        //    sous-cellule et « Diacre » ici n'est pas un membre à un seul rôle.
        for (MemberRoleAssignment a : sources.assignments) {
            if (a.getUserId() == null || a.getNodeId() == null || !scopeIds.contains(a.getNodeId())) {
                continue;
            }
            keep(best, depth, new Attachment(a.getUserId(), a.getNodeId(),
                    a.getNodeId().equals(node.getId()), "ASSIGNATION_V3", a.getAssignedAt()));
            String label = sources.roleLabels.get(a.getRoleId());
            if (label != null) {
                rolesByUser.computeIfAbsent(a.getUserId(), k -> new LinkedHashSet<>()).add(label);
            }
        }

        // 3. Responsables de nœud — signal autoritatif (pasteur de campus…).
        for (OrganizationNode n : nodesById.values()) {
            if (n.getResponsibleId() == null || !scopeIds.contains(n.getId())) continue;
            keep(best, depth, new Attachment(n.getResponsibleId(), n.getId(),
                    n.getId().equals(node.getId()), "RESPONSABLE_NOEUD", n.getUpdatedAt()));
        }
        // Les libellés suivent le membre, une fois l'arbitrage terminé : la ligne
        // rendue porte TOUS les rôles exercés dans le périmètre consulté, où qu'ait
        // été retenu le point d'attachement. Un `responsibleId` seul n'est pas une
        // capacité : il rattache (origine RESPONSABLE_NOEUD) mais n'invente aucun
        // libellé de rôle.
        for (Map.Entry<UUID, Set<String>> e : rolesByUser.entrySet()) {
            Attachment att = best.get(e.getKey());
            if (att != null) att.roles.addAll(e.getValue());
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

    private Map<UUID, User> loadUsers(Collection<Attachment> rows) {
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
                List.copyOf(a.roles),
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
}
