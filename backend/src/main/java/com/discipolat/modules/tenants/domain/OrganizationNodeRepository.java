package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Query("SELECT n FROM OrganizationNode n WHERE n.tenantId = :tenantId AND n.path <@ :parentPath")
    List<OrganizationNode> findDescendants(@Param("tenantId") UUID tenantId, @Param("parentPath") String parentPath);

    Optional<OrganizationNode> findRootByTenantId(UUID tenantId);

    long countByTenantIdAndType(UUID tenantId, OrganizationNodeType type);
}