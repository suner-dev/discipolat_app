package com.discipolat.modules.platform.domain;

import com.discipolat.common.exception.BadRequestException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.platform.api.CreateFeedbackRequest;
import com.discipolat.modules.platform.api.FeedbackResponse;
import com.discipolat.modules.platform.api.FeedbackStatsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Collecte et suivi des retours des testeurs (bêta-testing).
 * - Soumission authentifiée (widget intégré à l'application)
 * - Consultation & changement de statut réservés à l'administration
 * - Résolution de l'email émetteur via la table users (lecture seule,
 *   sans couplage de module — JdbcTemplate)
 */
@Service
@Transactional
public class FeedbackService {

    private static final Set<String> VALID_STATUS = Set.of("NOUVEAU", "EN_COURS", "RESOLU", "REJETE");
    private static final Set<String> VALID_PRIORITY = Set.of("BASSE", "MOYENNE", "HAUTE", "CRITIQUE");
    private static final Set<String> VALID_CATEGORY = Set.of(
            "BUG", "UX", "SUGGESTION", "FONCTIONNALITE_MANQUANTE",
            "PERFORMANCE", "TRADUCTION", "AFFICHAGE", "AUTRE");

    private final FeedbackRepository repository;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    public FeedbackService(FeedbackRepository repository, AuditService auditService,
                            EntityPropagationPublisher propagationPublisher, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Soumission d'un retour par un utilisateur authentifié. */
    public FeedbackResponse create(UUID userId, CreateFeedbackRequest req) {
        if (!VALID_CATEGORY.contains(req.category())) {
            throw new BadRequestException("Catégorie invalide : " + req.category());
        }
        String priority = req.priority() != null && !req.priority().isBlank() ? req.priority() : "MOYENNE";
        if (!VALID_PRIORITY.contains(priority)) {
            throw new BadRequestException("Priorité invalide : " + priority);
        }
        Feedback feedback = Feedback.builder()
                .category(req.category())
                .priority(priority)
                .subject(req.subject().trim())
                .description(req.description())
                .pageUrl(req.pageUrl())
                .userAgent(req.userAgent())
                .browser(req.browser())
                .device(req.device())
                .os(req.os())
                .appVersion(appVersion)
                .status("NOUVEAU")
                .createdBy(userId)
                .build();
        repository.save(feedback);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishCreated("FEEDBACK", feedback.getId(),
                Map.of("category", feedback.getCategory(), "priority", feedback.getPriority(),
                        "subject", feedback.getSubject()),
                "Retour créé: " + feedback.getSubject());
        return FeedbackResponse.from(feedback, resolveEmail(userId));
    }

    /** Liste complète (les plus récents d'abord) — usage admin. */
    @Transactional(readOnly = true)
    public List<FeedbackResponse> listAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(f -> FeedbackResponse.from(f, resolveEmail(f.getCreatedBy())))
                .toList();
    }

    /** Statistiques agrégées — usage admin. */
    @Transactional(readOnly = true)
    public FeedbackStatsResponse stats() {
        long total = repository.count();
        long nouveaux = repository.countByStatus("NOUVEAU");
        long enCours = repository.countByStatus("EN_COURS");
        long resolus = repository.countByStatus("RESOLU");
        long rejetes = repository.countByStatus("REJETE");

        Map<String, Long> parCategorie = new LinkedHashMap<>();
        repository.countByCategory().forEach(row -> {
            String cat = String.valueOf(row.get("category"));
            long count = ((Number) row.get("count")).longValue();
            parCategorie.put(cat, count);
        });
        return new FeedbackStatsResponse(total, nouveaux, enCours, resolus, rejetes, parCategorie);
    }

    /** Changement de statut par l'administration. */
    public FeedbackResponse updateStatus(UUID id, String status) {
        if (!VALID_STATUS.contains(status)) {
            throw new BadRequestException("Statut invalide : " + status);
        }
        Feedback feedback = repository.findById(id)
                .orElseThrow(() -> new com.discipolat.common.exception.ResourceNotFoundException("Feedback", "id", id));
        String oldStatus = feedback.getStatus();
        feedback.setStatus(status);
        repository.save(feedback);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishStatusChanged("FEEDBACK", id,
                oldStatus, status,
                "Retour mis à jour: " + feedback.getSubject());
        return FeedbackResponse.from(feedback, resolveEmail(feedback.getCreatedBy()));
    }

    private String resolveEmail(UUID userId) {
        if (userId == null) return null;
        try {
            List<String> emails = jdbcTemplate.queryForList(
                    "SELECT email FROM users WHERE id = ?", String.class, userId);
            return emails.isEmpty() ? null : emails.get(0);
        } catch (Exception e) {
            return null;
        }
    }
}
