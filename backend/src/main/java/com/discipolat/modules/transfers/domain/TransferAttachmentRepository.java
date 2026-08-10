package com.discipolat.modules.transfers.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransferAttachmentRepository extends JpaRepository<TransferAttachment, UUID> {

    List<TransferAttachment> findByTransferRequestIdOrderByCreatedAtAsc(UUID transferRequestId);

    void deleteByTransferRequestId(UUID transferRequestId);
}
