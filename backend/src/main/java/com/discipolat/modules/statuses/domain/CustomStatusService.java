package com.discipolat.modules.statuses.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * G2.7 — Moteur de statuts configurables.
 *
 * Aucun statut métier n'est un enum figé : chaque espace définit son cycle
 * et ses transitions. Héritage §G1.7 : un jeu défini sur l'espace remplace
 * (OVERRIDDEN) le jeu défini au niveau du tenant ; sinon le tenant fait foi.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomStatusService {

    private final CustomStatusRepository repository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityPropagationPublisher propagationPublisher;

    // ==================== Résolution / héritage ====================

    /** Jeu de statuts effectif : espace (OVERRIDDEN) sinon tenant (héritage G1.7). */
    @Transactional(readOnly = true)
    public List<CustomStatus> resolveStatusSet(UUID tenantId, String entityType, UUID spaceId) {
        requireTenant(tenantId);
        requireEntityType(entityType);
        if (spaceId != null) {
            List<CustomStatus> specific = repository
                    .findByTenantIdAndEntityTypeAndSpaceIdAndDeletedAtIsNullOrderByDisplayOrderAsc(
                            tenantId, entityType, spaceId);
            if (!specific.isEmpty()) {
                return specific;
            }
        }
        return repository
                .findByTenantIdAndEntityTypeAndSpaceIdIsNullAndDeletedAtIsNullOrderByDisplayOrderAsc(
                        tenantId, entityType);
    }

    /** Colonnes de kanban : statuts ordonnés, couleurs et bornes initial/final. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStatusBoard(UUID tenantId, String entityType, UUID spaceId) {
        List<Map<String, Object>> board = new ArrayList<>();
        for (CustomStatus status : resolveStatusSet(tenantId, entityType, spaceId)) {
            Map<String, Object> column = new LinkedHashMap<>();
            column.put("code", status.getCode());
            column.put("name", status.getName());
            column.put("color", status.getColor());
            column.put("icon", status.getIcon());
            column.put("displayOrder", status.getDisplayOrder());
            column.put("initial", status.getInitial());
            column.put("final", status.getFinalStatus());
            column.put("allowedTransitions", status.getAllowedTransitions());
            board.add(column);
        }
        return board;
    }

    @Transactional(readOnly = true)
    public CustomStatus getStatus(UUID tenantId, UUID statusId) {
        requireTenant(tenantId);
        CustomStatus status = repository.findByIdAndTenantId(statusId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("CustomStatus", statusId));
        if (status.isDeleted()) {
            throw new EntityNotFoundException("CustomStatus", statusId);
        }
        return status;
    }

    // ==================== Transitions contrôlées ====================

    /**
     * Valide une transition. Toute transition non déclarée est refusée :
     * la décision est prise côté backend, jamais par l'UI (§0.3 n°4).
     */
    @Transactional(readOnly = true)
    public void validateTransition(UUID tenantId, String entityType, UUID spaceId,
                                   String fromCode, String toCode) {
        List<CustomStatus> set = resolveStatusSet(tenantId, entityType, spaceId);
        CustomStatus from = findByCode(set, fromCode)
                .orElseThrow(() -> new StatusTransitionDeniedException("Statut source inconnu : " + fromCode));
        CustomStatus to = findByCode(set, toCode)
                .orElseThrow(() -> new StatusTransitionDeniedException("Statut cible inconnu : " + toCode));

        if (fromCode.equals(toCode)) {
            throw new StatusTransitionDeniedException("Statut déjà positionné sur " + toCode);
        }
        if (Boolean.TRUE.equals(from.getFinalStatus())) {
            throw new StatusTransitionDeniedException("Aucune transition possible depuis le statut final " + fromCode);
        }
        if (!from.allowsTransitionTo(toCode)) {
            throw new StatusTransitionDeniedException(
                    "Transition non autorisée : " + fromCode + " → " + toCode + " (" + to.getName() + ")");
        }
    }

    /** Applique un changement de statut : validation + événement + audit. */
    public void changeStatus(UUID tenantId, String entityType, UUID spaceId, UUID entityId,
                             String fromCode, String toCode, UUID actorId) {
        validateTransition(tenantId, entityType, spaceId, fromCode, toCode);
        auditService.logSimple("STATUS_CHANGED", entityType, entityId);
        eventPublisher.publishEvent(new StatusChangedEvent(
                tenantId, entityType, entityId, fromCode, toCode, spaceId, actorId));
        // Propagation temps réel (< 5s) : badges et kanban se rafraîchissent côté web + mobile.
        propagationPublisher.publishStatusChanged(entityType, entityId, fromCode, toCode,
                "Statut " + fromCode + " → " + toCode);
    }

    // ==================== CRUD configuration ====================

    public CustomStatus createStatus(UUID tenantId, UUID spaceId, StatusCommand command) {
        requireTenant(tenantId);
        requireEntityType(command.entityType());
        requireCodeAndName(command.code(), command.name());

        Optional<CustomStatus> existing = spaceId != null
                ? repository.findByTenantIdAndEntityTypeAndSpaceIdAndCodeAndDeletedAtIsNull(
                        tenantId, command.entityType(), spaceId, command.code())
                : repository.findByTenantIdAndEntityTypeAndSpaceIdIsNullAndCodeAndDeletedAtIsNull(
                        tenantId, command.entityType(), command.code());
        if (existing.isPresent()) {
            throw new IllegalStateException("Le statut " + command.code() + " existe déjà pour " + command.entityType());
        }

        CustomStatus status = CustomStatus.builder()
                .tenantId(tenantId)
                .entityType(command.entityType())
                .spaceId(spaceId)
                .code(command.code())
                .name(command.name())
                .color(command.color())
                .icon(command.icon())
                .displayOrder(command.displayOrder() != null ? command.displayOrder() : 0)
                .initial(Boolean.TRUE.equals(command.initial()))
                .finalStatus(Boolean.TRUE.equals(command.finalStatus()))
                .allowedTransitions(command.allowedTransitions() != null
                        ? new ArrayList<>(command.allowedTransitions()) : new ArrayList<>())
                .build();

        CustomStatus saved = repository.save(status);
        ensureSingleInitial(tenantId, saved);
        auditService.logSimple("STATUS_CREATED", "CUSTOM_STATUS", saved.getId());
        return saved;
    }

    public CustomStatus updateStatus(UUID tenantId, UUID statusId, StatusCommand command) {
        CustomStatus status = getStatus(tenantId, statusId);
        if (command.name() != null) status.setName(command.name());
        if (command.color() != null) status.setColor(command.color());
        if (command.icon() != null) status.setIcon(command.icon());
        if (command.displayOrder() != null) status.setDisplayOrder(command.displayOrder());
        if (command.initial() != null) status.setInitial(command.initial());
        if (command.finalStatus() != null) status.setFinalStatus(command.finalStatus());
        if (command.allowedTransitions() != null) {
            status.setAllowedTransitions(new ArrayList<>(command.allowedTransitions()));
        }
        CustomStatus saved = repository.save(status);
        auditService.logSimple("STATUS_UPDATED", "CUSTOM_STATUS", saved.getId());
        return saved;
    }

    public void deleteStatus(UUID tenantId, UUID statusId) {
        CustomStatus status = getStatus(tenantId, statusId);
        status.setDeletedAt(Instant.now());
        repository.save(status);
        auditService.logSimple("STATUS_DELETED", "CUSTOM_STATUS", status.getId());
    }

    // ==================== Helpers ====================

    private void ensureSingleInitial(UUID tenantId, CustomStatus saved) {
        if (!Boolean.TRUE.equals(saved.getInitial())) return;
        List<CustomStatus> set = resolveStatusSet(tenantId, saved.getEntityType(), saved.getSpaceId());
        set.stream()
                .filter(s -> !s.getId().equals(saved.getId()) && Boolean.TRUE.equals(s.getInitial()))
                .forEach(other -> {
                    other.setInitial(false);
                    repository.save(other);
                });
    }

    private Optional<CustomStatus> findByCode(List<CustomStatus> set, String code) {
        if (code == null) return Optional.empty();
        return set.stream().filter(s -> code.equals(s.getCode())).findFirst();
    }

    private void requireTenant(UUID tenantId) {
        if (tenantId == null) throw new SecurityException("Aucun tenant dans le contexte");
    }

    private void requireEntityType(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType est obligatoire");
        }
    }

    private void requireCodeAndName(String code, String name) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code est obligatoire");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name est obligatoire");
    }

    /** Commande de création/mise à jour d'un statut. */
    public record StatusCommand(
            String entityType,
            String code,
            String name,
            String color,
            String icon,
            Integer displayOrder,
            Boolean initial,
            Boolean finalStatus,
            List<String> allowedTransitions
    ) {}
}