package com.discipolat.modules.transfers.domain;

import com.discipolat.common.enums.TransferType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferWorkflowConfigRepository extends JpaRepository<TransferWorkflowConfig, UUID> {

    Optional<TransferWorkflowConfig> findByTransferType(TransferType transferType);

    List<TransferWorkflowConfig> findByActifTrueOrderByTransferTypeAsc();

    List<TransferWorkflowConfig> findAllByOrderByTransferTypeAsc();
}
