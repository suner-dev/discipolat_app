package com.discipolat.modules.platform.domain;

import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.modules.tenants.domain.Invitation;
import com.discipolat.modules.tenants.domain.InvitationRepository;
import com.discipolat.modules.tenants.domain.InvitationStatus;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Constat M4 — relances automatiques des invitations en attente.
 *
 * <p>Horloge fixe : les paliers J-3 et J-1 sont testés sans attendre 7 jours,
 * et surtout on vérifie qu'un palier n'est jamais renvoyé deux fois.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitationReminderSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-09-27T08:00:00Z");
    private static final String FRONTEND_URL = "https://app.example.com";

    @Mock private InvitationRepository invitationRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;

    private InvitationReminderScheduler scheduler;
    private UUID tenantId;
    private UUID inviterId;

    @BeforeEach
    void setUp() {
        scheduler = new InvitationReminderScheduler(invitationRepository, tenantRepository,
                emailService, auditService, FRONTEND_URL);
        scheduler.setClock(Clock.fixed(NOW, ZoneOffset.UTC));
        tenantId = UUID.randomUUID();
        inviterId = UUID.randomUUID();
        when(tenantRepository.findById(any()))
                .thenReturn(Optional.of(Tenant.builder().id(tenantId).name("Église Bethel").build()));
        when(emailService.sendInvitationReminder(anyString(), any(), any(), anyString(), anyInt()))
                .thenReturn(true);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Invitation pendingInvitationExpiringIn(int days, Instant lastReminder) {
        Invitation invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email("invite@" + days + "days.example")
                .role("MEMBER")
                .scopeType(MembershipScopeType.TENANT.name())
                .inviterId(inviterId)
                .status(InvitationStatus.PENDING)
                .expiresAt(NOW.plus(days, ChronoUnit.DAYS))
                .build();
        invitation.setRemindedAt(lastReminder);
        return invitation;
    }

    private void givenWindowContains(Invitation... invitations) {
        when(invitationRepository.findByStatusAndExpiresAtBetween(
                eq(InvitationStatus.PENDING), any(), any())).thenReturn(List.of(invitations));
    }

    // ---------- palier J-3 ----------

    @Test
    @DisplayName("J-3 : la relance est envoyée, reminded_at est positionné, audit écrit")
    void threeDayTierIsSent() {
        Invitation invitation = pendingInvitationExpiringIn(3, null);
        givenWindowContains(invitation);

        int sent = scheduler.sendRemindersFor(NOW, 3, InvitationReminderScheduler.WINDOW_THREE_DAYS);

        assertThat(sent).isEqualTo(1);
        verify(emailService).sendInvitationReminder(
                eq(invitation.getEmail()), any(), eq("Église Bethel"),
                eq(FRONTEND_URL + "/login"), eq(3));
        assertThat(invitation.getRemindedAt()).isEqualTo(NOW);
        verify(invitationRepository).save(invitation);
        verify(auditService).logSimple("INVITATION_REMINDER_SENT", "INVITATION", invitation.getId());
    }

    // ---------- palier J-1 ----------

    @Test
    @DisplayName("J-1 : la relance est envoyée")
    void oneDayTierIsSent() {
        Invitation invitation = pendingInvitationExpiringIn(1, null);
        givenWindowContains(invitation);

        int sent = scheduler.sendRemindersFor(NOW, 1, InvitationReminderScheduler.WINDOW_ONE_DAY);

        assertThat(sent).isEqualTo(1);
        verify(emailService).sendInvitationReminder(
                eq(invitation.getEmail()), any(), anyString(), anyString(), eq(1));
        assertThat(invitation.getRemindedAt()).isEqualTo(NOW);
    }

    // ---------- pas de doublon par palier ----------

    @Test
    @DisplayName("Un palier déjà envoyé n'est JAMAIS renvoyé")
    void sameTierIsNeverSentTwice() {
        // Dernier rappel = exactement l'instant du palier J-1 (maintenant).
        Invitation invitation = pendingInvitationExpiringIn(1, NOW);
        givenWindowContains(invitation);

        int sent = scheduler.sendRemindersFor(NOW, 1, InvitationReminderScheduler.WINDOW_ONE_DAY);

        assertThat(sent).isZero();
        verify(emailService, never()).sendInvitationReminder(anyString(), any(), any(), anyString(), anyInt());
        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    @DisplayName("Le palier J-3 déjà envoyé n'empêche PAS le palier J-1")
    void nextTierIsStillSentAfterPreviousOne() {
        // Dernier rappel = il y a 2 jours, soit le palier J-3 (J-3 => il y a 4 jours).
        Invitation invitation = pendingInvitationExpiringIn(1, NOW.minus(2, ChronoUnit.DAYS));
        givenWindowContains(invitation);

        int sent = scheduler.sendRemindersFor(NOW, 1, InvitationReminderScheduler.WINDOW_ONE_DAY);

        assertThat(sent).isEqualTo(1);
        verify(emailService).sendInvitationReminder(
                eq(invitation.getEmail()), any(), anyString(), anyString(), eq(1));
    }

    // ---------- hors fenêtre ----------

    @Test
    @DisplayName("Hors fenêtre : aucun envoi")
    void outsideWindowSendsNothing() {
        when(invitationRepository.findByStatusAndExpiresAtBetween(any(), any(), any()))
                .thenReturn(List.of());

        int sent = scheduler.sendRemindersFor(NOW, 3, InvitationReminderScheduler.WINDOW_THREE_DAYS);

        assertThat(sent).isZero();
        verify(emailService, never()).sendInvitationReminder(anyString(), any(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("La fenêtre de recherche est bien centrée sur le palier")
    void windowIsCentredOnTheTier() {
        givenWindowContains();
        scheduler.sendRemindersFor(NOW, 3, InvitationReminderScheduler.WINDOW_THREE_DAYS);

        // J-3 = NOW+3j ; fenêtre ± 12 h
        verify(invitationRepository).findByStatusAndExpiresAtBetween(
                eq(InvitationStatus.PENDING),
                eq(NOW.plus(3, ChronoUnit.DAYS).minus(12, ChronoUnit.HOURS)),
                eq(NOW.plus(3, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS)));
    }

    @Test
    @DisplayName("La fenêtre J-1 est plus étroite (± 6 h) que la fenêtre J-3")
    void oneDayWindowIsNarrower() {
        givenWindowContains();
        scheduler.sendRemindersFor(NOW, 1, InvitationReminderScheduler.WINDOW_ONE_DAY);

        verify(invitationRepository).findByStatusAndExpiresAtBetween(
                eq(InvitationStatus.PENDING),
                eq(NOW.plus(1, ChronoUnit.DAYS).minus(6, ChronoUnit.HOURS)),
                eq(NOW.plus(1, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS)));
    }

    // ---------- robustesse ----------

    @Test
    @DisplayName("Un échec SMTP ne marque PAS l'invitation : le job réessaiera")
    void smtpFailureDoesNotMarkTheInvitation() {
        Invitation invitation = pendingInvitationExpiringIn(3, null);
        givenWindowContains(invitation);
        doThrow(new IllegalStateException("SMTP indisponible"))
                .when(emailService).sendInvitationReminder(anyString(), any(), any(), anyString(), anyInt());

        int sent = scheduler.sendRemindersFor(NOW, 3, InvitationReminderScheduler.WINDOW_THREE_DAYS);

        assertThat(sent).isZero();
        assertThat(invitation.getRemindedAt()).isNull();
        verify(invitationRepository, never()).save(any(Invitation.class));
        verify(auditService, never()).logSimple(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Une panne de l'audit n'empêche pas l'envoi")
    void auditFailureDoesNotBreakTheReminder() {
        Invitation invitation = pendingInvitationExpiringIn(3, null);
        givenWindowContains(invitation);
        doThrow(new IllegalStateException("audit indisponible"))
                .when(auditService).logSimple(anyString(), anyString(), any());

        int sent = scheduler.sendRemindersFor(NOW, 3, InvitationReminderScheduler.WINDOW_THREE_DAYS);

        assertThat(sent).isEqualTo(1);
        verify(emailService).sendInvitationReminder(anyString(), any(), anyString(), anyString(), eq(3));
    }

    @Test
    @DisplayName("Le job quotidien enchaîne les deux paliers")
    void dailyJobHandlesBothTiers() {
        Invitation threeDays = pendingInvitationExpiringIn(3, null);
        Invitation oneDay = pendingInvitationExpiringIn(1, null);
        when(invitationRepository.findByStatusAndExpiresAtBetween(any(), any(), any()))
                .thenReturn(List.of(threeDays))
                .thenReturn(List.of(oneDay));

        scheduler.sendReminders();

        verify(emailService).sendInvitationReminder(
                eq(threeDays.getEmail()), any(), anyString(), anyString(), eq(3));
        verify(emailService).sendInvitationReminder(
                eq(oneDay.getEmail()), any(), anyString(), anyString(), eq(1));
        verify(invitationRepository, times(2)).save(any(Invitation.class));
    }

    @Test
    @DisplayName("Seules les invitations PENDING sont considérées (jamais ACCEPTED/EXPIRED)")
    void onlyPendingInvitationsAreConsidered() {
        givenWindowContains();
        scheduler.sendReminders();

        // Le job quotidien interroge la fenêtre une fois par palier (J-3 puis J-1).
        verify(invitationRepository, times(2)).findByStatusAndExpiresAtBetween(
                eq(InvitationStatus.PENDING), any(), any());
    }
}
