package com.discipolat.common.infrastructure.propagation;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.SoulHistory;
import com.discipolat.modules.souls.domain.SoulHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central listener for all entity change propagation events.
 * Ensures consistent downstream effects:
 *
 * 1. AUDIT — Every mutation is logged with old/new values
 * 2. HISTORY — Soul changes are tracked in soul_history
 * 3. NOTIFICATIONS — Relevant actors are notified
 * 4. SEARCH — Search index is consistent (live-queried, no index needed)
 * 5. STATISTICS — Dashboard/stats recalc on next read (live-calculated)
 *
 * One entity = one source of truth. This listener guarantees
 * that no matter which service performs the mutation, the same
 * downstream effects are triggered consistently.
 */
@Component
public class EntityPropagationListener {

    private static final Logger log = LoggerFactory.getLogger(EntityPropagationListener.class);

    private final AuditService auditService;
    private final SoulHistoryRepository soulHistoryRepository;
    private final NotificationService notificationService;

    public EntityPropagationListener(AuditService auditService,
                                     SoulHistoryRepository soulHistoryRepository,
                                     NotificationService notificationService) {
        this.auditService = auditService;
        this.soulHistoryRepository = soulHistoryRepository;
        this.notificationService = notificationService;
    }

    // ========================================================================
    // AUDIT LOGGING — Toute mutation est traçable
    // ========================================================================

    @EventListener
    @Transactional
    public void onEntityChanged(EntityChangedEvent event) {
        logAudit(event);
        logSoulHistoryIfApplicable(event);
    }

    /**
     * Logs the change to the audit trail with old and new values.
     */
    private void logAudit(EntityChangedEvent event) {
        try {
            String action = buildAuditAction(event);
            Map<String, Object> oldValues = event.getOldValues().isEmpty() ? null : new LinkedHashMap<>(event.getOldValues());
            Map<String, Object> newValues = event.getNewValues().isEmpty() ? null : new LinkedHashMap<>(event.getNewValues());
            auditService.log(action, event.getEntityType(), event.getEntityId(),
                    oldValues, newValues, null);
        } catch (Exception e) {
            log.warn("Audit logging failed for {} {}: {}",
                    event.getEntityType(), event.getEntityId(), e.getMessage());
        }
    }

    /**
     * Builds a consistent audit action string from the event.
     */
    private String buildAuditAction(EntityChangedEvent event) {
        return switch (event.getChangeType()) {
            case CREATED -> event.getEntityType() + "_CREATED";
            case UPDATED -> event.getEntityType() + "_UPDATED";
            case DELETED -> event.getEntityType() + "_DELETED";
            case SOFT_DELETED -> event.getEntityType() + "_SOFT_DELETED";
            case RESTORED -> event.getEntityType() + "_RESTORED";
            case STATUS_CHANGED -> event.getEntityType() + "_STATUS_CHANGED";
            case REASSIGNED -> event.getEntityType() + "_REASSIGNED";
        };
    }

    // ========================================================================
    // SOUL HISTORY — Historique métier pour les âmes
    // ========================================================================

