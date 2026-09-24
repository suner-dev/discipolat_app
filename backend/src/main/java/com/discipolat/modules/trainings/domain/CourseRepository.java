package com.discipolat.modules.trainings.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByActifTrueOrderByTitreAsc();
    List<Course> findByCategorieOrderByTitreAsc(String categorie);

    @Query(value = "SELECT COUNT(*) FROM courses WHERE tenant_id = :tenantId", nativeQuery = true)
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
