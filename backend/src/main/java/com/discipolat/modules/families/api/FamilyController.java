package com.discipolat.modules.families.api;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.enums.NiveauRisque;
import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.enums.StatutEntite;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRiskHistory;
import com.discipolat.modules.families.domain.FamilyRiskService;
import com.discipolat.modules.families.domain.FamilyService;
import com.discipolat.modules.transfers.api.TransferResponse;
import com.discipolat.modules.transfers.domain.TransferBridgeService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/families")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
public class FamilyController {

    private final FamilyService familyService;
    private final FamilyRiskService familyRiskService;
    private final SoulRepository soulRepository;
    private final TransferBridgeService transferBridgeService;

    public FamilyController(FamilyService familyService, FamilyRiskService familyRiskService,
                            SoulRepository soulRepository,
                            TransferBridgeService transferBridgeService) {
        this.familyService = familyService;
        this.familyRiskService = familyRiskService;
        this.soulRepository = soulRepository;
        this.transferBridgeService = transferBridgeService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<FamilyResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID chefFamilleId) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Family> families;
        if (chefFamilleId != null) {
            families = familyService.findByChefFamille(chefFamilleId, pageable);
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

    /**
     * Création d'une famille avec 2 cas :
     * Cas 1 : Sélectionner un chef existant (chefFamilleId requis)
     * Cas 2 : Créer immédiatement un nouveau chef (createNewChef = true + infos)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<FamilyResponse> create(@Valid @RequestBody CreateFamilyRequest request) {
        Family family = familyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(FamilyResponse.from(family));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<FamilyResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateFamilyRequest request) {
        Family family = familyService.update(id, request);
        return ResponseEntity.ok(FamilyResponse.from(family));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR') && @perm.has('FAMILY','DELETE')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        try {
            familyService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Family dissolved successfully"));
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * US-07 : changement du chef de famille.
     * Passe désormais par le MOTEUR DE WORKFLOW : demande soumise au circuit de
     * validation configuré par le pasteur (exécution immédiate si circuit vide).
     */
    @PatchMapping("/{id}/chief")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<TransferResponse> reassignChief(@PathVariable UUID id, @RequestBody ReassignChiefRequest request) {
        return ResponseEntity.ok(transferBridgeService.reassignChef(id, request.newChefId()));
    }

    @GetMapping("/by-chef/{chefId}")
    public ResponseEntity<List<FamilyResponse>> findByChef(@PathVariable UUID chefId) {
        List<Family> families = familyService.findByChefFamille(chefId);
        return ResponseEntity.ok(families.stream().map(FamilyResponse::from).toList());
    }

    // ======================== US-08: TREE VIEW ========================

    @GetMapping("/{id}/tree")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> getFamilyTree(@PathVariable UUID id) {
        return ResponseEntity.ok(familyService.getFamilyTree(id));
    }

    // ======================== FAISEUR PERFORMANCE ========================

    @GetMapping("/{id}/faiseur-performance")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<Map<String, Object>>> getFaiseurPerformance(
            @PathVariable UUID id,
            @RequestParam(required = false) String semaine) {
        LocalDate week = semaine != null ? LocalDate.parse(semaine) : LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        return ResponseEntity.ok(familyService.getFaiseurPerformance(id, week));
    }

    // ======================== US-10: FAMILY HISTORY ========================

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
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
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<List<com.discipolat.modules.families.domain.FamilyChiefHistory>> getChiefHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(familyService.getChiefHistory(id));
    }

    // ======================== US-60: RESTORE FAMILY ========================

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<FamilyResponse> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(FamilyResponse.from(familyService.restore(id)));
    }

    // ======================== FAMILLE À RISQUE ========================

    /** Indice de risque calculé automatiquement pour une famille. */
    @GetMapping("/{id}/risk")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<Map<String, Object>> getRiskAssessment(@PathVariable UUID id) {
        return ResponseEntity.ok(familyRiskService.getRiskAssessment(id));
    }

    /** Définition manuelle du niveau de risque par le pasteur. */
    @PutMapping("/{id}/risk-level")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<FamilyResponse> setNiveauRisque(
            @PathVariable UUID id,
            @RequestBody SetRiskLevelRequest request) {
        Family family = familyRiskService.setNiveauRisque(id, request.niveauRisque(), request.raison());
        return ResponseEntity.ok(FamilyResponse.from(family));
    }

    /** Historique des changements de niveau de risque. */
    @GetMapping("/{id}/risk-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<FamilyRiskHistory>> getRiskHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(familyRiskService.getRiskHistory(id));
    }

    public record SetRiskLevelRequest(NiveauRisque niveauRisque, String raison) {}
}