    /**
     * Logs a soul history entry when a SOUL entity is changed.
     * The history provides a complete audit trail of all changes
     * to a soul's data, visible in the pastoral dossier 360°.
     */
    private void logSoulHistoryIfApplicable(EntityChangedEvent event) {
        if (!"SOUL".equals(event.getEntityType())) return;

        try {
            SoulHistory history = new SoulHistory();
            history.setAmeId(event.getEntityId());
            history.setUtilisateurId(event.getActorId());

            switch (event.getChangeType()) {
                case CREATED -> {
                    history.setTypeEvenement("CREATION");
                    history.setDescription("Âme créée");
                }
                case STATUS_CHANGED -> {
                    history.setTypeEvenement("CHANGEMENT_STATUT");
                    String oldStatut = String.valueOf(event.previousValue("statut"));
                    String newStatut = String.valueOf(event.currentValue("statut"));
                    history.setDescription("Statut changé: " + oldStatut + " -> " + newStatut);
                    history.setAncienStatut(oldStatut);
                    history.setNouveauStatut(newStatut);
                }
                case REASSIGNED -> {
                    if (event.getNewValues().containsKey("faiseurId")) {
                        history.setTypeEvenement("REAFFECTATION");
                        history.setAncienFaiseurId(uuidOrNull(event.previousValue("faiseurId")));
                        history.setNouveauFaiseurId(uuidOrNull(event.currentValue("faiseurId")));
                        history.setDescription("Réaffectation de faiseur");
                    } else if (event.getNewValues().containsKey("familleId")) {
                        history.setTypeEvenement("TRANSFERT_FAMILLE");
                        history.setDescription("Transfert de famille");
                    } else if (event.getNewValues().containsKey("departementId")) {
                        history.setTypeEvenement("TRANSFERT_DEPARTEMENT");
                        history.setDescription("Transfert de département");
                    }
                }
                case UPDATED -> {
                    // Log specific field changes
                    if (event.fieldChanged("etatSpirituel")) {
                        SoulHistory h = new SoulHistory();
                        h.setAmeId(event.getEntityId());
                        h.setUtilisateurId(event.getActorId());
                        h.setTypeEvenement("CHANGEMENT_ETAT_SPIRITUEL");
                        h.setDescription("État spirituel: " +
                                event.previousValue("etatSpirituel") + " -> " +
                                event.currentValue("etatSpirituel"));
                        soulHistoryRepository.save(h);
                        return; // Already saved, don't fall through
                    }
                    if (event.fieldChanged("niveauCroissance")) {
                        SoulHistory h = new SoulHistory();
                        h.setAmeId(event.getEntityId());
                        h.setUtilisateurId(event.getActorId());
                        h.setTypeEvenement("CHANGEMENT_NIVEAU_CROISSANCE");
                        h.setDescription("Niveau de croissance: " +
                                event.previousValue("niveauCroissance") + " -> " +
                                event.currentValue("niveauCroissance"));
                        soulHistoryRepository.save(h);
                        return;
                    }
                    history.setTypeEvenement("MISE_A_JOUR");
                    history.setDescription(event.getDescription() != null
                            ? event.getDescription() : "Mise à jour de l'âme");
                }
                case SOFT_DELETED -> {
                    history.setTypeEvenement("SUPPRESSION");
                    history.setDescription("Âme supprimée (corbeille)");
                }
                case RESTORED -> {
                    history.setTypeEvenement("RESTAURATION");
                    history.setDescription("Âme restaurée");
                }
                default -> {
                    return; // Don't save generic history for unknown types
                }
            }

            soulHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("Soul history logging failed for soul {}: {}",
                    event.getEntityId(), e.getMessage());
        }
    }

    // ========================================================================
    // NOTIFICATIONS — Les personnes concernées sont notifiées
    // ========================================================================

    /**
     * Sends notifications for soul faiseur reassignment.
     * Both old and new faiseurs are notified.
     */
    public void notifyFaiseurReassignment(UUID soulId, String soulName,
                                          UUID oldFaiseurId, UUID newFaiseurId) {
        if (oldFaiseurId != null) {
            try {
                notificationService.create(oldFaiseurId, TypeNotification.INFORMATION,
                        CanalNotification.IN_APP, "Disciple réaffecté",
                        "Le disciple " + soulName + " ne fait plus partie de vos disciples.",
                        soulId, "SOUL");
            } catch (Exception e) {
                log.warn("Notification failed for old faiseur {}: {}", oldFaiseurId, e.getMessage());
            }
        }
        if (newFaiseurId != null) {
            try {
                notificationService.create(newFaiseurId, TypeNotification.INFORMATION,
                        CanalNotification.IN_APP, "Nouveau disciple affecté",
                        "Le disciple " + soulName + " vous a été affecté.",
                        soulId, "SOUL");
            } catch (Exception e) {
                log.warn("Notification failed for new faiseur {}: {}", newFaiseurId, e.getMessage());
            }
        }
    }

    /**
     * Sends notifications for family chef change.
     */
    public void notifyChefFamilleChange(UUID familyId, String familyName,
                                        UUID oldChefId, UUID newChefId) {
        if (oldChefId != null) {
            try {
                notificationService.create(oldChefId, TypeNotification.INFORMATION,
                        CanalNotification.IN_APP, "Vous n'êtes plus chef de famille",
                        "Vous n'êtes plus chef de la famille " + familyName,
                        familyId, "FAMILY");
            } catch (Exception e) {
                log.warn("Notification failed for old chef {}: {}", oldChefId, e.getMessage());
            }
        }
        if (newChefId != null) {
            try {
                notificationService.create(newChefId, TypeNotification.INFORMATION,
                        CanalNotification.IN_APP, "Nouveau chef de famille",
                        "Vous êtes maintenant chef de la famille " + familyName,
                        familyId, "FAMILY");
            } catch (Exception e) {
                log.warn("Notification failed for new chef {}: {}", newChefId, e.getMessage());
            }
        }
    }

