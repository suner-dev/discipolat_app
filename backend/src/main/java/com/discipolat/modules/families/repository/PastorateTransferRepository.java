package com.discipolat.modules.families.repository;

import com.discipolat.modules.families.domain.PastorateTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PastorateTransferRepository extends JpaRepository<PastorateTransfer, UUID> {

    List<PastorateTransfer> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status);

    List<PastorateTransfer> findByPastorIdAndStatus(UUID pastorId, String status);

    Optional<PastorateTransfer> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<PastorateTransfer> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, org.springframework.data.domain.Pageable pageable);
}