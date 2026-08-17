package com.discipolat.modules.transfers.domain;

import com.discipolat.common.enums.TransferStatus;
import com.discipolat.common.enums.TransferType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, UUID> {

    Page<TransferRequest> findByStatutOrderByCreatedAtDesc(TransferStatus statut, Pageable pageable);

    Page<TransferRequest> findByTypeOrderByCreatedAtDesc(TransferType type, Pageable pageable);

    Page<TransferRequest> findByStatutAndTypeOrderByCreatedAtDesc(TransferStatus statut, TransferType type, Pageable pageable);

    List<TransferRequest> findByDemandeurId(UUID demandeurId);

    List<TransferRequest> findByPersonneId(UUID personneId);

    long countByWorkflowConfigId(UUID workflowConfigId);

    /** Demandes toujours en attente de validation dont le délai de traitement est dépassé. */
    List<TransferRequest> findByStatutInAndDelaiLimiteBefore(Collection<TransferStatus> statuts,
                                                             LocalDateTime delaiLimite);

    /** Sources du Page Builder : transferts en attente et demandes récentes. */
    long countByStatut(TransferStatus statut);

    List<TransferRequest> findTop10ByOrderByCreatedAtDesc();

    long countByDemandeurIdAndStatut(UUID demandeurId, TransferStatus statut);

    List<TransferRequest> findTop10ByDemandeurIdOrderByCreatedAtDesc(UUID demandeurId);
}
