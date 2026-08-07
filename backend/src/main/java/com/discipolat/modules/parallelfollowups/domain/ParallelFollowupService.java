package com.discipolat.modules.parallelfollowups.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutSuiviParallele;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.parallelfollowups.api.CreateParallelFollowupRequest;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ParallelFollowupService {

    private final ParallelFollowupRepository repository;
    private final WorkspaceScopeService workspaceScopeService;
    private final SecurityUtils securityUtils;

    public ParallelFollowupService(ParallelFollowupRepository repository,
                                   WorkspaceScopeService workspaceScopeService,
                                   SecurityUtils securityUtils) {
        this.repository = repository;
        this.workspaceScopeService = workspaceScopeService;
        this.securityUtils = securityUtils;
    }

    public ParallelFollowup create(CreateParallelFollowupRequest request) {
        // L'initiateur doit avoir accès à l'âme suivie
        if (!workspaceScopeService.isSuperUser() && !workspaceScopeService.canAccessSoul(request.ameId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé : vous ne pouvez pas créer un suivi pour cette âme");
        }
        UUID initiateurId = workspaceScopeService.isSuperUser() && request.initiateurId() != null
                ? request.initiateurId()
                : securityUtils.getCurrentUserId();
        ParallelFollowup followup = ParallelFollowup.builder()
                .ameId(request.ameId())
                .initiateurId(initiateurId)
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
        ParallelFollowup followup = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ParallelFollowup", id));
        assertAccessible(followup);
        return followup;
    }

    @Transactional(readOnly = true)
    public Page<ParallelFollowup> findAll(UUID initiateurId, String statut, Pageable pageable) {
        Page<ParallelFollowup> page;
        if (initiateurId != null && statut != null) {
            page = repository.findByInitiateurIdAndStatut(initiateurId,
                    StatutSuiviParallele.valueOf(statut), pageable);
        } else if (initiateurId != null) {
            page = repository.findByInitiateurId(initiateurId, pageable);
        } else if (statut != null) {
            page = repository.findByStatut(StatutSuiviParallele.valueOf(statut), pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return scopePage(page, pageable);
    }

    public ParallelFollowup close(UUID id) {
        ParallelFollowup followup = findById(id);
        followup.setStatut(StatutSuiviParallele.CLOTURE);
        followup.setDateFin(LocalDate.now());
        return repository.save(followup);
    }

    @Transactional(readOnly = true)
    public Page<ParallelFollowup> findActive(Pageable pageable) {
        return scopePage(repository.findByStatut(StatutSuiviParallele.EN_COURS, pageable), pageable);
    }

    @Transactional(readOnly = true)
    public long countActiveByFamille(UUID familleId) {
        return repository.countByFamilleIdAndStatut(familleId, StatutSuiviParallele.EN_COURS);
    }

    /** Restreint la page aux suivis de l'espace métier courant (hors super-utilisateurs). */
    private Page<ParallelFollowup> scopePage(Page<ParallelFollowup> page, Pageable pageable) {
        if (workspaceScopeService.isSuperUser()) return page;
        UUID currentUserId = securityUtils.getCurrentUserId();
        List<ParallelFollowup> scoped = page.getContent().stream()
                .filter(f -> f.getInitiateurId() != null && f.getInitiateurId().equals(currentUserId)
                        || (f.getAmeId() != null && workspaceScopeService.canAccessSoul(f.getAmeId())))
                .toList();
        return new PageImpl<>(scoped, pageable, scoped.size());
    }

    private void assertAccessible(ParallelFollowup followup) {
        if (workspaceScopeService.isSuperUser()) return;
        UUID currentUserId = securityUtils.getCurrentUserId();
        boolean own = followup.getInitiateurId() != null && followup.getInitiateurId().equals(currentUserId);
        boolean soulAccessible = followup.getAmeId() != null && workspaceScopeService.canAccessSoul(followup.getAmeId());
        if (!own && !soulAccessible) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé : ce suivi ne fait pas partie de votre espace métier");
        }
    }
}
