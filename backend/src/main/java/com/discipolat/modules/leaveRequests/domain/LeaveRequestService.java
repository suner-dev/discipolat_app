package com.discipolat.modules.leaveRequests.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class LeaveRequestService {

    private final LeaveRequestRepository repository;

    public LeaveRequestService(LeaveRequestRepository repository) {
        this.repository = repository;
    }

    public Page<LeaveRequest> list(Pageable pageable, String statut) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (statut != null) {
            return repository.findByTenantIdAndStatut(tenantId, LeaveRequest.Statut.valueOf(statut), pageable);
        }
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    public LeaveRequest getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("LeaveRequest", id));
    }

    public LeaveRequest create(String type, LocalDate dateDebut, LocalDate dateFin, String motif, UUID userId) {
        LeaveRequest req = new LeaveRequest();
        req.setTenantId(TenantContext.getCurrentTenantId());
        req.setType(LeaveRequest.Type.valueOf(type));
        req.setDateDebut(dateDebut);
        req.setDateFin(dateFin);
        req.setMotif(motif);
        req.setDemandeurId(userId);
        return repository.save(req);
    }

    public LeaveRequest approve(UUID id, UUID approverId) {
        LeaveRequest req = getById(id);
        req.setStatut(LeaveRequest.Statut.APPROUVE);
        req.setValideParId(approverId);
        req.setUpdatedAt(LocalDateTime.now());
        return repository.save(req);
    }

    public LeaveRequest reject(UUID id, UUID approverId, String commentaire) {
        LeaveRequest req = getById(id);
        req.setStatut(LeaveRequest.Statut.REFUSE);
        req.setValideParId(approverId);
        req.setCommentaire(commentaire);
        req.setUpdatedAt(LocalDateTime.now());
        return repository.save(req);
    }

    public LeaveRequest cancel(UUID id, UUID userId) {
        LeaveRequest req = getById(id);
        if (!req.getDemandeurId().equals(userId)) {
            throw new IllegalStateException("Seul le demandeur peut annuler");
        }
        req.setStatut(LeaveRequest.Statut.ANNULE);
        req.setUpdatedAt(LocalDateTime.now());
        return repository.save(req);
    }
}
