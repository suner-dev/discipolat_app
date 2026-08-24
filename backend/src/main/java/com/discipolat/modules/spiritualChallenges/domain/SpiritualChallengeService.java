package com.discipolat.modules.spiritualChallenges.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class SpiritualChallengeService {

    private final SpiritualChallengeRepository repository;

    public SpiritualChallengeService(SpiritualChallengeRepository repository) {
        this.repository = repository;
    }

    public Page<SpiritualChallenge> list(Pageable pageable) {
        return repository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId(), pageable);
    }

    public SpiritualChallenge getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SpiritualChallenge", id));
    }

    public SpiritualChallenge create(String titre, String description, String type,
                                      UUID assignéÀ, int objectifJours, LocalDateTime deadline, UUID userId) {
        SpiritualChallenge challenge = new SpiritualChallenge();
        challenge.setTenantId(TenantContext.getCurrentTenantId());
        challenge.setTitre(titre);
        challenge.setDescription(description);
        challenge.setType(SpiritualChallenge.Type.valueOf(type != null ? type : "AUTRE"));
        challenge.setCreatedBy(userId);
        challenge.setAssignéÀ(assignéÀ);
        challenge.setObjectifJours(objectifJours > 0 ? objectifJours : 7);
        challenge.setDeadline(deadline);
        return repository.save(challenge);
    }

    public SpiritualChallenge progress(UUID id) {
        SpiritualChallenge challenge = getById(id);
        challenge.setJoursComplétés(challenge.getJoursComplétés() + 1);
        if (challenge.getJoursComplétés() >= challenge.getObjectifJours()) {
            challenge.setStatut(SpiritualChallenge.Statut.TERMINÉ);
            challenge.setCompletedAt(LocalDateTime.now());
        }
        return repository.save(challenge);
    }

    public SpiritualChallenge updateStatut(UUID id, String statut) {
        SpiritualChallenge challenge = getById(id);
        challenge.setStatut(SpiritualChallenge.Statut.valueOf(statut));
        if (statut.equals("TERMINÉ")) {
            challenge.setCompletedAt(LocalDateTime.now());
        }
        return repository.save(challenge);
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("enCours", repository.countByTenantIdAndStatut(tenantId, SpiritualChallenge.Statut.EN_COURS));
        stats.put("terminés", repository.countByTenantIdAndStatut(tenantId, SpiritualChallenge.Statut.TERMINÉ));
        stats.put("abandonnés", repository.countByTenantIdAndStatut(tenantId, SpiritualChallenge.Statut.ABANDONNÉ));
        return stats;
    }
}
