package com.discipolat.modules.parallelfollowups.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutSuiviParallele;
import com.discipolat.modules.parallelfollowups.api.CreateParallelFollowupRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class ParallelFollowupService {

    private final ParallelFollowupRepository repository;

    public ParallelFollowupService(ParallelFollowupRepository repository) {
        this.repository = repository;
    }

    public ParallelFollowup create(CreateParallelFollowupRequest request) {
        ParallelFollowup followup = ParallelFollowup.builder()
                .ameId(request.ameId())
                .initiateurId(request.initiateurId())
                .familleId(request.familleId())
                .raison(request.raison())
                .raisonDetail(request.raisonDetail())
                .dateDebut(request.dateDebut() != null ? request.dateDebut() : LocalDate.now())
                .statut(StatutSuiviParallele.EN_COURS)
                .build();
        return repository.save(followup);
    }

    @Transactional(readOnly = true)
    public ParallelFollowup findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ParallelFollowup", id));
    }

    @Transactional(readOnly = true)
    public Page<ParallelFollowup> findAll(UUID initiateurId, String statut, Pageable pageable) {
        if (initiateurId != null && statut != null) {
            return repository.findByInitiateurIdAndStatut(initiateurId,
                    StatutSuiviParallele.valueOf(statut), pageable);
        }
        if (initiateurId != null) return repository.findByInitiateurId(initiateurId, pageable);
        if (statut != null) return repository.findByStatut(StatutSuiviParallele.valueOf(statut), pageable);
        return repository.findAll(pageable);
    }

    public ParallelFollowup close(UUID id) {
        ParallelFollowup followup = findById(id);
        followup.setStatut(StatutSuiviParallele.CLOTURE);
        followup.setDateFin(LocalDate.now());
        return repository.save(followup);
    }

    @Transactional(readOnly = true)
    public Page<ParallelFollowup> findActive(Pageable pageable) {
        return repository.findByStatut(StatutSuiviParallele.EN_COURS, pageable);
    }

    @Transactional(readOnly = true)
    public long countActiveByFamille(UUID familleId) {
        return repository.countByFamilleIdAndStatut(familleId, StatutSuiviParallele.EN_COURS);
    }
}
