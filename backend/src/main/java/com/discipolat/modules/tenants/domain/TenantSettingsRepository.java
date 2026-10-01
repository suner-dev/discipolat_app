package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantSettingsRepository extends JpaRepository<TenantSettings, UUID> {

    Optional<TenantSettings> findByTenantId(UUID tenantId);

    @Query("SELECT ts FROM TenantSettings ts WHERE ts.tenant.id = :tenantId")
    Optional<TenantSettings> findByTenant_Id(UUID tenantId);

    boolean existsByTenantId(UUID tenantId);

    /**
     * §G6.9 — annuaire public « Églises sur Discipolat ». Retourne uniquement les
     * réglages des églises ayant explicitement opté (toggle {@code public_directory_enabled}
     * = true) ET dont le tenant est ACTIVE. Cross-tenant par nature (listing public) :
     * {@code TenantSettings}/{@code Tenant} ne portent pas le {@code @Filter} tenant.
     * Le contrôleur expose ensuite un champ minimal non sensible.
     */
    @Query("""
            SELECT ts FROM TenantSettings ts
            JOIN FETCH ts.tenant t
            WHERE ts.publicDirectoryEnabled = true
              AND t.status = com.discipolat.modules.tenants.domain.TenantStatus.ACTIVE
            ORDER BY t.name ASC
            """)
    List<TenantSettings> findPublicDirectoryEntries();

    /**
     * PORT Develop1 (§G6.9) — lecture en bloc des réglages de vitrine pour une
     * collection de tenants. L'annuaire public a besoin des champs marketing
     * (slogan, logo, couverture) sans N+1 : une seule requête pour la page.
     */
    @Query("SELECT ts FROM TenantSettings ts WHERE ts.tenant.id IN :tenantIds")
    List<TenantSettings> findAllForTenants(@Param("tenantIds") Collection<UUID> tenantIds);
}