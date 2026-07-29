package com.discipolat.modules.families.api;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.enums.StatutEntite;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/families")
public class FamilyController {

    private final FamilyService familyService;
    private final SoulRepository soulRepository;

    public FamilyController(FamilyService familyService, SoulRepository soulRepository) {
        this.familyService = familyService;
        this.soulRepository = soulRepository;
    }

    @GetMapping
    public ResponseEntity<PageResponse<FamilyResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID departementId) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Family> families;
        if (departementId != null) {
            families = familyService.findByDepartement(departementId, pageable);
        } else {
            families = familyService.findAll(pageable);
        }
        Page<FamilyResponse> response = families.map(FamilyResponse::from);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FamilyResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(FamilyResponse.from(familyService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<FamilyResponse> create(@Valid @RequestBody CreateFamilyRequest request) {
        Family family = Family.builder()
                .nom(request.nom())
                .departementId(request.departementId())
                .chefFamilleId(request.chefFamilleId())
                .build();
        family = familyService.create(family);
        return ResponseEntity.status(HttpStatus.CREATED).body(FamilyResponse.from(family));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<FamilyResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateFamilyRequest request) {
        Family family = familyService.findById(id);
        family.setNom(request.nom());
        family.setChefFamilleId(request.chefFamilleId());
        family = familyService.update(family);
        return ResponseEntity.ok(FamilyResponse.from(family));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        try {
            familyService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Family dissolved successfully"));
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/chief")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<FamilyResponse> reassignChief(@PathVariable UUID id, @RequestBody ReassignChiefRequest request) {
        familyService.reassignChef(id, request.newChefId());
        return ResponseEntity.ok(FamilyResponse.from(familyService.findById(id)));
    }

    @GetMapping("/by-departement/{departementId}")
    public ResponseEntity<List<FamilyResponse>> findByDepartement(@PathVariable UUID departementId) {
        List<Family> families = familyService.findByDepartement(departementId, Pageable.unpaged()).getContent();
        return ResponseEntity.ok(families.stream().map(FamilyResponse::from).toList());
    }

    @GetMapping("/by-chef/{chefId}")
    public ResponseEntity<List<FamilyResponse>> findByChef(@PathVariable UUID chefId) {
        List<Family> families = familyService.findByChefFamille(chefId);
        return ResponseEntity.ok(families.stream().map(FamilyResponse::from).toList());
    }

    // ======================== US-08: TREE VIEW ========================

    @GetMapping("/{id}/tree")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> getFamilyTree(@PathVariable UUID id) {
        return ResponseEntity.ok(familyService.getFamilyTree(id));
    }

    // ======================== US-10: FAMILY HISTORY ========================

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> getFamilyHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(familyService.getFamilyHistory(id));
    }

    // ======================== US-11: COMPARE FAMILIES ========================

    @PostMapping("/compare")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<List<Map<String, Object>>> compareFamilies(
            @RequestBody Map<String, List<UUID>> body) {
        List<UUID> familyIds = body.getOrDefault("familyIds", List.of());
        return ResponseEntity.ok(familyService.compareFamilies(familyIds));
    }

    // ======================== US-07: CHIEF HISTORY ========================

    @GetMapping("/{id}/chief-history")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<com.discipolat.modules.families.domain.FamilyChiefHistory>> getChiefHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(familyService.getChiefHistory(id));
    }

    // ======================== US-60: RESTORE FAMILY ========================

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<FamilyResponse> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(FamilyResponse.from(familyService.restore(id)));
    }
}
