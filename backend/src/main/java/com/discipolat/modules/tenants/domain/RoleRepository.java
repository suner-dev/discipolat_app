package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends TenantAwareRepository<Role, UUID> {

    Optional<Role> findByTenantIdAndKey(UUID tenantId, String key);

    List<Role> findByTenantId(UUID tenantId);

    List<Role> findByTenantIdIsNull(); // System roles

    List<Role> findByTenantIdAndSystem(UUID tenantId, boolean system);

    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.key = :permissionKey AND (r.tenantId = :tenantId OR r.tenantId IS NULL)")
    List<Role> findByPermissionAndTenant(@Param("permissionKey") String permissionKey, @Param("tenantId") UUID tenantId);
}