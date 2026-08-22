package com.discipolat.modules.testimonials.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TestimonyRepository extends JpaRepository<Testimony, UUID> {
    Page<Testimony> findByTenantIdAndStatutOrderByCreatedAtDesc(UUID tenantId, Testimony.Statut statut, Pageable pageable);
    Page<Testimony> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<Testimony> findByTenantIdAndCategorieOrderByCreatedAtDesc(UUID tenantId, Testimony.Categorie categorie, Pageable pageable);
}
