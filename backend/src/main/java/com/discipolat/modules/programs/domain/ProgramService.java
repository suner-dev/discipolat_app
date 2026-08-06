package com.discipolat.modules.programs.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.programs.api.ProgramTypeRequest;
import com.discipolat.modules.programs.api.ProgramTypeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Configuration des types de programmes (système de présences flexible).
 * Le pasteur configure les types (dimanche, convention, séminaire, retraite, campagne, etc.)
 * et leurs sous-programmes (ex : premier culte, deuxième culte).
 */
@Service
@Transactional
public class ProgramService {

    private final ProgramTypeRepository programTypeRepository;
    private final ProgramSubTypeRepository programSubTypeRepository;
    private final SecurityUtils securityUtils;

    public ProgramService(ProgramTypeRepository programTypeRepository,
                          ProgramSubTypeRepository programSubTypeRepository,
                          SecurityUtils securityUtils) {
        this.programTypeRepository = programTypeRepository;
        this.programSubTypeRepository = programSubTypeRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public List<ProgramTypeResponse> findAll(boolean actifsSeulement) {
        List<ProgramType> types = actifsSeulement
                ? programTypeRepository.findByActifTrueOrderByOrdreAsc()
                : programTypeRepository.findAllByOrderByOrdreAsc();
        return types.stream()
                .map(t -> ProgramTypeResponse.from(t, programSubTypeRepository
                        .findByProgramTypeIdOrderByOrdreAsc(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgramTypeResponse> findActifs() {
        return findAll(true);
    }

    @Transactional(readOnly = true)
    public ProgramTypeResponse findById(UUID id) {
        ProgramType type = programTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ProgramType", id));
        return ProgramTypeResponse.from(type,
                programSubTypeRepository.findByProgramTypeIdOrderByOrdreAsc(id));
    }

    public ProgramTypeResponse create(ProgramTypeRequest request) {
        if (programTypeRepository.findByCode(request.code()).isPresent()) {
            throw new BusinessRuleException("Un type de programme avec ce code existe déjà: "
                    + request.code(), "DUPLICATE_PROGRAM_TYPE");
        }
        ProgramType type = ProgramType.builder()
                .code(request.code().toUpperCase())
                .label(request.label())
                .description(request.description())
                .aSousProgrammes(request.aSousProgrammes())
                .couleur(request.couleur())
                .actif(request.actif())
                .ordre(request.ordre() != null ? request.ordre() : 0)
                .createdBy(securityUtils.getCurrentUserId())
                .build();
        type = programTypeRepository.save(type);

        saveSubTypes(type.getId(), request.sousProgrammes());
        return findById(type.getId());
    }

    public ProgramTypeResponse update(UUID id, ProgramTypeRequest request) {
        ProgramType type = programTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ProgramType", id));
        type.setLabel(request.label());
        type.setDescription(request.description());
        type.setASousProgrammes(request.aSousProgrammes());
        type.setCouleur(request.couleur());
        type.setActif(request.actif());
        type.setOrdre(request.ordre() != null ? request.ordre() : type.getOrdre());
        programTypeRepository.save(type);

        if (request.aSousProgrammes()) {
            programSubTypeRepository.deleteByProgramTypeId(id);
            saveSubTypes(id, request.sousProgrammes());
        }
        return findById(id);
    }

    public void delete(UUID id) {
        programTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ProgramType", id));
        programSubTypeRepository.deleteByProgramTypeId(id);
        programTypeRepository.deleteById(id);
    }

    private void saveSubTypes(UUID programTypeId, List<ProgramTypeRequest.SubTypeRequest> subs) {
        if (subs == null) return;
        int ordre = 0;
        for (ProgramTypeRequest.SubTypeRequest sub : subs) {
            if (sub.label() == null || sub.label().isBlank()) continue;
            programSubTypeRepository.save(ProgramSubType.builder()
                    .programTypeId(programTypeId)
                    .label(sub.label())
                    .heureDebut(sub.heureDebut() != null ? LocalTime.parse(sub.heureDebut()) : null)
                    .heureFin(sub.heureFin() != null ? LocalTime.parse(sub.heureFin()) : null)
                    .actif(sub.actif())
                    .ordre(sub.ordre() != null ? sub.ordre() : ordre++)
                    .build());
        }
    }
}
