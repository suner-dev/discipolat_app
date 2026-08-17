package com.discipolat.modules.evaluations.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.evaluations.domain.CategorieEvaluation;
import com.discipolat.modules.evaluations.domain.Evaluation;
import com.discipolat.modules.evaluations.domain.EvaluationService;
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
@RequestMapping("/api/v1/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * Submit an anonymous evaluation.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        UUID evalueId = UUID.fromString((String) body.get("evalueId"));
        CategorieEvaluation categorie = CategorieEvaluation.valueOf((String) body.get("categorie"));
        int note = (Integer) body.get("note");
        String commentaire = (String) body.getOrDefault("commentaire", null);

        Evaluation evaluation = evaluationService.submit(evalueId, categorie, note, commentaire);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", evaluation.getId(),
                "message", "Évaluation soumise avec succès (anonyme)"
        ));
    }

    /**
     * Donne une évaluation si l'utilisateur n'en a pas encore, ou modifie
     * la sienne s'il en a déjà une (upsert sur évaluateur + évalué + catégorie).
     * La catégorie est optionnelle : dérivée du rôle de l'évalué si absente.
     */
    @PutMapping("/{evalueId}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> submitOrUpdate(@PathVariable UUID evalueId,
                                                              @RequestBody Map<String, Object> body) {
        CategorieEvaluation categorie = body.get("categorie") != null
                ? CategorieEvaluation.valueOf((String) body.get("categorie"))
                : null;
        int note = (Integer) body.get("note");
        String commentaire = (String) body.getOrDefault("commentaire", null);

        Evaluation evaluation = evaluationService.submitOrUpdate(evalueId, categorie, note, commentaire);
        return ResponseEntity.ok(Map.of(
                "id", evaluation.getId(),
                "categorie", evaluation.getCategorie().name(),
                "note", evaluation.getNote(),
                "message", "Évaluation enregistrée"
        ));
    }

    /**
     * MES évaluations d'un utilisateur donné — pré-remplit le formulaire
     * « donner / modifier » de la fiche utilisateur.
     */
    @GetMapping("/my/{evalueId}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<Map<String, Object>>> getMyEvaluationsFor(@PathVariable UUID evalueId) {
        return ResponseEntity.ok(evaluationService.getMyEvaluationsFor(evalueId));
    }

    /**
     * Get my evaluations aggregated stats (what others think of me — anonymous).
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> getMyEvaluations() {
        return ResponseEntity.ok(evaluationService.getMyEvaluations());
    }

    /**
     * Get my evaluations as a paginated list (anonymous results, no evaluateur_id).
     */
    @GetMapping("/me/list")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<Map<String, Object>>> getMyEvaluationsList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String categorie) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Evaluation> evals = evaluationService.getMyEvaluationsList(categorie, pageable);
        Page<Map<String, Object>> response = evals.map(e -> Map.<String, Object>of(
                "id", e.getId(),
                "categorie", e.getCategorie().name(),
                "note", e.getNote(),
                "commentaire", e.getCommentaire() != null ? e.getCommentaire() : "",
                "date", e.getCreatedAt().toString()
        ));
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    /**
     * Get people I can evaluate.
     */
    @GetMapping("/to-evaluate")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<Map<String, Object>>> getPeopleToEvaluate() {
        return ResponseEntity.ok(evaluationService.getPeopleToEvaluate());
    }

    /**
     * Get evaluations for a specific user (encadrement : le service vérifie
     * que l'utilisateur courant est autorisé à évaluer/voir cette personne).
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> getEvaluationsForUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(evaluationService.getEvaluationsForUser(userId));
    }

    /**
     * Get all evaluations aggregated (Pasteur overview).
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Map<String, Object>> getAllEvaluations() {
        return ResponseEntity.ok(evaluationService.getAllEvaluationsAggregated());
    }
}
