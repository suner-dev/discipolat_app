package com.discipolat.modules.config.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.config.domain.SpaceTemplate;
import com.discipolat.modules.config.repository.SpaceTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SpaceTemplateService {

    private final SpaceTemplateRepository spaceTemplateRepository;

    public SpaceTemplateService(SpaceTemplateRepository spaceTemplateRepository) {
        this.spaceTemplateRepository = spaceTemplateRepository;
    }

    @Transactional(readOnly = true)
    public List<SpaceTemplate> getAllTemplates() {
        return spaceTemplateRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Page<SpaceTemplate> getAllTemplates(Pageable pageable) {
        return spaceTemplateRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public SpaceTemplate getTemplateByCode(String code) {
        return spaceTemplateRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("SpaceTemplate with code: " + code));
    }

    @Transactional(readOnly = true)
    public SpaceTemplate getTemplateById(UUID id) {
        return spaceTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SpaceTemplate", id));
    }

    public SpaceTemplate createTemplate(SpaceTemplate template) {
        if (spaceTemplateRepository.existsByCode(template.getCode())) {
            throw new IllegalArgumentException("Un template avec ce code existe déjà : " + template.getCode());
        }
        return spaceTemplateRepository.save(template);
    }

    public SpaceTemplate updateTemplate(UUID id, SpaceTemplate updated) {
        SpaceTemplate existing = getTemplateById(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setIcon(updated.getIcon());
        existing.setColor(updated.getColor());
        existing.setVersion(existing.getVersion() + 1);
        existing.setModulesJson(updated.getModulesJson());
        existing.setDefaultWorkflowsJson(updated.getDefaultWorkflowsJson());
        existing.setDefaultStatusesJson(updated.getDefaultStatusesJson());
        existing.setDefaultDashboardsJson(updated.getDefaultDashboardsJson());
        return spaceTemplateRepository.save(existing);
    }

    public void deleteTemplate(UUID id) {
        SpaceTemplate template = getTemplateById(id);
        spaceTemplateRepository.delete(template);
    }
}