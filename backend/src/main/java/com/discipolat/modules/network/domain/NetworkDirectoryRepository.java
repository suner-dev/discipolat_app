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

    /**
     * LOT 1 §GLISE-D'ABORD (T1.1) — correspondance EXACTE (insensible à la casse)
     * sur une église en opt-in annuaire, pour {@code /public/churches/exists}.
     * La liste des retours (et non un booléen) permet plusieurs églises homonymes
     * listées ; le contrôleur répond {@code found:false} à la fois pour « aucune
     * correspondance » et « église existante mais non listée » (anti-énumération,
     * D2/R3) — un {@code isListed=false} ne remonte donc jamais ici.
     */
    List<NetworkDirectory> findTop10ByIsListedTrueAndChurchNameIgnoreCaseOrderByChurchNameAsc(String name);

    /** L'entrée de l'église courante. */
    Optional<NetworkDirectory> findByTenantId(UUID tenantId);

    long countByIsListedTrue();
}
