package com.discipolat.modules.aiPredictions.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.aiPredictions.domain.AiPrediction;
import com.discipolat.modules.aiPredictions.domain.AiPredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Prédictions IA — accessibles à TOUS les rôles authentifiés.
 *
 * <p>Le tenant courant est résolu côté serveur via {@link SecurityUtils} :
 * aucun identifiant de tenant n'est accepté depuis le client (anti-IDOR /
 * isolation multi-tenant).</p>
 */
@RestController
@RequestMapping({"/api/v1/ai-predictions", "/api/ai-predictions"})
@PreAuthorize("isAuthenticated()")
public class AiPredictionController {

    private final AiPredictionService service;
    private final SecurityUtils securityUtils;

    public AiPredictionController(AiPredictionService service, SecurityUtils securityUtils) {
        this.service = service;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<List<AiPrediction>> list() {
        return ResponseEntity.ok(service.listByTenant(securityUtils.getCurrentTenantId()));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<AiPrediction>> listByType(
            @PathVariable AiPrediction.PredictionType type) {
        return ResponseEntity.ok(service.listByType(securityUtils.getCurrentTenantId(), type));
    }

    @GetMapping("/risks")
    public ResponseEntity<List<AiPrediction>> listRisks() {
        return ResponseEntity.ok(service.listRisks(securityUtils.getCurrentTenantId()));
    }

    @PostMapping("/generate")
    public ResponseEntity<List<AiPrediction>> generate() {
        return ResponseEntity.ok(service.generatePredictions(securityUtils.getCurrentTenantId()));
    }

    @PostMapping
    public ResponseEntity<AiPrediction> save(@RequestBody AiPrediction prediction) {
        prediction.setTenantId(securityUtils.getCurrentTenantId());
        return ResponseEntity.ok(service.save(prediction));
    }
}