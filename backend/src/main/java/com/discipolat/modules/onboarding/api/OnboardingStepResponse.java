package com.discipolat.modules.onboarding.api;

import com.discipolat.modules.onboarding.domain.OnboardingStepDefinition;
import com.discipolat.modules.onboarding.domain.OnboardingWizardStep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrat figé §3.1 — représentation d'une étape du wizard d'onboarding.
 *
 * <p><b>Les noms de champs sont figés par le contrat et ne doivent jamais être
 * renommés</b> : le web (tâche B1) et le mobile (tâche B7) codent contre eux.
 * L'ordre ci-dessous est celui du contrat.
 *
 * <p>Points de conception :
 * <ul>
 *   <li>l'<b>entité n'est jamais renvoyée</b> (elle exposait `config` et
 *       `completedData` sous forme de chaîne JSON brute) ;</li>
 *   <li>`completedData` est renvoyé comme <b>objet JSON</b> désérialisé, ou
 *       {@code null} si la chaîne stockée est absente ou illisible ;</li>
 *   <li>`isCompleted` = {@code status == COMPLETED || status == SKIPPED} — les
 *       clients-existants utilisent déjà ce champ ;</li>
 *   <li>`title` / `description` proviennent de {@link OnboardingStepDefinition}
 *       (français, décision D5) — le client peut les remplacer par ses clés i18n ;</li>
 *   <li>les dates sont sérialisées en <b>ISO-8601 UTC</b> ; l'entité les stocke
 *       en {@code LocalDateTime}, la conversion est donc explicite et UTC.</li>
 * </ul>
 */
public record OnboardingStepResponse(
        UUID id,
        String stepType,
        Integer stepOrder,
        String title,
        String description,
        String status,
        boolean isCompleted,
        boolean isSkippable,
        boolean skipRequiresReason,
        Instant startedAt,
        Instant completedAt,
        Map<String, Object> completedData) {

    /** Clé interne du motif de saut, exposée à titre de traçabilité dans `completedData`. */
    public static final String SKIP_REASON_KEY = "skipReason";

    /**
     * Convertit une entité en réponse de contrat.
     *
     * @param entity     étape persistée
     * @param definition définition canonique (ordre, titres, skippabilité)
     * @param objectMapper désérialiseur de {@code completedData}
     */
    public static OnboardingStepResponse from(
            OnboardingWizardStep entity,
            OnboardingStepDefinition definition,
            ObjectMapper objectMapper) {

        OnboardingWizardStep.Status status = entity.getStatus();
        boolean completed = status == OnboardingWizardStep.Status.COMPLETED
                || status == OnboardingWizardStep.Status.SKIPPED;

        return new OnboardingStepResponse(
                entity.getId(),
                entity.getStepType().name(),
                entity.getStepOrder(),
                definition.title(),
                definition.description(),
                status.name(),
                completed,
                definition.isSkippable(),
                definition.skipRequiresReason(),
                toUtc(entity.getStartedAt()),
                toUtc(entity.getCompletedAt()),
                deserializeCompletedData(entity, definition, objectMapper));
    }

    private static Instant toUtc(java.time.LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    /**
     * Le `skipReason` n'est pas un champ du contrat §3.1 : il est fusionné dans
     * `completedData` (qui est un objet libre) pour que le client puisse expliquer
     * à l'utilisateur pourquoi une étape a été sautée, sans ajouter de champ.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> deserializeCompletedData(
            OnboardingWizardStep entity,
            OnboardingStepDefinition definition,
            ObjectMapper objectMapper) {

        Map<String, Object> data = readJson(entity.getCompletedData(), objectMapper);
        String skipReason = entity.getSkipReason();
        if (skipReason != null && !skipReason.isBlank()) {
            if (data == null) {
                data = new java.util.LinkedHashMap<>();
            }
            data.put(SKIP_REASON_KEY, skipReason);
        }
        return data;
    }

    private static Map<String, Object> readJson(String raw, ObjectMapper objectMapper) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node == null || node.isNull() || !node.isObject()) {
                return null;
            }
            return objectMapper.convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
        } catch (Exception malformed) {
            // Une donnée historique illisible ne doit pas faire tomber le wizard :
            // on la signale dans le journal et on renvoie null.
            org.slf4j.LoggerFactory.getLogger(OnboardingStepResponse.class)
                    .warn("completedData illisible pour l'étape {} : {}", raw.length(), malformed.getMessage());
            return null;
        }
    }

    /** Liste ordonnée, pratique pour les réponses `/` et `/progress`. */
    public static List<OnboardingStepResponse> fromAll(
            List<OnboardingWizardStep> entities,
            java.util.Map<com.discipolat.modules.onboarding.domain.OnboardingWizardStep.StepType, OnboardingStepDefinition> definitions,
            ObjectMapper objectMapper) {
        return entities.stream()
                .map(entity -> from(entity, definitions.get(entity.getStepType()), objectMapper))
                .toList();
    }
}
