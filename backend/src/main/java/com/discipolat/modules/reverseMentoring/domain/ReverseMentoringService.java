package com.discipolat.modules.reverseMentoring.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ReverseMentoringService {

    private final ReverseMentoringRepository repo;

    public ReverseMentoringService(ReverseMentoringRepository repo) { this.repo = repo; }

    public ReverseMentoringRequest create(ReverseMentoringRequest req) {
        req.setTenantId(TenantContext.getCurrentTenantId());
        req.setStatus(ReverseMentoringRequest.Status.PENDING);
        req.setCreatedAt(LocalDateTime.now());
        return repo.save(req);
    }

    public ReverseMentoringRequest accept(UUID id, UUID mentorId) {
        ReverseMentoringRequest req = repo.findById(id).orElseThrow();
        req.setStatus(ReverseMentoringRequest.Status.ACCEPTED);
        req.setAssignedMentorId(mentorId);
        return repo.save(req);
    }

    public ReverseMentoringRequest resolve(UUID id, String outcome) {
        ReverseMentoringRequest req = repo.findById(id).orElseThrow();
        req.setStatus(ReverseMentoringRequest.Status.COMPLETED);
        req.setOutcome(outcome);
        req.setResolvedAt(LocalDateTime.now());
        return repo.save(req);
    }

    public List<ReverseMentoringRequest> listPending() {
        return repo.findByTenantIdAndStatusOrderByCreatedAtDesc(
                TenantContext.getCurrentTenantId(), ReverseMentoringRequest.Status.PENDING);
    }

    public List<ReverseMentoringRequest> listAll() {
        return repo.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        var all = repo.findByTenantIdOrderByCreatedAtDesc(tenantId);
        stats.put("total", all.size());
        stats.put("pending", all.stream().filter(r -> r.getStatus() == ReverseMentoringRequest.Status.PENDING).count());
        stats.put("completed", all.stream().filter(r -> r.getStatus() == ReverseMentoringRequest.Status.COMPLETED).count());
        return stats;
    }
}
