package com.discipolat.modules.churchComparison.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChurchComparisonRepository extends JpaRepository<ChurchComparison, UUID> {
    List<ChurchComparison> findByTenantIdOrderByEffectifDesc(UUID tenantId);
    List<ChurchComparison> findByCategorieOrderByEffectifDesc(String categorie);
    List<ChurchComparison> findByPaysOrderByEffectifDesc(String pays);
}
