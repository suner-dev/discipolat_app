package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * LOT 2 §GR — repository des groupes de navigation.
 *
 * <p>La résolution est <b>explicite</b> (globaux + ceux du tenant) et non
 * portée par le filtre Hibernate {@code tenantFilter} : c'est ce qui rend le
 * comportement testable et interdit qu'un groupe d'une église apparaisse chez
 * une autre (H4).
 */
public interface NavigationGroupRepository extends JpaRepository<NavigationGroup, UUID> {

    /** Groupes globaux (livrés par défaut) + groupes propres à l'église. */
    List<NavigationGroup> findByTenantIdIsNullOrTenantId(UUID tenantId);

    List<NavigationGroup> findByTenantId(UUID tenantId);

    List<NavigationGroup> findByParentGroupId(UUID parentGroupId);

    /** Suppression en cascade des affectations d'un groupe. */
    void deleteByParentGroupId(UUID parentGroupId);
}