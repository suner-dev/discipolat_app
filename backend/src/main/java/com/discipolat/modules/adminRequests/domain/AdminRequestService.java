package com.discipolat.modules.adminRequests.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.exception.UnauthorizedException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class AdminRequestService {

    private final AdminRequestRepository repo;
    private final SecurityUtils securityUtils;

    public AdminRequestService(AdminRequestRepository repo, SecurityUtils securityUtils) {
        this.repo = repo;
        this.securityUtils = securityUtils;
    }

    public List<AdminRequest> listAll() {
        assertAdminOrPasteur();
        return repo.findByTenantIdOrderBySoumiseLeDesc(TenantContext.getCurrentTenantId());
    }

    public List<AdminRequest> listByMember(UUID membreId) {
        if (!securityUtils.isSuperUser()
                && !membreId.equals(securityUtils.getCurrentUserId())) {
            throw new UnauthorizedException("You can only view your own requests");
        }
        return repo.findByDemandeurIdOrderBySoumiseLeDesc(membreId);
    }

    public AdminRequest get(UUID id) {
        AdminRequest req = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AdminRequest", id));
        if (!securityUtils.isSuperUser()
                && !req.getDemandeurId().equals(securityUtils.getCurrentUserId())) {
            throw new UnauthorizedException("You can only view your own requests");
        }
        return req;
    }

    public AdminRequest create(AdminRequest req) {
        req.setTenantId(TenantContext.getCurrentTenantId());
        if (req.getDemandeurId() == null) {
            req.setDemandeurId(securityUtils.getCurrentUserId());
        }
        return repo.save(req);
    }

    public AdminRequest process(UUID id, AdminRequest.Statut decision, UUID traiteurId, String commentaire) {
        assertAdminOrPasteur();
        AdminRequest req = get(id);
        req.setStatut(decision);
        req.setTraitePar(traiteurId);
        req.setTraiteLe(LocalDateTime.now());
        req.setCommentaireTraitement(commentaire);
        return repo.save(req);
    }

    public void delete(UUID id) {
        AdminRequest req = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AdminRequest", id));
        if (!securityUtils.isSuperUser()
                && !req.getDemandeurId().equals(securityUtils.getCurrentUserId())) {
            throw new UnauthorizedException("You can only delete your own requests");
        }
        repo.deleteById(id);
    }

    public Map<String, Object> getStats() {
        assertAdminOrPasteur();
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", repo.findByTenantIdOrderBySoumiseLeDesc(tenantId).size());
        stats.put("enExamen", repo.countByTenantIdAndStatut(tenantId, AdminRequest.Statut.EN_EXAMEN));
        stats.put("approuvees", repo.countByTenantIdAndStatut(tenantId, AdminRequest.Statut.APPROUVEE));
        stats.put("rejetees", repo.countByTenantIdAndStatut(tenantId, AdminRequest.Statut.REJETEE));
        return stats;
    }

    private void assertAdminOrPasteur() {
        if (!securityUtils.isSuperUser()) {
            throw new UnauthorizedException("Admin or Pasteur role required");
        }
    }
}
