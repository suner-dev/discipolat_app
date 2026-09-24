package com.discipolat.modules.platform.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRegistrationRequestRepository extends JpaRepository<TenantRegistrationRequest, UUID> {
    Optional<TenantRegistrationRequest> findByEmail(String email);

    Page<TenantRegistrationRequest> findByStatusOrderByCreatedAtDesc(TenantRegistrationStatus status, Pageable pageable);
}
