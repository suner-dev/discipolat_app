package com.discipolat.modules.discipleshipPath.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class DiscipleshipPathService {

    private final DiscipleshipPathRepository pathRepo;

    private static final Map<DiscipleshipPath.Stage, String> NEXT_STEPS = Map.of(
        DiscipleshipPath.Stage.DISCOVERY, "Participez aux sessions d'accueil et complétez le questionnaire de découverte.",
        DiscipleshipPath.Stage.FOUNDATION, "Suivez la formation 'Fondations de la foi' (4 semaines).",
        DiscipleshipPath.Stage.GROWTH, "Rejoignez un petit groupe et commencez un plan de lecture biblique.",
        DiscipleshipPath.Stage.SERVICE, "Inscrivez-vous dans un ministère et commencez à servir.",
        DiscipleshipPath.Stage.LEADERSHIP, "Acceptez de former un nouveau membre et prenez la responsabilité d'un petit groupe.",
        DiscipleshipPath.Stage.MATURITY, "Développez votre ministère et mentor d'autres disciples."
    );

    public DiscipleshipPathService(DiscipleshipPathRepository pathRepo) {
        this.pathRepo = pathRepo;
    }

    public DiscipleshipPath getOrCreate(UUID memberId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return pathRepo.findByTenantIdAndMemberId(tenantId, memberId).stream().findFirst()
                .orElseGet(() -> {
                    DiscipleshipPath path = new DiscipleshipPath();
                    path.setTenantId(tenantId);
                    path.setMemberId(memberId);
                    path.setCurrentStage(DiscipleshipPath.Stage.DISCOVERY);
                    path.setRecommendedNextStep(NEXT_STEPS.get(DiscipleshipPath.Stage.DISCOVERY));
                    return pathRepo.save(path);
                });
    }

    public DiscipleshipPath advanceStage(UUID id) {
        DiscipleshipPath path = pathRepo.findById(id).orElseThrow();
        DiscipleshipPath.Stage[] stages = DiscipleshipPath.Stage.values();
        int idx = path.getCurrentStage().ordinal();
        if (idx < stages.length - 1) {
            path.setCurrentStage(stages[idx + 1]);
            path.setRecommendedNextStep(NEXT_STEPS.get(stages[idx + 1]));
            path.setProgressPercent(((double)(idx + 1) / (stages.length - 1)) * 100);
            path.setLastActivityAt(LocalDateTime.now());
        } else {
            path.setStatus(DiscipleshipPath.Status.COMPLETED);
            path.setProgressPercent(100.0);
            path.setCompletedAt(LocalDateTime.now());
        }
        return pathRepo.save(path);
    }

    public List<DiscipleshipPath> listAll() {
        return pathRepo.findByTenantIdAndStatusOrderByCreatedAtDesc(
                TenantContext.getCurrentTenantId(), DiscipleshipPath.Status.ACTIVE);
    }

    public List<DiscipleshipPath> listByStage(DiscipleshipPath.Stage stage) {
        return pathRepo.findByTenantIdAndCurrentStageOrderByCreatedAtDesc(
                TenantContext.getCurrentTenantId(), stage);
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        for (DiscipleshipPath.Stage stage : DiscipleshipPath.Stage.values()) {
            stats.put(stage.name(), pathRepo.findByTenantIdAndCurrentStageOrderByCreatedAtDesc(tenantId, stage).size());
        }
        return stats;
    }
}
