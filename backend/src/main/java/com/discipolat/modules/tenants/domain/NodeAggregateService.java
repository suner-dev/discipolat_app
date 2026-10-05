package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §E / T-B15 — agrégats & drill-down.
 *
 * <p>Remonte par sous-arbre les compteurs d'un nœud (fidèles, églises/
 * campus, leaders, sermons, prières) et les persiste en <b>snapshot</b>.
 * La série de snapshots constitue la progression. Les compteurs sont
 * RECALCULÉS, jamais dérivés nominatifs : côté plateforme on n'expose que
 * des NOMBRES (D7, aucun PII).
 *
 * <p>Le recalcul à la demande est synchrone (drill-down) ; un job
 * planifié (T-B15, §9.5) rafraîchit périodiquement pour les grands
 * réseaux — hors du chemin critique de lecture.
 */
@Service
@Transactional
public class NodeAggregateService {

    /** Types de nœuds comptés comme « église / campus ». */
    private static final Set<OrganizationNodeType> CHURCH_TYPES = EnumSet.of(
            OrganizationNodeType.ROOT_CHURCH, OrganizationNodeType.CAMPUS,
            OrganizationNodeType.SUB_CHURCH, OrganizationNodeType.ASSEMBLY);

    private final OrganizationNodeRepository nodeRepository;
    private final TenantMembershipRepository membershipRepository;
    private final MemberRoleAssignmentRepository assignmentRepository;
    private final NodeAggregateSnapshotRepository snapshotRepository;

    public NodeAggregateService(OrganizationNodeRepository nodeRepository,
                                TenantMembershipRepository membershipRepository,
                                MemberRoleAssignmentRepository assignmentRepository,
                                NodeAggregateSnapshotRepository snapshotRepository) {
        this.nodeRepository = nodeRepository;
        this.membershipRepository = membershipRepository;
        this.assignmentRepository = assignmentRepository;
        this.snapshotRepository = snapshotRepository;
    }

    /** Le nœud + tous ses descendants (inclut le nœud lui-même). */
    private List<OrganizationNode> subtree(UUID tenantId, UUID nodeId) {
        OrganizationNode node = requireNode(tenantId, nodeId);
        List<OrganizationNode> descendants = nodeRepository.findDescendants(tenantId, node.getPath() + ".*");
        List<OrganizationNode> all = new java.util.ArrayList<>();
        all.add(node);
        all.addAll(descendants);
        return all;
    }

    /**
     * Recalcule et persiste un snapshot des agrégats du sous-arbre de
     * {@code nodeId}. Retourne le snapshot enregistré.
     */
    @Transactional
    public NodeAggregateSnapshot snapshot(UUID tenantId, UUID nodeId) {
        List<OrganizationNode> sub = subtree(tenantId, nodeId);
        Set<UUID> subIds = sub.stream().map(OrganizationNode::getId).collect(Collectors.toSet());

        long churchCount = sub.stream().filter(n -> CHURCH_TYPES.contains(n.getType())).count();

        // Fidèles : memberships ACTIVE dont la portée couvre le sous-arbre
        // (scopeId dans le sous-arbre, ou portée TENANT à la racine).
        boolean isRootScope = nodeId.equals(tenantId) || sub.size() == 1 && nodeRepository.findRootByTenantId(tenantId)
                .map(r -> r.getId().equals(nodeId)).orElse(false);
        List<TenantMembership> active = membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);
        long memberCount = active.stream().filter(m ->
                m.getScopeType() == MembershipScopeType.TENANT
                        ? (isRootScope || m.getScopeId() == null)
                        : (m.getScopeId() != null && subIds.contains(m.getScopeId()))
        ).map(TenantMembership::getUserId).distinct().count();

        // Leaders : porteurs d'une assignation ACTIVE couvrant le sous-arbre.
        long leaderCount = assignmentRepository.findByTenantIdAndStatus(tenantId,
                        MemberRoleAssignment.AssignmentStatus.ACTIVE).stream()
                .filter(a -> a.getNodeId() == null || subIds.contains(a.getNodeId()))
                .map(MemberRoleAssignment::getUserId).distinct().count();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("nodeCount", sub.size());
        metrics.put("computedAt", Instant.now().toString());

