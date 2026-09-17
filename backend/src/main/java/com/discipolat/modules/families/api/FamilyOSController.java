package com.discipolat.modules.families.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.families.domain.*;
import com.discipolat.modules.families.service.FamilyOSService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/families/{familyId}/os")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR')")
public class FamilyOSController {

    private final FamilyOSService familyOSService;

    public FamilyOSController(FamilyOSService familyOSService) {
        this.familyOSService = familyOSService;
    }

    // ========== DASHBOARD ==========

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@PathVariable UUID familyId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(familyOSService.getFamilyDashboard(tenantId, familyId));
    }

    // ========== VISITS ==========

    @GetMapping("/visits")
    public ResponseEntity<List<FamilyVisit>> getVisits(
            @PathVariable UUID familyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(familyOSService.getFamilyVisits(tenantId, familyId, from, to));
    }

    @PostMapping("/visits")
    public ResponseEntity<FamilyVisit> createVisit(@PathVariable UUID familyId, @RequestBody FamilyVisit visit) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        visit.setFamilyId(familyId);
        return ResponseEntity.ok(familyOSService.createVisit(tenantId, actorId, visit));
    }

    @PutMapping("/visits/{visitId}")
    public ResponseEntity<FamilyVisit> updateVisit(@PathVariable UUID familyId, @PathVariable UUID visitId, @RequestBody FamilyVisit visit) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(familyOSService.updateVisit(tenantId, actorId, visitId, visit));
    }

    // ========== RECEPTIONS ==========

    @GetMapping("/receptions")
    public ResponseEntity<List<FamilyReception>> getReceptions(
            @PathVariable UUID familyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(familyOSService.getFamilyReceptions(tenantId, familyId, from, to));
    }

    @PostMapping("/receptions")
    public ResponseEntity<FamilyReception> createReception(@PathVariable UUID familyId, @RequestBody FamilyReception reception) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        reception.setFamilyId(familyId);
        return ResponseEntity.ok(familyOSService.createReception(tenantId, actorId, reception));
    }

    // ========== MEETINGS ==========

    @GetMapping("/meetings")
    public ResponseEntity<List<FamilyMeeting>> getMeetings(
            @PathVariable UUID familyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(familyOSService.getFamilyMeetings(tenantId, familyId, from, to));
    }

    @PostMapping("/meetings")
    public ResponseEntity<FamilyMeeting> createMeeting(@PathVariable UUID familyId, @RequestBody FamilyMeeting meeting) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        meeting.setFamilyId(familyId);
        return ResponseEntity.ok(familyOSService.createMeeting(tenantId, actorId, meeting));
    }

    // ========== UNIFIED ACTIVITIES ==========

    @GetMapping("/activities")
    public ResponseEntity<PageResponse<FamilyActivity>> getActivities(
            @PathVariable UUID familyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID tenantId = TenantContext.requireTenantId();
        if (from != null && to != null) {
            List<FamilyActivity> activities = familyOSService.getFamilyActivitiesByDateRange(tenantId, familyId, from, to);
            return ResponseEntity.ok(PageResponse.of(activities, page, size, activities.size(), 1));
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("activityDate").descending());
        Page<FamilyActivity> result = familyOSService.getFamilyActivities(tenantId, familyId, page, size);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    // ========== G4.2: SEARCH & ADD MEMBERS ==========

    @GetMapping("/search-souls")
    public ResponseEntity<List<com.discipolat.modules.users.domain.User>> searchSouls(
            @PathVariable UUID familyId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "CHURCH") String scope) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(familyOSService.searchSoulsForFamily(tenantId, familyId, search, scope));
    }

    @PostMapping("/members")
    public ResponseEntity<com.discipolat.modules.families.domain.Family> addMember(
            @PathVariable UUID familyId,
            @RequestParam UUID soulId,
            @RequestParam(required = false) UUID faiseurId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(familyOSService.addSoulToFamily(tenantId, actorId, familyId, soulId, faiseurId));
    }
}