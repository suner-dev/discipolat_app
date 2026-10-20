package com.discipolat.modules.families.api;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.enums.NiveauRisque;
import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.enums.StatutEntite;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyChiefHistory;
import com.discipolat.modules.families.domain.FamilyRiskHistory;
import com.discipolat.modules.families.domain.FamilyRiskService;
import com.discipolat.modules.families.domain.FamilyService;
import com.discipolat.modules.transfers.api.TransferResponse;
import com.discipolat.modules.transfers.domain.TransferBridgeService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
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
@PreAuthorize("@authz.can('FAMILIES_EXECUTE_R5A', 'TENANT', null)")
public class FamilyController {

    private final FamilyService familyService;
    private final FamilyRiskService familyRiskService;
    private final SoulRepository soulRepository;
    private final TransferBridgeService transferBridgeService;
    private final UserRepository userRepository;

    public FamilyController(FamilyService familyService, FamilyRiskService familyRiskService,
                            SoulRepository soulRepository,
                            TransferBridgeService transferBridgeService,
                            UserRepository userRepository) {
        this.familyService = familyService;
        this.familyRiskService = familyRiskService;
        this.soulRepository = soulRepository;
        this.transferBridgeService = transferBridgeService;
        this.userRepository = userRepository;
    }

    /** Résout les noms des chefs (id → prénom + nom) pour un affichage professionnel. */
    private String userName(UUID id) {
        return id == null ? null
                : userRepository.findById(id)
                        .map(u -> u.getFirstName() + " " + u.getLastName())
                        .orElse(null);
    }

