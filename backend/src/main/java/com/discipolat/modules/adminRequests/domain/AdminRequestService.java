package com.discipolat.modules.adminRequests.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class AdminRequestService {

    private final AdminRequestRepository repo;

    public AdminRequestService(AdminRequestRepository repo) { this.repo = repo; }

    public List<AdminRequest> listAll() {
        return repo.findByTenantIdOrderBySoumiseLeDesc(TenantContext.getCurrentTenantId());
    }

    public List<AdminRequest> listByMember(UUID membreId) {
        return repo.findByDemandeurIdOrderBySoumiseLeDesc(membreId);
    }

    public AdminRequest get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("AdminRequest", id));
    }

    public AdminRequest create(AdminRequest req) {
        req.setTenantId(TenantContext.getCurrentTenantId());
        return repo.save(req);
    }

    public AdminRequest process(UUID id, AdminRequest.Statut decision, UUID traiteurId, String commentaire) {
        AdminRequest req = get(id);
        req.setStatut(decision);
        req.setTraitePar(traiteurId);
        req.setTraiteLe(LocalDateTime.now());
        req.setCommentaireTraitement(commentaire);
        return repo.save(req);
    }

    public void delete(UUID id) { repo.deleteById(id); }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", repo.findByTenantIdOrderBySoumiseLeDesc(tenantId).size());
        stats.put("enExamen", repo.countByTenantIdAndStatut(tenantId, AdminRequest.Statut.EN_EXAMEN));
        stats.put("approuvees", repo.countByTenantIdAndStatut(tenantId, AdminRequest.Statut.APPROUVEE));
        stats.put("rejetees", repo.countByTenantIdAndStatut(tenantId, AdminRequest.Statut.REJETEE));
        return stats;
    }
}
