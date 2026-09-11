package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends TenantAwareRepository<Permission, UUID> {

    Optional<Permission> findByKey(String key);

    List<Permission> findByTenantId(UUID tenantId);

    List<Permission> findByTenantIdIsNull(); // System permissions

    List<Permission> findByCategory(String category);

    List<Permission> findByScope(PermissionScope scope);

    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId")
    List<Permission> findByRoleId(@Param("roleId") UUID roleId);
}