package com.discipolat.modules.makerTracking.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class MakerTrackingService {

    private final MakerTrackingRepository repo;

    public MakerTrackingService(MakerTrackingRepository repo) { this.repo = repo; }

    public List<MakerTracking> listByFaiseur(UUID faiseurId) {
        return repo.findByFaiseurIdOrderByDateEvenementDesc(faiseurId);
    }

    /** Liste paginée des évènements de suivi d'un faiseur. */
    public Page<MakerTracking> listByFaiseurPage(UUID faiseurId, Pageable pageable) {
        return repo.findByFaiseurIdOrderByDateEvenementDesc(faiseurId, pageable);
    }

    public MakerTracking get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("MakerTracking", id));
    }

    public MakerTracking create(MakerTracking tracking) {
        tracking.setTenantId(TenantContext.getCurrentTenantId());
        return repo.save(tracking);
    }

    public void delete(UUID id) { repo.deleteById(id); }

    public Map<String, Object> getResume(UUID faiseurId) {
        List<MakerTracking> events = repo.findByFaiseurIdOrderByDateEvenementDesc(faiseurId);
        Map<String, Object> resume = new HashMap<>();
        resume.put("totalEvenements", events.size());
        resume.put("totalPoints", events.stream().mapToInt(MakerTracking::getPointsGagnes).sum());
        resume.put("formations", events.stream().filter(e -> e.getTypeEvenement() == MakerTracking.TypeEvenement.FORMATION).count());
        resume.put("competencesAcquises", events.stream().filter(e -> e.getTypeEvenement() == MakerTracking.TypeEvenement.COMPETENCE_ACQUISE).count());
        resume.put("amesAccompagnees", events.stream().filter(e -> e.getTypeEvenement() == MakerTracking.TypeEvenement.AIME_ACCOMPAGNEE).count());
        resume.put("defisReussis", events.stream().filter(e -> e.getTypeEvenement() == MakerTracking.TypeEvenement.DEFI_REUSSI).count());
        resume.put("certificats", events.stream().filter(e -> e.getTypeEvenement() == MakerTracking.TypeEvenement.CERTIFICAT).count());
        return resume;
    }
}
