package com.discipolat.modules.transfers.domain;

import com.discipolat.common.enums.DecisionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferDecisionRepository extends JpaRepository<TransferDecision, UUID> {

    List<TransferDecision> findByTransferRequestIdOrderByCreatedAtDesc(UUID transferRequestId);

    Optional<TransferDecision> findFirstByTransferRequestIdAndValidateurIdOrderByCreatedAtDesc(
            UUID transferRequestId, UUID validateurId);

    long countByTransferRequestIdAndDecision(UUID transferRequestId, DecisionType decision);

    long countByTransferRequestIdAndValidateurIdAndDecision(
            UUID transferRequestId, UUID validateurId, DecisionType decision);
}