    /**
     * Sends notifications for department responsable change.
     */
    public void notifyResponsableChange(UUID departmentId, String departmentName,
                                        UUID oldResponsableId, UUID newResponsableId) {
        if (oldResponsableId != null) {
            try {
                notificationService.create(oldResponsableId, TypeNotification.INFORMATION,
                        CanalNotification.IN_APP, "Vous n'êtes plus responsable",
                        "Vous n'êtes plus responsable du département " + departmentName,
                        departmentId, "DEPARTMENT");
            } catch (Exception e) {
                log.warn("Notification failed for old responsable {}: {}", oldResponsableId, e.getMessage());
            }
        }
        if (newResponsableId != null) {
            try {
                notificationService.create(newResponsableId, TypeNotification.INFORMATION,
                        CanalNotification.IN_APP, "Nouveau responsable",
                        "Vous êtes maintenant responsable du département " + departmentName,
                        departmentId, "DEPARTMENT");
            } catch (Exception e) {
                log.warn("Notification failed for new responsable {}: {}", newResponsableId, e.getMessage());
            }
        }
    }

    /**
     * Sends notification for soul status change to the faiseur.
     */
    public void notifyStatutChangeToFaiseur(UUID soulId, String soulName,
                                            UUID faiseurId, String oldStatut, String newStatut) {
        if (faiseurId == null) return;
        try {
            notificationService.create(faiseurId, TypeNotification.INFORMATION,
                    CanalNotification.IN_APP, "Statut disciple modifié",
                    "Le statut de " + soulName + " est passé de " + oldStatut + " à " + newStatut,
                    soulId, "SOUL");
        } catch (Exception e) {
            log.warn("Notification failed for faiseur {}: {}", faiseurId, e.getMessage());
        }
    }

    /**
     * Sends notification for soul spiritual state change to the faiseur.
     */
    public void notifyEtatSpirituelChangeToFaiseur(UUID soulId, String soulName,
                                                    UUID faiseurId, String oldEtat, String newEtat) {
        if (faiseurId == null) return;
        try {
            notificationService.create(faiseurId, TypeNotification.INFORMATION,
                    CanalNotification.IN_APP, "État spirituel modifié",
                    "L'état spirituel de " + soulName + " est passé de " + oldEtat + " à " + newEtat,
                    soulId, "SOUL");
        } catch (Exception e) {
            log.warn("Notification failed for faiseur {}: {}", faiseurId, e.getMessage());
        }
    }

    // ========================================================================
    // USER NOTIFICATIONS — Rôle, statut, promotion/démotion
    // ========================================================================

    public void notifyUserRoleChange(UUID userId, String userName,
                                    String oldRole, String newRole) {
        if (userId == null) return;
        try {
            notificationService.create(userId, TypeNotification.INFORMATION,
                    CanalNotification.IN_APP, "Rôle modifié",
                    "Votre rôle a été modifié de " + oldRole + " à " + newRole,
                    userId, "USER");
        } catch (Exception e) {
            log.warn("Notification failed for user {}: {}", userId, e.getMessage());
        }
    }

    public void notifyUserStatusChange(UUID userId, String userName,
                                      String oldStatus, String newStatus) {
        if (userId == null) return;
        try {
            notificationService.create(userId, TypeNotification.INFORMATION,
                    CanalNotification.IN_APP, "Statut modifié",
                    "Votre statut est passé de " + oldStatus + " à " + newStatus,
                    userId, "USER");
        } catch (Exception e) {
            log.warn("Notification failed for user {}: {}", userId, e.getMessage());
        }
    }

    // ========================================================================
    // EVENT NOTIFICATIONS — Création, annulation, modification
    // ========================================================================

    public void notifyEventCreated(UUID organisateurId, UUID eventId, String titre) {
        // Notifications already handled inline in EventService for pastoral notifications
    }

    public void notifyEventStatusChanged(UUID organisateurId, UUID eventId,
                                         String titre, String oldStatut, String newStatut) {
        if (organisateurId == null) return;
        try {
            notificationService.create(organisateurId, TypeNotification.INFORMATION,
                    CanalNotification.IN_APP, "Statut événement modifié",
                    "L'événement \"" + titre + "\" est passé de " + oldStatut + " à " + newStatut,
                    eventId, "EVENT");
        } catch (Exception e) {
            log.warn("Notification failed for event {}: {}", eventId, e.getMessage());
        }
    }

    // ========================================================================
    // VISIT NOTIFICATIONS — Planification, réalisation
    // ========================================================================

    public void notifyVisitCreated(UUID visiteurId, UUID visitId, UUID soulId) {
        // Visit creation is a personal action, no notification needed
    }

    public void notifyVisitStatusChanged(UUID visiteurId, UUID visitId,
                                         UUID soulId, String oldStatut, String newStatut) {
        if (visiteurId == null) return;
        try {
            notificationService.create(visiteurId, TypeNotification.INFORMATION,
                    CanalNotification.IN_APP, "Statut visite modifié",
                    "La visite est passée de " + oldStatut + " à " + newStatut,
                    visitId, "VISIT");
        } catch (Exception e) {
            log.warn("Notification failed for visit {}: {}", visitId, e.getMessage());
        }
    }

