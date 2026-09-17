package com.discipolat.modules.people.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.people.domain.EventAssignment;
import com.discipolat.modules.people.domain.Membership;
import com.discipolat.modules.people.domain.Person;
import com.discipolat.modules.people.domain.RoleAssignment;
import com.discipolat.modules.people.domain.SpaceMembership;
import com.discipolat.modules.people.service.PeopleService;
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
@RequestMapping("/api/v1/people")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'DEPARTMENT_LEADER', 'FAMILY_LEADER')")
public class PeopleController {

    private final PeopleService peopleService;

    public PeopleController(PeopleService peopleService) {
        this.peopleService = peopleService;
    }

    // ========== G3.1 : PERSON / REGISTRY ==========

    @GetMapping
    public ResponseEntity<PageResponse<Person>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID campusId,
            @RequestParam(defaultValue = "false") boolean withoutSpace,
            @RequestParam(defaultValue = "false") boolean withoutFamily,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("lastName", "firstName"));
        Page<Person> result = peopleService.searchPeople(tenantId, search, status, campusId, withoutSpace, withoutFamily, pageable);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/without-space")
    public ResponseEntity<List<Person>> getPeopleWithoutSpace() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(peopleService.getPeopleWithoutSpace(tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Person> getPerson(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(peopleService.getPerson(tenantId, id));
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<?> registerPerson(@RequestBody Person person, @RequestParam(required = false) String source) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        try {
            Person saved = peopleService.registerPerson(tenantId, person, source, actorId);
            return ResponseEntity.ok(saved);
        } catch (PeopleService.PersonAlreadyExistsException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "DUPLICATE_PERSON",
                    "message", e.getMessage(),
                    "existingPerson", e.getExistingPerson()
            ));
        }
    }

    @PostMapping("/{masterId}/merge/{duplicateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Person> mergePersons(@PathVariable UUID masterId, @PathVariable UUID duplicateId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(peopleService.mergePersons(tenantId, masterId, duplicateId, actorId));
    }

    // ========== G3.2 : SPACE MEMBERSHIP ==========

    @GetMapping("/{personId}/spaces")
    public ResponseEntity<List<SpaceMembership>> getPersonSpaces(@PathVariable UUID personId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(peopleService.getPersonSpaces(tenantId, personId));
    }

    @PostMapping("/{personId}/spaces")
    public ResponseEntity<SpaceMembership> assignToSpace(
            @PathVariable UUID personId,
            @RequestParam UUID spaceId,
            @RequestParam(required = false) String membershipType,
            @RequestParam(required = false) String responsibility) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(peopleService.assignToSpace(tenantId, actorId, personId, spaceId, membershipType, responsibility));
    }

    @DeleteMapping("/{personId}/spaces/{spaceId}")
    public ResponseEntity<Void> removeFromSpace(@PathVariable UUID personId, @PathVariable UUID spaceId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        peopleService.removeFromSpace(tenantId, actorId, personId, spaceId);
        return ResponseEntity.noContent().build();
    }

    // ========== G3.2 : ROLE ASSIGNMENT ==========

    @GetMapping("/{personId}/roles")
    public ResponseEntity<List<RoleAssignment>> getPersonRoles(@PathVariable UUID personId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(peopleService.getPersonRoles(tenantId, personId));
    }

    @PostMapping("/{personId}/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<RoleAssignment> assignRole(
            @PathVariable UUID personId,
            @RequestParam UUID roleId,
            @RequestParam(required = false) UUID orgUnitId,
            @RequestParam(required = false) UUID spaceId,
            @RequestParam(required = false) String reason) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(peopleService.assignRole(tenantId, actorId, personId, roleId, orgUnitId, spaceId, reason));
    }

    @DeleteMapping("/roles/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> endRole(@PathVariable UUID assignmentId, @RequestParam(required = false) String reason) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        peopleService.endRole(tenantId, actorId, assignmentId, reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{personId}/transfer")
    @PreAuthorize("hasRole('PASTOR_PRINCIPAL')")
    public ResponseEntity<RoleAssignment> transferPastor(
            @PathVariable UUID personId,
            @RequestParam UUID newOrgUnitId,
            @RequestParam String reason) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(peopleService.transferPastor(tenantId, actorId, personId, newOrgUnitId, reason));
    }

    // ========== EVENT ASSIGNMENT ==========

    @GetMapping("/{personId}/event-assignments")
    public ResponseEntity<List<EventAssignment>> getPersonEventAssignments(@PathVariable UUID personId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(peopleService.getPersonEventAssignments(tenantId, personId));
    }
}