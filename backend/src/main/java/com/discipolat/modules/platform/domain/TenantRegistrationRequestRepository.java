package com.discipolat.modules.platform.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRegistrationRequestRepository extends JpaRepository<TenantRegistrationRequest, UUID> {
    Optional<TenantRegistrationRequest> findByEmail(String email);

    /**
     * Recherche INSENSIBLE A LA CASSE (constat B4 / migration V185 : l'email est
     * une identité globale unique). Utilisée par le suivi public de demande.
     */
    @Query("SELECT r FROM TenantRegistrationRequest r WHERE LOWER(r.email) = LOWER(:email)")
    Optional<TenantRegistrationRequest> findByEmailIgnoreCase(@Param("email") String email);

    Page<TenantRegistrationRequest> findByStatusOrderByCreatedAtDesc(TenantRegistrationStatus status, Pageable pageable);
}
