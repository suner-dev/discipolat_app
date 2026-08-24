package com.discipolat.modules.forms.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@Service
@Transactional
public class FormService {

    private final FormTemplateRepository templateRepo;
    private final FormResponseRepository responseRepo;

    public FormService(FormTemplateRepository templateRepo, FormResponseRepository responseRepo) {
        this.templateRepo = templateRepo;
        this.responseRepo = responseRepo;
    }

    public List<FormTemplate> listTemplates() {
        return templateRepo.findByTenantIdOrderByCreeLeDesc(TenantContext.getCurrentTenantId());
    }

    public List<FormTemplate> listPublished() {
        return templateRepo.findByTenantIdAndStatutOrderByCreeLeDesc(
                TenantContext.getCurrentTenantId(), FormTemplate.Statut.PUBLIE);
    }

    public FormTemplate getTemplate(UUID id) {
        return templateRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FormTemplate", id));
    }

    public FormTemplate createTemplate(FormTemplate t) {
        t.setTenantId(TenantContext.getCurrentTenantId());
        return templateRepo.save(t);
    }

    public FormTemplate updateTemplate(UUID id, FormTemplate updates) {
        FormTemplate t = getTemplate(id);
        if (updates.getTitre() != null) t.setTitre(updates.getTitre());
        if (updates.getDescription() != null) t.setDescription(updates.getDescription());
        if (updates.getFieldsJson() != null) t.setFieldsJson(updates.getFieldsJson());
        if (updates.getCategorie() != null) t.setCategorie(updates.getCategorie());
        t.setAnonyme(updates.isAnonyme());
        t.setExpireLe(updates.getExpireLe());
        return templateRepo.save(t);
    }

    public FormTemplate publishTemplate(UUID id) {
        FormTemplate t = getTemplate(id);
        t.setStatut(FormTemplate.Statut.PUBLIE);
        t.setPublieLe(LocalDateTime.now());
        return templateRepo.save(t);
    }

    public FormTemplate archiveTemplate(UUID id) {
        FormTemplate t = getTemplate(id);
        t.setStatut(FormTemplate.Statut.ARCHIVE);
        return templateRepo.save(t);
    }

    public void deleteTemplate(UUID id) {
        templateRepo.deleteById(id);
    }

    public FormResponse submitResponse(UUID templateId, FormResponse response) {
        FormTemplate t = getTemplate(templateId);
        if (t.getStatut() != FormTemplate.Statut.PUBLIE) {
            throw new IllegalStateException("Formulaire non publié");
        }
        if (t.getExpireLe() != null && t.getExpireLe().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Formulaire expiré");
        }
        response.setTenantId(TenantContext.getCurrentTenantId());
        response.setFormTemplateId(templateId);
        FormResponse saved = responseRepo.save(response);
        t.setNbReponses(t.getNbReponses() + 1);
        templateRepo.save(t);
        return saved;
    }

    public List<FormResponse> getResponses(UUID templateId) {
        return responseRepo.findByFormTemplateIdOrderBySubmittedAtDesc(templateId);
    }

    public Map<String, Object> getStats(UUID templateId) {
        Map<String, Object> stats = new HashMap<>();
        FormTemplate t = getTemplate(templateId);
        stats.put("template", t.getTitre());
        stats.put("totalReponses", t.getNbReponses());
        stats.put("statut", t.getStatut().name());
        stats.put("creeLe", t.getCreeLe());
        stats.put("publieLe", t.getPublieLe());
        return stats;
    }
}
