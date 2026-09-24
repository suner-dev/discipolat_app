package com.discipolat.modules.config.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.config.domain.SpaceTemplate;
import com.discipolat.modules.config.service.SpaceTemplateService;
import com.discipolat.modules.spaces.domain.Space;
import com.discipolat.modules.spaces.domain.SpaceService;
import com.discipolat.modules.spaces.domain.SpaceStatus;
import com.discipolat.modules.spaces.domain.SpaceType;
import com.discipolat.modules.spaces.domain.VisiblePeopleScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/space-templates")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
public class SpaceTemplateController {

    private final SpaceTemplateService spaceTemplateService;
    private final SpaceService spaceService;

    public SpaceTemplateController(SpaceTemplateService spaceTemplateService, SpaceService spaceService) {
        this.spaceTemplateService = spaceTemplateService;
        this.spaceService = spaceService;
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
        SpaceTemplate template = spaceTemplateService.getTemplateByCode(code);
        UUID organizationUnitId = UUID.fromString(String.valueOf(request.get("organizationUnitId")));
        String name = String.valueOf(request.getOrDefault("name", template.getName()));
        String spaceCode = String.valueOf(request.getOrDefault("code", template.getCode()));
        SpaceType type;
        try {
            type = SpaceType.valueOf(String.valueOf(request.getOrDefault("spaceType", "DEPARTMENT"))
                    .toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Type d'espace invalide", exception);
        }
        Map<String, Object> configuration = new LinkedHashMap<>();
        configuration.put("templateCode", template.getCode());
        configuration.put("modules", template.getModulesJson());
        configuration.put("workflows", template.getDefaultWorkflowsJson());
        configuration.put("statuses", template.getDefaultStatusesJson());
        configuration.put("dashboards", template.getDefaultDashboardsJson());
        Space created = spaceService.createSpace(TenantContext.requireTenantId(), SecurityUtils.getCurrentUserId(),
                new SpaceService.SpaceCommand(organizationUnitId, type, template.getCode(), name, spaceCode,
                        template.getIcon(), template.getColor(), template.getDescription(), SpaceStatus.ACTIVE,
                        VisiblePeopleScope.CHURCH, configuration));
        return ResponseEntity.status(201).body(Map.of(
                "space", created,
                "template", template,
                "message", "Espace créé depuis le template"
        ));
    }
}