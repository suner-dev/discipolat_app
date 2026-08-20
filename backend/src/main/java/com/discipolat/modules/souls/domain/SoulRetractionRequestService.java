package com.discipolat.modules.souls.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SoulRetractionRequestService {

    private final SoulRetractionRequestRepository repository;
    private final SoulService soulService;
    private final SecurityUtils securityUtils;

    public SoulRetractionRequestService(SoulRetractionRequestRepository repository,
                                         SoulService soulService,
                                         SecurityUtils securityUtils) {
        this.repository = repository;
        this.soulService = soulService;
        this.securityUtils = securityUtils;
    }

    public SoulRetractionRequest create(UUID ameId, String justification) {
        soulService.assertAccessible(ameId);
        SoulRetractionRequest request = SoulRetractionRequest.builder()
                .ameId(ameId)
                .demandeurId(securityUtils.getCurrentUserId())
                .justification(justification)
                .statut("EN_ATTENTE")
                .build();
        return repository.save(request);
    }

    @Transactional(readOnly = true)
    public SoulRetractionRequest findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SoulRetractionRequest", id));
    }

    @Transactional(readOnly = true)
    public Page<SoulRetractionRequest> findByStatut(String statut, Pageable pageable) {
        Page<SoulRetractionRequest> all = repository.findByStatutOrderByCreatedAtDesc(statut, pageable);
        if (securityUtils.isSuperUser()) return all;
        List<UUID> accessibleSoulIds = new java.util.ArrayList<>(soulService.accessibleSoulIds());
        if (accessibleSoulIds.isEmpty()) {
            return new org.springframework.data.domain.PageImpl<>(
                    java.util.List.of(), pageable, 0);
        }
        List<SoulRetractionRequest> filtered = all.getContent().stream()
                .filter(r -> accessibleSoulIds.contains(r.getAmeId()))
                .toList();
        return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public List<SoulRetractionRequest> findByAmeId(UUID ameId) {
        soulService.assertAccessible(ameId);
        return repository.findByAmeIdOrderByCreatedAtDesc(ameId);
    }

    public SoulRetractionRequest approve(UUID id, String commentaire) {
        SoulRetractionRequest request = findById(id);
        soulService.assertAccessible(request.getAmeId());
        request.setStatut("APPROUVEE");
        request.setTraitePar(securityUtils.getCurrentUserId());
        request.setDateTraitement(LocalDateTime.now());
        request.setCommentaireReponse(commentaire);
        return repository.save(request);
    }

    public SoulRetractionRequest reject(UUID id, String commentaire) {
        SoulRetractionRequest request = findById(id);
        soulService.assertAccessible(request.getAmeId());
        request.setStatut("REJETEE");
        request.setTraitePar(securityUtils.getCurrentUserId());
        request.setDateTraitement(LocalDateTime.now());
        request.setCommentaireReponse(commentaire);
        return repository.save(request);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return repository.countByStatut("EN_ATTENTE");
    }
}
