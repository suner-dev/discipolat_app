package com.discipolat.modules.discipline.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class SoulDisciplineEventService {

    private final SoulDisciplineEventRepository repository;
    private final SecurityUtils securityUtils;

    public SoulDisciplineEventService(SoulDisciplineEventRepository repository,
                                      SecurityUtils securityUtils) {
        this.repository = repository;
        this.securityUtils = securityUtils;
    }

    public SoulDisciplineEvent create(UUID ameId, CategorieDiscipline categorie, String typeEvenement,
                                      String titre, String description, GraviteDiscipline gravite,
                                      LocalDate dateEvenement) {
        SoulDisciplineEvent event = SoulDisciplineEvent.builder()
                .ameId(ameId)
                .auteurId(securityUtils.getCurrentUserId())
                .categorie(categorie)
                .typeEvenement(typeEvenement)
                .gravite(gravite)
                .titre(titre)
                .description(description)
                .dateEvenement(dateEvenement != null ? dateEvenement : LocalDate.now())
                .resolu(false)
                .deleted(false)
                .build();
        return repository.save(event);
    }

    @Transactional(readOnly = true)
    public SoulDisciplineEvent findById(UUID id) {
        return repository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("SoulDisciplineEvent", id));
    }

    @Transactional(readOnly = true)
    public Page<SoulDisciplineEvent> findByAmeId(UUID ameId, Pageable pageable) {
        return repository.findByAmeIdAndDeletedFalse(ameId, pageable);
    }

    @Transactional(readOnly = true)
    public List<SoulDisciplineEvent> findByAmeId(UUID ameId) {
        return repository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(ameId);
    }

    @Transactional(readOnly = true)
    public Page<SoulDisciplineEvent> findByAmeIdAndCategorie(UUID ameId, CategorieDiscipline categorie, Pageable pageable) {
        return repository.findByAmeIdAndCategorieAndDeletedFalse(ameId, categorie, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(UUID ameId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", repository.countByAmeIdAndDeletedFalse(ameId));
        stats.put("nonResolus", repository.countByAmeIdAndResoluFalseAndDeletedFalse(ameId));

        Map<String, Long> parCategorie = new LinkedHashMap<>();
        for (CategorieDiscipline cat : CategorieDiscipline.values()) {
            long count = repository.countByAmeIdAndCategorieAndDeletedFalse(ameId, cat);
            if (count > 0) parCategorie.put(cat.name(), count);
        }
        stats.put("parCategorie", parCategorie);
        return stats;
    }

    public SoulDisciplineEvent resolve(UUID id, String resolution) {
        SoulDisciplineEvent event = findById(id);
        event.setResolu(true);
        event.setDateResolution(LocalDate.now());
        event.setResoluPar(securityUtils.getCurrentUserId());
        if (resolution != null && !resolution.isEmpty()) {
            event.setDescription(event.getDescription() != null
                    ? event.getDescription() + "\n[RÉSOLU] " + resolution
                    : "[RÉSOLU] " + resolution);
        }
        return repository.save(event);
    }

    public void delete(UUID id) {
        SoulDisciplineEvent event = findById(id);
        event.setDeleted(true);
        repository.save(event);
    }
}