        NodeAggregateSnapshot snap = NodeAggregateSnapshot.builder()
                .tenantId(tenantId)
                .nodeId(nodeId)
                .snapshotAt(Instant.now())
                .memberCount(memberCount)
                .churchCount(churchCount)
                .leaderCount(leaderCount)
                .sermonCount(0L)          // branches module sermon quand exposé (T-B15 affimage)
                .prayerTopicCount(0L)     // branches module prière quand exposé
                .metricsJson(metrics)
                .build();
        return snapshotRepository.save(snap);
    }

    /** Dernier snapshot (recalcule si aucun n'existe encore). */
    @Transactional
    public NodeAggregateSnapshot latest(UUID tenantId, UUID nodeId) {
        return snapshotRepository.findFirstByTenantIdAndNodeIdOrderBySnapshotAtDesc(tenantId, nodeId)
                .orElseGet(() -> snapshot(tenantId, nodeId));
    }

    /** Série temporelle (progression), du plus ancien au plus récent. */
    @Transactional(readOnly = true)
    public List<NodeAggregateSnapshot> series(UUID tenantId, UUID nodeId) {
        requireNode(tenantId, nodeId);
        return snapshotRepository.findByTenantIdAndNodeIdOrderBySnapshotAtAsc(tenantId, nodeId);
    }

    /**
     * Dernier snapshot par nœud pour TOUT le tenant, en une seule lecture.
     * Utilisé par l'arbre §5.4 (compteurs inline) — évite le N+1 et surtout le
     * effet de bord d'écriture de {@link #latest} (qui recrée un snapshot absent).
     * Les snapshots sont remontés du plus ancien au plus récent : le dernier
     * écrasement par nœud donne donc le plus récent.
     */
    @Transactional(readOnly = true)
    public Map<UUID, NodeAggregateSnapshot> latestForTenant(UUID tenantId) {
        Map<UUID, NodeAggregateSnapshot> latest = new LinkedHashMap<>();
        for (NodeAggregateSnapshot s : snapshotRepository.findByTenantIdOrderBySnapshotAtAsc(tenantId)) {
            latest.put(s.getNodeId(), s);
        }
        return latest;
    }

    /** Vue drill-down : enfants directs + dernier snapshot de chacun. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> childrenWithAggregate(UUID tenantId, UUID nodeId) {
        requireNode(tenantId, nodeId);
        List<OrganizationNode> children = nodeRepository.findByParentId(nodeId);
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (OrganizationNode child : children) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("nodeId", child.getId());
            row.put("name", child.getName());
            row.put("type", child.getType().name());
            row.put("levelId", child.getLevelId());
            row.put("responsibleId", child.getResponsibleId());
            snapshotRepository.findFirstByTenantIdAndNodeIdOrderBySnapshotAtDesc(tenantId, child.getId())
                    .ifPresent(s -> {
                        row.put("memberCount", s.getMemberCount());
                        row.put("churchCount", s.getChurchCount());
                        row.put("leaderCount", s.getLeaderCount());
                    });
            out.add(row);
        }
        return out;
    }

    /** Projection lisible d'un snapshot (sans PII). */
    public Map<String, Object> toView(NodeAggregateSnapshot s, String levelName, String responsibleName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nodeId", s.getNodeId());
        m.put("levelName", levelName);
        m.put("responsibleName", responsibleName);
        m.put("snapshotAt", s.getSnapshotAt());
        m.put("memberCount", s.getMemberCount());
        m.put("churchCount", s.getChurchCount());
        m.put("leaderCount", s.getLeaderCount());
        m.put("sermonCount", s.getSermonCount());
        m.put("prayerTopicCount", s.getPrayerTopicCount());
        m.put("metrics", s.getMetricsJson());
        return m;
    }

    private OrganizationNode requireNode(UUID tenantId, UUID nodeId) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));
        if (!node.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("OrganizationNode", nodeId);
        }
        return node;
    }
}
