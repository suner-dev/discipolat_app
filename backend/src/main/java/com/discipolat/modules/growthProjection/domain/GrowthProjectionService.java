package com.discipolat.modules.growthProjection.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class GrowthProjectionService {

    private final GrowthProjectionRepository repo;

    public GrowthProjectionService(GrowthProjectionRepository repo) { this.repo = repo; }

    public List<GrowthProjection> listAll() {
        return repo.findByTenantIdOrderByCalculeLeDesc(TenantContext.getCurrentTenantId());
    }

    public GrowthProjection get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("GrowthProjection", id));
    }

    /**
     * Simulate growth projection using exponential model:
     * projected = current * (1 + rate/12)^months + conversions - departures
     */
    public GrowthProjection simulate(GrowthProjection proj) {
        proj.setTenantId(TenantContext.getCurrentTenantId());
        double monthlyRate = proj.getTauxCroissanceAnnuel() / 100.0 / 12.0;
        int projected = (int) Math.round(proj.getEffectifActuel() * Math.pow(1 + monthlyRate, proj.getMoisProjection()));
        proj.setEffectifProjete(projected);
        proj.setCalculeLe(LocalDateTime.now());

        // Auto-generate recommendations
        if (projected > proj.getEffectifActuel() * 1.1) {
            proj.setRecommandations("Croissance forte prévue — prévoir l'intégration de nouveaux membres et le renforcement des équipes.");
        } else if (projected < proj.getEffectifActuel() * 0.95) {
            proj.setRecommandations("Déclin prévu — déclencher le plan de revitalisation et renforcer l'accompagnement pastoral.");
        } else {
            proj.setRecommandations("Croissance stable — maintenir les efforts actuels et identifier des leviers de croissance.");
        }
        return repo.save(proj);
    }

    public GrowthProjection save(GrowthProjection proj) {
        proj.setTenantId(TenantContext.getCurrentTenantId());
        proj.setCalculeLe(LocalDateTime.now());
        return repo.save(proj);
    }

    public void delete(UUID id) { repo.deleteById(id); }
}
