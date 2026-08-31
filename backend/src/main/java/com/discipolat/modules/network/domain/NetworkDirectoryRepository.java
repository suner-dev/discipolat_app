package com.discipolat.modules.network.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetworkDirectoryRepository extends JpaRepository<NetworkDirectory, UUID> {

    /** Toutes les églises listées volontairement. */
    List<NetworkDirectory> findByIsListedTrueOrderByChurchNameAsc();

    /** Recherche par pays. */
    List<NetworkDirectory> findByIsListedTrueAndCountryOrderByChurchNameAsc(String country);

    /** Recherche par nom. */
    List<NetworkDirectory> findByIsListedTrueAndChurchNameContainingIgnoreCaseOrderByChurchNameAsc(String name);

    /** L'entrée de l'église courante. */
    Optional<NetworkDirectory> findByTenantId(UUID tenantId);

    long countByIsListedTrue();
}
