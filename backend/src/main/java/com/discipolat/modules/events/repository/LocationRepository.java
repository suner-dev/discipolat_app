package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    List<Location> findByTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId);

    List<Location> findByParentLocationId(UUID parentId);

    Optional<Location> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}