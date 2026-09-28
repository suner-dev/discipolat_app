package com.discipolat.modules.platform.domain;

import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.modules.tenants.domain.Invitation;
import com.discipolat.modules.tenants.domain.InvitationRepository;
import com.discipolat.modules.tenants.domain.InvitationStatus;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Constat M4 — relances automatiques des invitations en attente.
 *
 * <p>Avant ce correctif, une invitation expirait silencieusement au bout de 7
 * jours : personne n'était prévenu, l'église ne savait pas que son invitation
 * <p>Avant ce correctif, une invitation expirait silencieusement au bout de 7
 *
 * <p><b>Deux paliers</b> : J-3 et J-1 avant l'expiration. Chaque palier n'est
 * envoyé <b>qu'une fois par invitation</b>, grâce à {@code reminded_at} (V184).
 *
 * <p><b>JAMAIS de relance pour une invitation déjà relancée au même palier</b> :
 * le contrôle compare la date du dernier rappel à la fenêtre du palier courant.
 * Sans ce garde-fou, le job quotidien enverrait 7 rappels J-1 successifs à la
 * même personne.
 */
@Component
public class InvitationReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvitationReminderScheduler.class);

    /** Demi-fenêtre de recherche autour du palier J-3 (± 12 h). */
    static final Duration WINDOW_THREE_DAYS = Duration.ofHours(12);
    /** Demi-fenêtre de recherche autour du palier J-1 (± 6 h). */
    static final Duration WINDOW_ONE_DAY = Duration.ofHours(6);

    /** Durée de validité d'une invitation : 7 jours (cf. InvitationService). */
    static final int INVITATION_VALIDITY_DAYS = 7;

    private final InvitationRepository invitationRepository;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final String frontendUrl;

    /** Horloge injectable pour tester les paliers sans attendre 7 jours. */
    private Clock clock = Clock.systemUTC();

    public InvitationReminderScheduler(InvitationRepository invitationRepository,
                                       TenantRepository tenantRepository,
                                       EmailService emailService,
                                       AuditService auditService,
                                       @org.springframework.beans.factory.annotation.Value(
                                               "${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.invitationRepository = invitationRepository;
        this.tenantRepository = tenantRepository;
        this.emailService = emailService;
        this.auditService = auditService;
        this.frontendUrl = frontendUrl;
    }

    /** @visibleForTesting */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    /** Exécution quotidienne à 08h : relance les invitations sur le point d'expirer. */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendReminders() {
        Instant now = Instant.now(clock);
        int sentThreeDays = sendRemindersFor(now, 3, WINDOW_THREE_DAYS);
        int sentOneDay = sendRemindersFor(now, 1, WINDOW_ONE_DAY);
        if (sentThreeDays + sentOneDay > 0) {
            log.info("Relances d'invitation envoyées : {} à J-3, {} à J-1", sentThreeDays, sentOneDay);
        }
    }

    /**
     * Envoie les relances d'un palier.
     *
     * @param now          instant de référence
     * @param daysLeft     palier (3 ou 1)
     * @param windowWidth  demi-largeur de la fenêtre de recherche
     * @return nombre de relances effectivement envoyées
     */
    int sendRemindersFor(Instant now, int daysLeft, Duration windowWidth) {
        Instant target = now.plus(daysLeft, ChronoUnit.DAYS);
        Instant from = target.minus(windowWidth);
        Instant to = target.plus(windowWidth);

        List<Invitation> invitations = invitationRepository
                .findByStatusAndExpiresAtBetween(InvitationStatus.PENDING, from, to);

        int sent = 0;
        for (Invitation invitation : invitations) {
            if (alreadyRemindedFor(invitation, daysLeft, now)) {
                continue;
            }
            if (sendOne(invitation, daysLeft, now)) {
                sent++;
            }
        }
        return sent;
    }

    /**
     * Un rappel n'est renvoyé que si le dernier envoi est plus ancien que le
 * <p>Avant ce correctif, une invitation expirait silencieusement au bout de 7
     * recevrait un rappel par exécution du job.
     */
    private boolean alreadyRemindedFor(Invitation invitation, int daysLeft, Instant now) {
        Instant lastReminder = invitation.getRemindedAt();
        if (lastReminder == null) {
            return false;
        }
        Instant currentTierSentAt = invitation.getExpiresAt().minus(daysLeft, ChronoUnit.DAYS);
        return !lastReminder.isBefore(currentTierSentAt);
    }

    /**
     * Envoie une relance. <b>Aucune exception ne sort d'ici</b> : un job planifié
     * qui propagerait une panne d'un tiers s'arrêterait silencieusement pour tous
     * les tenants, et l'interruption serait plus grave que la relance perdue.
     */
    private boolean sendOne(Invitation invitation, int daysLeft, Instant now) {
        try {
            return doSendOne(invitation, daysLeft, now);
        } catch (RuntimeException failure) {
            log.warn("Relance d'invitation en echec pour {} : {}",
                    invitation.getId(), failure.getMessage());
            return false;
        }
    }

    private boolean doSendOne(Invitation invitation, int daysLeft, Instant now) {
        // Le lien de relance n'est pas reconstructible : le token n'est stocké
        // que haché (V173/V175). On ne renvoie donc PAS de lien de/token
        // inventé : l'invité doit repasser par l'écran d'invitations, et le
        // message le redirige vers la connexion s'il possède déjà un compte.
        String churchName = tenantRepository.findById(invitation.getTenantId())
                .map(Tenant::getName)
                .orElse(null);
        String loginUrl = frontendUrl + "/login";

        boolean sent = emailService.sendInvitationReminder(
                invitation.getEmail(), null, churchName, loginUrl, daysLeft);
        if (!sent) {
            // Échec SMTP : on ne marque pas l'invitation comme relancée, le job
            // réessaiera au prochain passage plutôt que de perdre la relance.
            log.warn("Relance d'invitation non envoyée à {} (palier J-{})", invitation.getEmail(), daysLeft);
            return false;
        }

        invitation.setRemindedAt(now);
        invitationRepository.save(invitation);
        try {
            auditService.logSimple("INVITATION_REMINDER_SENT", "INVITATION", invitation.getId());
        } catch (RuntimeException auditFailure) {
            // La relance EST partie : l'échec de l'audit ne doit pas la rejouer.
            log.warn("Audit de relance non écrit pour l'invitation {} : {}",
                    invitation.getId(), auditFailure.getMessage());
        }
        return true;
    }
}
