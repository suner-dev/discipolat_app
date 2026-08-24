package com.discipolat.modules.directory.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DirectoryRepository extends JpaRepository<DirectoryEntry, UUID> {
    Page<DirectoryEntry> findByTenantIdAndPublicProfilTrueOrderByMembreId(UUID tenantId, Pageable pageable);
    List<DirectoryEntry> findByTenantIdAndPublicProfilTrue(UUID tenantId);
    List<DirectoryEntry> findByMembreId(UUID membreId);
}
