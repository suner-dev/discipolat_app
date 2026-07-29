package com.discipolat.modules.audit.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AuditLog> findByUtilisateurIdOrderByCreatedAtDesc(UUID utilisateurId, Pageable pageable);
    Page<AuditLog> findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(String entiteType, UUID entiteId, Pageable pageable);
}
