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

    /**
     * Nœuds d'un tenant, triés par nom — alimente le sélecteur de
     * sous-église de la console des codes d'entrée (F14).
     */
    List<OrganizationNode> findByTenantIdOrderByNameAsc(UUID tenantId);

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

    /**
     * Racine d'un tenant.
     *
     * <p><b>Requête explicite, obligatoire.</b> Le nom
     * {@code findRootByTenantId} était interprété par Spring Data comme une
     * requête par SUJET : le prédicat réel retenu n'était que
     * {@code tenantId = ?}. La méthode renvoyant un {@code Optional}, elle levait
     * alors {@code IncorrectResultSizeDataAccessException: 2 results} dès que le
     * tenant possédait sa racine <b>et</b> un autre nœud de premier niveau — ce
     * qui est le cas de tout tenant provisionné (église racine + département).
     * C'est ce qui faisait échouer l'étape CHURCH_IDENTITY du wizard sur une base
     * réelle ; invisible en test unitaire, où un seul nœud est simulé.
     *
     * <p>Une église racine est identifiée par son TYPE, pas par l'absence de
     * parent : un département peut lui aussi être un nœud de premier niveau.
     */
    @Query("SELECT n FROM OrganizationNode n WHERE n.tenantId = :tenantId AND n.type = :type ORDER BY n.createdAt ASC")
    List<OrganizationNode> findRootCandidates(UUID tenantId, @Param("type") OrganizationNodeType type);

    /** Première racine du tenant, ou vide. Utilise toujours par la lecture. */
    default Optional<OrganizationNode> findRootByTenantId(UUID tenantId) {
        return findRootCandidates(tenantId, OrganizationNodeType.ROOT_CHURCH).stream().findFirst();
    }

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