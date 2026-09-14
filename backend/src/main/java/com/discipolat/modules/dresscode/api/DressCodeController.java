package com.discipolat.modules.dresscode.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.dresscode.domain.*;
import com.discipolat.modules.dresscode.api.DressCodeRequests.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dress Code Controller (G3.4 — exigence utilisateur)
 * Gestion des dress codes par espace/événement avec règles par groupe.
 */
@RestController
@RequestMapping("/api/v1/dress-codes")
public class DressCodeController {

    private final DressCodeService dressCodeService;

    public DressCodeController(DressCodeService dressCodeService) {
        this.dressCodeService = dressCodeService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DressCodeResponse>> list(
            @RequestParam(required = false) UUID spaceId,
            @RequestParam(required = false) UUID eventId) {
        UUID tenantId = TenantContext.requireTenantId();
        List<DressCode> dressCodes = dressCodeService.findAll(tenantId, spaceId, eventId);
        return ResponseEntity.ok(dressCodes.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DressCodeDetailResponse> getById(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        DressCode dc = dressCodeService.findById(tenantId, id);
        List<DressCodeRule> rules = dressCodeService.getRules(tenantId, id);
        return ResponseEntity.ok(toDetailResponse(dc, rules));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<DressCodeDetailResponse> create(@RequestBody DressCodeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        DressCode dc = new DressCode();
        dc.setTitle(request.title());
        dc.setServiceName(request.serviceName());
        dc.setSpaceId(request.spaceId());
        dc.setEventId(request.eventId());
        dc.setBeginsAt(request.beginsAt());
        dc.setEndsAt(request.endsAt());
        dc.setStatus(request.status() != null ? request.status() : "DRAFT");

        List<DressCodeRule> rules = request.rules() != null ? request.rules().stream().map(r -> {
            DressCodeRule rule = new DressCodeRule();
            rule.setGroupName(r.groupName());
            rule.setDescription(r.description());
            rule.setImageUrl(r.imageUrl());
            return rule;
        }).toList() : null;

        DressCode saved = dressCodeService.create(tenantId, userId, dc, rules);
        List<DressCodeRule> savedRules = dressCodeService.getRules(tenantId, saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDetailResponse(saved, savedRules));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<DressCodeDetailResponse> update(@PathVariable UUID id, @RequestBody DressCodeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        DressCode dc = new DressCode();
        dc.setTitle(request.title());
        dc.setServiceName(request.serviceName());
        dc.setSpaceId(request.spaceId());
        dc.setEventId(request.eventId());
        dc.setBeginsAt(request.beginsAt());
        dc.setEndsAt(request.endsAt());
        dc.setStatus(request.status());

        List<DressCodeRule> rules = request.rules() != null ? request.rules().stream().map(r -> {
            DressCodeRule rule = new DressCodeRule();
            rule.setGroupName(r.groupName());
            rule.setDescription(r.description());
            rule.setImageUrl(r.imageUrl());
            return rule;
        }).toList() : null;

        DressCode updated = dressCodeService.update(tenantId, id, dc, rules);
        List<DressCodeRule> savedRules = dressCodeService.getRules(tenantId, updated.getId());
        return ResponseEntity.ok(toDetailResponse(updated, savedRules));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        dressCodeService.archive(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        dressCodeService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    private DressCodeResponse toResponse(DressCode dc) {
        return new DressCodeResponse(
                dc.getId(), dc.getSpaceId(), dc.getEventId(), dc.getServiceName(),
                dc.getTitle(), dc.getBeginsAt(), dc.getEndsAt(), dc.getStatus(), dc.isArchived()
        );
    }

    private DressCodeDetailResponse toDetailResponse(DressCode dc, List<DressCodeRule> rules) {
        return new DressCodeDetailResponse(
                dc.getId(), dc.getSpaceId(), dc.getEventId(), dc.getServiceName(),
                dc.getTitle(), dc.getBeginsAt(), dc.getEndsAt(), dc.getStatus(), dc.isArchived(),
                dc.getCreatedAt(), dc.getUpdatedAt(),
                rules.stream().map(r -> new DressCodeRuleResponse(r.getId(), r.getGroupName(), r.getDescription(), r.getImageUrl())).toList()
        );
    }
}
