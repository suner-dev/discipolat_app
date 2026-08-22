package com.discipolat.modules.facerec.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reconnaissance faciale — enrôlement, identification et statistiques.
 *
 * L'empreinte du portrait probe est comparée à tous les gabarits actifs du
 * tenant ; le meilleur candidat sous {@link FaceHasher#MATCH_THRESHOLD} est
 * retourné avec son score de confiance (1 - distance/256).
 */
@Service
public class FaceRecognitionService {

    private final FaceTemplateRepository repository;
    private final SecurityUtils securityUtils;

    public FaceRecognitionService(FaceTemplateRepository repository,
                                  SecurityUtils securityUtils) {
        this.repository = repository;
        this.securityUtils = securityUtils;
    }

    /**
     * Enrôle (ou met à jour) le gabarit facial d'un utilisateur.
     * Si {@code userId} est absent, l'utilisateur courant est utilisé
     * (auto-enrôlement depuis le mobile).
     */
    @Transactional
    public FaceTemplate enroll(UUID userId, UUID soulId, String displayName, byte[] image) throws IOException {
        FaceHasher.FaceDescriptor descriptor = FaceHasher.hash(image);
        if (descriptor.qualityScore() < 0.25) {
            throw new IllegalArgumentException(
                    "Photo inexploitable (contraste insuffisant) — reprenez la photo dans un cadre éclairé");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Le nom d'affichage est obligatoire");
        }
        UUID effectiveUserId = userId != null ? userId : securityUtils.getCurrentUserId();

        FaceTemplate template = repository
                .findByTenantIdAndUserId(securityUtils.getCurrentTenantId(), effectiveUserId)
                .orElseGet(() -> FaceTemplate.builder().build());
        template.setTenantId(securityUtils.getCurrentTenantId());
        template.setUserId(effectiveUserId);
        template.setSoulId(soulId);
        template.setDisplayName(displayName.trim());
        template.setDescriptorHash(descriptor.descriptorHash());
        template.setQualityScore(descriptor.qualityScore());
        template.setActive(true);
        return repository.save(template);
    }

    /**
     * Identifie un visage parmi les gabarits actifs du tenant.
     *
     * @return le meilleur match + confiance (0–1), ou un résultat non-matché
     *         avec la meilleure distance trouvée pour diagnostic.
     */
    @Transactional(readOnly = true)
    public IdentificationResult identify(byte[] image) throws IOException {
        FaceHasher.FaceDescriptor probe = FaceHasher.hash(image);
        List<FaceTemplate> candidates =
                repository.findByTenantIdAndActiveTrueOrderByCreatedAtDesc(securityUtils.getCurrentTenantId());

        FaceTemplate best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (FaceTemplate t : candidates) {
            int d = FaceHasher.hammingDistance(probe.descriptorHash(), t.getDescriptorHash());
            if (d < bestDistance) {
                bestDistance = d;
                best = t;
            }
        }

        if (best == null || bestDistance > FaceHasher.MATCH_THRESHOLD) {
            return new IdentificationResult(null,
                    best == null ? 1.0 : Math.min(1.0, (double) bestDistance / 256),
                    false,
                    best == null ? "Aucun gabarit enrôlé" : "Aucune correspondance (meilleure distance : "
                            + bestDistance + ")");
        }
        double confidence = 1.0 - (double) bestDistance / 256;
        return new IdentificationResult(best, confidence, true,
                "Reconnu : " + best.getDisplayName());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        long active = repository.countByTenantIdAndActiveTrue(securityUtils.getCurrentTenantId());
        return Map.of("enrolledFaces", active);
    }

    @Transactional(readOnly = true)
    public List<FaceTemplate> list(String query) {
        return (query == null || query.isBlank())
                ? repository.findByTenantIdAndActiveTrueOrderByCreatedAtDesc(securityUtils.getCurrentTenantId())
                : repository.searchByDisplayName(securityUtils.getCurrentTenantId(), query.trim());
    }

    /** Désactive (effacement RGPD) le gabarit facial d'un utilisateur. */
    @Transactional
    public void deactivate(UUID templateId) {
        FaceTemplate template = repository.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("FaceTemplate", templateId));
        template.setActive(false);
        repository.save(template);
    }

    /** Meilleur candidat trié par distance — utilitaire de tests. */
    static FaceTemplate closest(List<FaceTemplate> templates, String hash) {
        return templates.stream()
                .min(Comparator.comparingInt(t -> FaceHasher.hammingDistance(hash, t.getDescriptorHash())))
                .orElse(null);
    }

    /** Résultat d'identification : gabarit trouvé (ou null), confiance, verdict, message. */
    public record IdentificationResult(FaceTemplate template, double confidence,
                                       boolean matched, String message) {
    }
}
