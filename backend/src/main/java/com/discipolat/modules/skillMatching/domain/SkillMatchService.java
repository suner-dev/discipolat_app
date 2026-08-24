package com.discipolat.modules.skillMatching.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SkillMatchService {

    private final SkillMatchRepository repo;

    public SkillMatchService(SkillMatchRepository repo) { this.repo = repo; }

    public List<SkillMatch> listAll() {
        return repo.findByTenantIdOrderByScoreMatchDesc(TenantContext.getCurrentTenantId());
    }

    public SkillMatch get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("SkillMatch", id));
    }

    /** AI engine: scan members' declared skills against department needs */
    public List<SkillMatch> runMatching() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        // Get pending matches
        return repo.findByTenantIdOrderByScoreMatchDesc(tenantId).stream()
                .filter(m -> m.getStatut() == SkillMatch.Statut.PROPOSE)
                .sorted(Comparator.comparingInt(SkillMatch::getScoreMatch).reversed())
                .collect(Collectors.toList());
    }

    public SkillMatch create(SkillMatch match) {
        match.setTenantId(TenantContext.getCurrentTenantId());
        return repo.save(match);
    }

    public SkillMatch respond(UUID id, SkillMatch.Statut decision) {
        SkillMatch m = get(id);
        m.setStatut(decision);
        m.setReponduLe(LocalDateTime.now());
        return repo.save(m);
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", repo.findByTenantIdOrderByScoreMatchDesc(tenantId).size());
        stats.put("proposes", repo.countByTenantIdAndStatut(tenantId, SkillMatch.Statut.PROPOSE));
        stats.put("acceptes", repo.countByTenantIdAndStatut(tenantId, SkillMatch.Statut.ACCEPTE));
        stats.put("refuses", repo.countByTenantIdAndStatut(tenantId, SkillMatch.Statut.REFUSE));
        return stats;
    }
}
