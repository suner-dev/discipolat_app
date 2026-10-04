package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantJoinCodeRepository extends JpaRepository<TenantJoinCode, UUID> {

    Optional<TenantJoinCode> findByCodeAndIsActiveTrue(String code);

    boolean existsByCodeAndIsActiveTrue(String code);

    List<TenantJoinCode> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<TenantJoinCode> findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId);

    /**
     * Codes principaux (racine) ACTIFS d'un tenant.
     *
     * <p>SPF ONBOARDING_DENOMINATION_V2 §7.1 / T-B4 (faille F16) : la
     * signature d'origine renvoyait un {@code Optional}. Or rien n'empêchait
     * d'avoir <b>deux</b> codes principaux actifs — {@code create} accepte un
     * {@code orgNodeId} absent et ne désactivait rien — et le moindre appelant
     * levait alors {@code IncorrectResultSizeDataAccessException}, c'est-à-dire
     * un <b>500 sur la page publique</b> {@code /join?slug=}. La liste est le
     * seul type sûr : l'appelant tranche (le plus récent) au lieu de subir
     * une exception.
     */
    List<TenantJoinCode> findByTenantIdAndOrgNodeIdIsNullAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId);

    Optional<TenantJoinCode> findByTenantIdAndOrgNodeIdAndIsActiveTrue(UUID tenantId, UUID orgNodeId);
}
