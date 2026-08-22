package com.discipolat.modules.compliance.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsentLogRepository extends JpaRepository<ConsentLog, UUID> {
    List<ConsentLog> findByUtilisateurId(UUID utilisateurId);
    List<ConsentLog> findByTenantId(UUID tenantId);
}
