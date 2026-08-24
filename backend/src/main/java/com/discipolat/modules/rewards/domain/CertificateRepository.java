package com.discipolat.modules.rewards.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findByTenantIdOrderByIssuedAtDesc(UUID tenantId);
    List<Certificate> findByUserIdOrderByIssuedAtDesc(UUID userId);
}