    /** FamilyResponse avec les noms de chefs résolus (jamais d'UUID brut). */
    private FamilyResponse toResponse(Family family) {
        return FamilyResponse.from(family, userName(family.getChefFamilleId()),
                userName(family.getChefAdjointId()));
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
        Page<FamilyResponse> response = families.map(this::toResponse);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FamilyResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(familyService.findById(id)));
    }

    /**
     * Création d'une famille avec 2 cas :
     * Cas 1 : Sélectionner un chef existant (chefFamilleId requis)
     * Cas 2 : Créer immédiatement un nouveau chef (createNewChef = true + infos)
     */
    @PostMapping
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_CREATE', 'TENANT', null)")
    public ResponseEntity<FamilyResponse> create(@Valid @RequestBody CreateFamilyRequest request) {
        Family family = familyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(family));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_UPDATE', 'TENANT', null)")
    public ResponseEntity<FamilyResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateFamilyRequest request) {
        Family family = familyService.update(id, request);
        return ResponseEntity.ok(toResponse(family));
    }

    /**
     * Verrou canonique : la seule matrice lue par le moteur d'autorisation est
     * &#64;authz.can(...) (table permissions + surcouche locale par église). Le
     * conjonctif {@code && @perm.has('FAMILY','DELETE')} a été retiré : le bean
     * perm interrogeait {@code role_permissions(role, permission)}, forme
     * pré-V135 abandonnée par V135__create_multi_tenant_core_tables.sql au profit
     * de {@code role_permissions(role_id, permission_id)} ; il levait donc une
     * BadSqlGrammarException (500) pour tout rôle non ADMIN/PASTEUR ; il ne
     * pouvait rien paramétrer pour les autres.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_DELETE', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        familyService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Family dissolved successfully"));
    }

    /**
     * US-07 : changement du chef de famille.
     * Passe désormais par le MOTEUR DE WORKFLOW : demande soumise au circuit de
     * validation configuré par le pasteur (exécution immédiate si circuit vide).
     */
    @PatchMapping("/{id}/chief")
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_REASSIGNCHIEF', 'TENANT', null)")
    public ResponseEntity<TransferResponse> reassignChief(@PathVariable UUID id, @Valid @RequestBody ReassignChiefRequest request) {
        return ResponseEntity.ok(transferBridgeService.reassignChef(id, request.newChefId()));
    }

    @GetMapping("/by-chef/{chefId}")
    public ResponseEntity<List<FamilyResponse>> findByChef(@PathVariable UUID chefId) {
        List<Family> families = familyService.findByChefFamille(chefId);
        return ResponseEntity.ok(families.stream().map(this::toResponse).toList());
    }

    // ======================== US-08: TREE VIEW ========================

    @GetMapping("/{id}/tree")
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_GETFAMILYTREE', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getFamilyTree(@PathVariable UUID id) {
        return ResponseEntity.ok(familyService.getFamilyTree(id));
    }

    // ======================== FAISEUR PERFORMANCE ========================

    @GetMapping("/{id}/faiseur-performance")
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_GETFAISEURPERFORMANCE', 'TENANT', null)")
    public ResponseEntity<List<Map<String, Object>>> getFaiseurPerformance(
            @PathVariable UUID id,
            @RequestParam(required = false) String semaine) {
        LocalDate week = semaine != null ? LocalDate.parse(semaine) : LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        return ResponseEntity.ok(familyService.getFaiseurPerformance(id, week));
    }

    // ======================== US-10: FAMILY HISTORY ========================

    @GetMapping("/{id}/history")
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_GETFAMILYHISTORY', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getFamilyHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(familyService.getFamilyHistory(id));
    }

    // ======================== US-11: COMPARE FAMILIES ========================

    @PostMapping("/compare")
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_COMPAREFAMILIES', 'TENANT', null)")
    public ResponseEntity<List<Map<String, Object>>> compareFamilies(
            @RequestBody Map<String, List<UUID>> body) {
        List<UUID> familyIds = body.getOrDefault("familyIds", List.of());
        return ResponseEntity.ok(familyService.compareFamilies(familyIds));
    }

    // ======================== US-07: CHIEF HISTORY ========================

    /**
     * Historique des chefs : DTO professionnel (noms résolus, date lisible,
     * raison) — jamais d'UUID brut ni de date technique.
     */
    @GetMapping("/{id}/chief-history")
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_GETCHIEFHISTORY', 'TENANT', null)")
    public ResponseEntity<List<Map<String, Object>>> getChiefHistory(@PathVariable UUID id) {
        List<Map<String, Object>> history = new java.util.ArrayList<>();
        for (FamilyChiefHistory change : familyService.getChiefHistory(id)) {
            Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("id", change.getId());
            entry.put("ancienChefId", change.getAncienChefId());
            entry.put("ancienChefNom", change.getAncienChefId() == null ? null
                    : userRepository.findById(change.getAncienChefId())
                            .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null));
            entry.put("nouveauChefId", change.getNouveauChefId());
            entry.put("nouveauChefNom", userRepository.findById(change.getNouveauChefId())
                    .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null));
            entry.put("dateChangement", change.getCreatedAt());
            entry.put("raison", change.getRaison());
            history.add(entry);
        }
        return ResponseEntity.ok(history);
    }

    // ======================== US-60: RESTORE FAMILY ========================

    @PatchMapping("/{id}/restore")
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_RESTORE', 'TENANT', null)")
    public ResponseEntity<FamilyResponse> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(familyService.restore(id)));
    }

    // ======================== FAMILLE À RISQUE ========================

    /** Indice de risque calculé automatiquement pour une famille. */
    @GetMapping("/{id}/risk")
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_GETRISKASSESSMENT', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getRiskAssessment(@PathVariable UUID id) {
        return ResponseEntity.ok(familyRiskService.getRiskAssessment(id));
    }

    /** Définition manuelle du niveau de risque par le pasteur. */
    @PutMapping("/{id}/risk-level")
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_SETNIVEAURISQUE', 'TENANT', null)")
    public ResponseEntity<FamilyResponse> setNiveauRisque(
            @PathVariable UUID id,
            @Valid @RequestBody SetRiskLevelRequest request) {
        Family family = familyRiskService.setNiveauRisque(id, request.niveauRisque(), request.raison());
        return ResponseEntity.ok(toResponse(family));
    }

    /** Historique des changements de niveau de risque. */
    @GetMapping("/{id}/risk-history")
    @PreAuthorize("@authz.can('FAMILIES_EXECUTE_GETRISKHISTORY', 'TENANT', null)")
    public ResponseEntity<List<FamilyRiskHistory>> getRiskHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(familyRiskService.getRiskHistory(id));
    }

    public record SetRiskLevelRequest(NiveauRisque niveauRisque, String raison) {}
}
