package com.discipolat.modules.tenants.api;

import com.discipolat.modules.tenants.domain.OrganizationLevel;
import com.discipolat.modules.tenants.domain.OrganizationLevelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Référentiel de <b>niveaux configurables</b> d'une dénomination —
 * SPEC_ORGANISATION_MODULABLE_V3 §5.1 / T-B10.
 *
 * <p><b>Garde d'accès (F10).</b> On lit l'appartenance via
 * {@code @authz.isTenantAdmin()} (jamais le claim {@code role} du JWT). La
 * suppression, destructive, est réservée au {@code @authz.isTenantOwner()}.
 */
@RestController
@RequestMapping("/api/v1/tenant/organization/levels")
@PreAuthorize("@authz.isTenantAdmin()")
public class OrganizationLevelController {

    private final OrganizationLevelService levelService;

    public OrganizationLevelController(OrganizationLevelService levelService) {
        this.levelService = levelService;
    }

    public record CreateLevelRequest(String name, String pluralName, String semanticType,
                                     Integer depthOrder, UUID parentLevelId,
                                     String icon, String color, Boolean branching) {
    }

    public record UpdateLevelRequest(String name, String pluralName, String description,
                                     String semanticType, Integer depthOrder,
                                     String icon, String color, Boolean branching, Boolean active) {
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(levelService.listAll().stream().map(this::toMap).toList());
    }

    /** Graine idempotente des niveaux par défaut, puis les renvoie. */
    @PostMapping("/ensure-defaults")
    public ResponseEntity<List<Map<String, Object>>> ensureDefaults() {
        return ResponseEntity.ok(levelService.ensureDefaults().stream().map(this::toMap).toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateLevelRequest req) {
        OrganizationLevel level = levelService.create(req.name(), req.pluralName(), req.semanticType(),
                req.depthOrder(), req.parentLevelId(), req.icon(), req.color(), req.branching());
        return ResponseEntity.status(201).body(toMap(level));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                     @RequestBody UpdateLevelRequest req) {
        OrganizationLevel level = levelService.update(id, req.name(), req.pluralName(), req.description(),
                req.semanticType(), req.depthOrder(), req.icon(), req.color(), req.branching(), req.active());
        return ResponseEntity.ok(toMap(level));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.isTenantOwner()")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        levelService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toMap(OrganizationLevel level) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", level.getId());
        body.put("rootTenantId", level.getRootTenantId());
        body.put("name", level.getName());
        body.put("pluralName", level.getPluralName());
        body.put("description", level.getDescription());
        body.put("depthOrder", level.getDepthOrder());
        body.put("semanticType", level.getSemanticType());
        body.put("parentLevelId", level.getParentLevelId());
        body.put("icon", level.getIcon());
        body.put("color", level.getColor());
        body.put("branching", level.getBranching());
        body.put("active", level.getActive());
        return body;
    }
}
