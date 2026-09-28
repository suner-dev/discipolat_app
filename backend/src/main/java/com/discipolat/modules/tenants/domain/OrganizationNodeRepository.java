package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationNodeRepository extends TenantAwareRepository<OrganizationNode, UUID> {

    Optional<OrganizationNode> findByTenantIdAndCode(UUID tenantId, String code);

    List<OrganizationNode> findByTenantId(UUID tenantId);

    List<OrganizationNode> findByTenantIdAndType(UUID tenantId, OrganizationNodeType type);

    List<OrganizationNode> findByParentId(UUID parentId);

    List<OrganizationNode> findByTenantIdAndParentId(UUID tenantId, UUID parentId);

    List<OrganizationNode> findByTenantIdAndStatus(UUID tenantId, OrganizationNodeStatus status);

    List<OrganizationNode> findByResponsibleId(UUID responsibleId);

    @Query("SELECT n FROM OrganizationNode n WHERE n.path LIKE :pathPattern")
    List<OrganizationNode> findByPathPrefix(@Param("pathPattern") String pathPattern);

    @Query("SELECT n FROM OrganizationNode n WHERE n.tenantId = :tenantId AND n.path LIKE CONCAT(:parentPath, '%')")
    List<OrganizationNode> findDescendants(@Param("tenantId") UUID tenantId, @Param("parentPath") String parentPath);

    @Query("SELECT n FROM OrganizationNode n WHERE n.tenantId = :tenantId AND n.path LIKE CONCAT((SELECT p.path FROM OrganizationNode p WHERE p.id = :parentId), '%') AND n.id != :parentId")
    List<OrganizationNode> findDescendantsByNodeId(@Param("tenantId") UUID tenantId, @Param("parentId") UUID parentId);

    Optional<OrganizationNode> findRootByTenantId(UUID tenantId);

    @Query(value = "SELECT COUNT(*) FROM organization_nodes WHERE tenant_id = :tenantId AND type = :type", nativeQuery = true)
    long countByTenantIdAndType(@Param("tenantId") UUID tenantId, @Param("type") OrganizationNodeType type);

    @Query(value = "SELECT COUNT(*) FROM organization_nodes WHERE tenant_id = :tenantId", nativeQuery = true)
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = "SELECT COUNT(*) FROM organization_nodes WHERE type = :type", nativeQuery = true)
    long countByType(@Param("type") OrganizationNodeType type);

    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM OrganizationNode n WHERE n.id = :nodeId AND EXISTS (SELECT a FROM OrganizationNode a WHERE a.id = :ancestorId AND n.path LIKE CONCAT(a.path, '%'))")
    boolean isDescendantOf(@Param("nodeId") UUID nodeId, @Param("ancestorId") UUID ancestorId);

    @Query(value = """
        SELECT
            COUNT(*) as total,
            COUNT(*) FILTER (WHERE type = 'ROOT_CHURCH') as churches,
            COUNT(*) FILTER (WHERE type = 'CAMPUS') as campuses,
            COUNT(*) FILTER (WHERE type = 'SUB_CHURCH') as sub_churches,
            COUNT(*) FILTER (WHERE type = 'DEPARTMENT') as departments,
            COUNT(*) FILTER (WHERE type = 'GROUP') as groups,
            COUNT(*) FILTER (WHERE type = 'FAMILY') as families
        FROM organization_nodes
        """, nativeQuery = true)
    Map<String, Object> getDashboardStats();

    @Query(value = """
        SELECT
            onr.tenant_id,
            COUNT(*) as count
        FROM organization_nodes onr
        GROUP BY onr.tenant_id
        ORDER BY count DESC
        LIMIT 100
        """, nativeQuery = true)
    List<Map<String, Object>> getTopTenantsByNodeCount();

    @Query(value = """
        SELECT
            type,
            COUNT(*) as count
        FROM organization_nodes
        GROUP BY type
        ORDER BY count DESC
        """, nativeQuery = true)
    List<Map<String, Object>> getNodesByType();
}