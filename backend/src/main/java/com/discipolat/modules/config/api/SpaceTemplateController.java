package com.discipolat.modules.config.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.config.domain.SpaceTemplate;
import com.discipolat.modules.config.service.SpaceTemplateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/space-templates")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
public class SpaceTemplateController {

    private final SpaceTemplateService spaceTemplateService;

    public SpaceTemplateController(SpaceTemplateService spaceTemplateService) {
        this.spaceTemplateService = spaceTemplateService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<SpaceTemplate>> getAllTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("name"));
        Page<SpaceTemplate> templates = spaceTemplateService.getAllTemplates(pageable);
        return ResponseEntity.ok(PageResponse.of(
                templates.getContent(), templates.getNumber(), templates.getSize(),
                templates.getTotalElements(), templates.getTotalPages()));
    }

    @GetMapping("/list")
    public ResponseEntity<List<SpaceTemplate>> getAllTemplatesList() {
        return ResponseEntity.ok(spaceTemplateService.getAllTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpaceTemplate> getTemplateById(@PathVariable UUID id) {
        return ResponseEntity.ok(spaceTemplateService.getTemplateById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<SpaceTemplate> getTemplateByCode(@PathVariable String code) {
        return ResponseEntity.ok(spaceTemplateService.getTemplateByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpaceTemplate> createTemplate(@RequestBody SpaceTemplate template) {
        return ResponseEntity.ok(spaceTemplateService.createTemplate(template));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpaceTemplate> updateTemplate(@PathVariable UUID id, @RequestBody SpaceTemplate template) {
        return ResponseEntity.ok(spaceTemplateService.updateTemplate(id, template));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        spaceTemplateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Crée un espace à partir d'un template (clone la configuration)
     */
    @PostMapping("/{code}/create-space")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'DEPARTMENT_LEADER', 'FAMILY_LEADER')")
    public ResponseEntity<Map<String, Object>> createSpaceFromTemplate(
            @PathVariable String code,
            @RequestBody Map<String, Object> request) {
        // TODO: Implémenter la création d'espace depuis template
        // Nécessite SpaceService, OrganizationNode, etc.
        // Pour l'instant, retourner le template pour que le frontend puisse l'utiliser
        SpaceTemplate template = spaceTemplateService.getTemplateByCode(code);
        return ResponseEntity.ok(Map.of(
                "template", template,
                "message", "Template récupéré. Implémentation création espace à faire dans SpaceService."
        ));
    }
}