    // ========================================================================
    // TRANSFER NOTIFICATIONS — Soumission, validation, exécution
    // ========================================================================

    public void notifyTransferStatusChanged(UUID userId, UUID transferId,
                                            String type, String oldStatut, String newStatut) {
        if (userId == null) return;
        try {
            notificationService.create(userId, TypeNotification.INFORMATION,
                    CanalNotification.IN_APP, "Transfert " + type,
                    "La demande de transfert est passée de " + oldStatut + " à " + newStatut,
                    transferId, "TRANSFER");
        } catch (Exception e) {
            log.warn("Notification failed for transfer {}: {}", transferId, e.getMessage());
        }
    }

    public void notifyTransferValidationRequired(UUID validateurId, UUID transferId,
                                                 String type, String demandeurNom) {
        if (validateurId == null) return;
        try {
            notificationService.create(validateurId, TypeNotification.INFORMATION,
                    CanalNotification.IN_APP, "Validation de transfert requise",
                    demandeurNom + " a soumis une demande de transfert de type " + type
                            + " nécessitant votre validation.",
                    transferId, "TRANSFER");
        } catch (Exception e) {
            log.warn("Notification failed for transfer validation {}: {}", transferId, e.getMessage());
        }
    }

    // ========================================================================
    // EVALUATION NOTIFICATIONS — Nouvelle évaluation
    // ========================================================================

    public void notifyEvaluationCreated(UUID evalueId, UUID evaluationId,
                                        String categorie, int note) {
        if (evalueId == null) return;
        try {
            notificationService.create(evalueId, TypeNotification.INFORMATION,
                    CanalNotification.IN_APP, "Nouvelle évaluation",
                    "Vous avez reçu une nouvelle évaluation en " + categorie.toLowerCase()
                            + " avec la note: " + note + "/5",
                    evaluationId, "EVALUATION");
        } catch (Exception e) {
            log.warn("Notification failed for evaluation {}: {}", evaluationId, e.getMessage());
        }
    }

    // ========================================================================
    // EVANGELISM NOTIFICATIONS — Avancement dans le pipeline
    // ========================================================================

    public void notifyEvangelismStageChanged(UUID faiseurId, UUID trackId,
                                             String soulName, String newEtape) {
        if (faiseurId == null) return;
        try {
            notificationService.create(faiseurId, TypeNotification.INFORMATION,
                    CanalNotification.IN_APP, "Étape d'évangélisation avancée",
                    "Le disciple " + soulName + " est maintenant à l'étape: " + newEtape,
                    trackId, "EVANGELISM_TRACK");
        } catch (Exception e) {
            log.warn("Notification failed for evangelism {}: {}", trackId, e.getMessage());
        }
    }

    // ========================================================================
    // INVENTORY NOTIFICATIONS — Affectation/désaffectation
    // ========================================================================

    public void notifyInventoryAssigned(UUID memberId, UUID itemId, String itemName) {
        if (memberId == null) return;
        try {
            notificationService.create(memberId, TypeNotification.INFORMATION,
                    CanalNotification.IN_APP, "Équipement affecté",
                    "L'équipement \"" + itemName + "\" vous a été affecté.",
                    itemId, "INVENTORY_ITEM");
        } catch (Exception e) {
            log.warn("Notification failed for inventory {}: {}", itemId, e.getMessage());
        }
    }

    // ========================================================================
    // APPOINTMENT NOTIFICATIONS — Prise/de validation de RDV
    // ========================================================================

    public void notifyAppointmentStatusChanged(UUID userId, UUID appointmentId,
                                               String motif, String newStatut) {
        if (userId == null) return;
        try {
            notificationService.create(userId, TypeNotification.INFORMATION,
                    CanalNotification.IN_APP, "Rendez-vous " + newStatut.toLowerCase(),
                    "Votre rendez-vous \"" + motif + "\" a été " + newStatut.toLowerCase(),
                    appointmentId, "APPOINTMENT");
        } catch (Exception e) {
            log.warn("Notification failed for appointment {}: {}", appointmentId, e.getMessage());
        }
    }

    // ========================================================================
    // COMMUNICATION NOTIFICATIONS — Publication d'annonce
    // ========================================================================

    public void notifyCommunicationPublished(UUID userId, UUID communicationId, String titre) {
        // Notifications are handled inline in CommunicationService.publish() for targeted delivery
    }

    private UUID uuidOrNull(Object value) {
        if (value == null) return null;
        if (value instanceof UUID uuid) return uuid;
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
