package com.discipolat.modules.transfers.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransferHistoryRepository extends JpaRepository<TransferHistory, UUID> {

    List<TransferHistory> findByTransferRequestIdOrderByCreatedAtAsc(UUID transferRequestId);

    List<TransferHistory> findByTransferRequestIdOrderByCreatedAtDesc(UUID transferRequestId);
}
