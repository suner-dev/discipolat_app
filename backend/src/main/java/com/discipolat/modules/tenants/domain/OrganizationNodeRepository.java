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

    @Query("SELECT n FROM OrganizationNode n WHERE n.tenantId = :tenantId AND n.path LIKE CONCAT(:parentPath, '%')")
    List<OrganizationNode> findDescendants(@Param("tenantId") UUID tenantId, @Param("parentPath") String parentPath);

    @Query("SELECT n FROM OrganizationNode n WHERE n.tenantId = :tenantId AND n.path LIKE CONCAT((SELECT p.path FROM OrganizationNode p WHERE p.id = :parentId), '%') AND n.id != :parentId")
    List<OrganizationNode> findDescendantsByNodeId(@Param("tenantId") UUID tenantId, @Param("parentId") UUID parentId);

    Optional<OrganizationNode> findRootByTenantId(UUID tenantId);

    /**
     * Constat H7 : la colonne {@code type} est un {@code varchar} et l'entité la
     * mappe en {@code @Enumerated(STRING)}, mais dans une requête NATIVE
     * l'annotation ne s'applique pas : Hibernate liait l'ORDINAL de l'énumère et
     * PostgreSQL répondait
     * {@code operator does not exist: character varying = smallint}.
     *
     * <p>Conséquence mesurée : <b>tous</b> les compteurs d'organisations étaient
     * cassés — quotas églises/départements/campus (A8), tableau de bord Super
     * Admin, tableau de bord admin tenant, statistiques de hiérarchie. C'est-à-dire
     * précisément le travail A8, invisible à la suite de tests qui mocke les
     * repositories.
     *
     * <p>La requête doit donc comparer au NOM de l'énumère, pas à sa position.
     * Les surcharges ci-dessous gardent l'API publique en enum pour les ~25
     * appelants existants.
     */
    @Query(value = "SELECT COUNT(*) FROM organization_nodes WHERE tenant_id = :tenantId AND type = :type", nativeQuery = true)
    long countByTenantIdAndTypeName(@Param("tenantId") UUID tenantId, @Param("type") String type);

    default long countByTenantIdAndType(UUID tenantId, OrganizationNodeType type) {
        return countByTenantIdAndTypeName(tenantId, type.name());
    }

    @Query(value = "SELECT COUNT(*) FROM organization_nodes WHERE tenant_id = :tenantId", nativeQuery = true)
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = "SELECT COUNT(*) FROM organization_nodes WHERE type = :type", nativeQuery = true)
    long countByTypeName(@Param("type") String type);

    default long countByType(OrganizationNodeType type) {
        return countByTypeName(type.name());
    }

    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM OrganizationNode n WHERE n.id = :nodeId AND EXISTS (SELECT a FROM OrganizationNode a WHERE a.id = :ancestorId AND n.path LIKE CONCAT(a.path, '%'))")
    boolean isDescendantOf(@Param("nodeId") UUID nodeId, @Param("ancestorId") UUID ancestorId);
}