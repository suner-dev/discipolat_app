package com.discipolat.modules.familyCohesion.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class FamilyCohesionService {

    private final FamilyCohesionRepository repository;

    public FamilyCohesionService(FamilyCohesionRepository repository) {
        this.repository = repository;
    }

    public FamilyCohesion getLatest(UUID familleId) {
        return repository.findByFamilleIdOrderByCalculéLeDesc(familleId).orElse(null);
    }

    public FamilyCohesion calculate(UUID familleId, double tauxParticipation, int diversité, int équilibre) {
        FamilyCohesion cohesion = new FamilyCohesion();
        cohesion.setTenantId(TenantContext.getCurrentTenantId());
        cohesion.setFamilleId(familleId);
        cohesion.setTauxParticipation(tauxParticipation);
        cohesion.setDiversitéÂmes(diversité);
        cohesion.setÉquilibreCharges(équilibre);

        // Score formula: weighted average
        double score = (tauxParticipation * 0.4) +
                       (Math.min(diversité / 10.0, 1.0) * 0.3) +
                       (Math.min(équilibre / 10.0, 1.0) * 0.3);
        cohesion.setScoreCohésion(Math.round(score * 100) / 100.0);

        // Generate recommendations
        List<String> recs = new ArrayList<>();
        if (tauxParticipation < 0.6) recs.add("Le taux de participation est faible — envisagez des activités pour revitaliser la famille");
        if (diversité < 3) recs.add("Peu de diversité dans les profils — encouragez l'intégration de nouveaux profils");
        if (équilibre < 4) recs.add("Charges inégalement réparties — redistribuez les tâches");
        if (score > 0.8) recs.add("Excellente cohésion ! Continuez comme ça");
        cohesion.setRecommandations(String.join(" | ", recs));

        return repository.save(cohesion);
    }
}
