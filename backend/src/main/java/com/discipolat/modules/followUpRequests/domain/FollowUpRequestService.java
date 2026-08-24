package com.discipolat.modules.followUpRequests.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * P3 #112 — Demandes de suivi (membre).
 */
@Service
@Transactional
public class FollowUpRequestService {

    private final FollowUpRequestRepository repository;

    public FollowUpRequestService(FollowUpRequestRepository repository) {
        this.repository = repository;
    }

    public FollowUpRequest create(UUID requesterId, String requesterName, String type,
                                  String message, UUID preferredFamilyId) {
        FollowUpRequest r = new FollowUpRequest();
        r.setTenantId(TenantContext.getCurrentTenantId());
        r.setRequesterId(requesterId);
        r.setRequesterName(requesterName);
        if (type != null) {
            try {
                r.setType(FollowUpRequest.Type.valueOf(type));
            } catch (IllegalArgumentException ignored) { /* valeur par défaut */ }
        }
        r.setMessage(message);
        r.setPreferredFamilyId(preferredFamilyId);
        return repository.save(r);
    }

    @Transactional(readOnly = true)
    public List<FollowUpRequest> myRequests(UUID userId) {
        return repository.findByRequesterIdOrderByCreatedAtDesc(userId);
    }

    /** File d'attente pour les responsables (pasteur, admin, chefs). */
    @Transactional(readOnly = true)
    public List<FollowUpRequest> pending() {
        return repository.findByTenantIdAndStatusOrderByCreatedAtAsc(
                TenantContext.getCurrentTenantId(), FollowUpRequest.Status.EN_ATTENTE);
    }

    @Transactional(readOnly = true)
    public List<FollowUpRequest> assignedToMe(UUID userId) {
        return repository.findByAssignedToIdOrderByUpdatedAtDesc(userId);
    }

    public FollowUpRequest assign(UUID id, UUID assignedToId, String assignedToName) {
        FollowUpRequest r = getById(id);
        r.setAssignedToId(assignedToId);
        r.setAssignedToName(assignedToName);
        r.setStatus(FollowUpRequest.Status.ASSIGNEE);
        r.setUpdatedAt(LocalDateTime.now());
        return repository.save(r);
    }

    public FollowUpRequest updateStatus(UUID id, String status, String notes) {
        FollowUpRequest r = getById(id);
        try {
            r.setStatus(FollowUpRequest.Status.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Statut invalide : " + status);
        }
        if (notes != null) r.setResolutionNotes(notes);
        r.setUpdatedAt(LocalDateTime.now());
        return repository.save(r);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> s = new HashMap<>();
        s.put("enAttente", repository.countByTenantIdAndStatus(tenantId, FollowUpRequest.Status.EN_ATTENTE));
        s.put("assignees", repository.countByTenantIdAndStatus(tenantId, FollowUpRequest.Status.ASSIGNEE));
        s.put("enCours", repository.countByTenantIdAndStatus(tenantId, FollowUpRequest.Status.EN_COURS));
        s.put("terminees", repository.countByTenantIdAndStatus(tenantId, FollowUpRequest.Status.TERMINEE));
        return s;
    }

    @Transactional(readOnly = true)
    public FollowUpRequest getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("FollowUpRequest", id));
    }
}
