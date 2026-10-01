package com.discipolat.modules.lowband;

import com.discipolat.modules.dresscode.domain.DressCodeRepository;
import com.discipolat.modules.dresscode.domain.DressCodeRuleRepository;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventAttendance;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.events.repository.EventAttendanceRepository;
import com.discipolat.modules.lowband.domain.LowBandInteractionRepository;
import com.discipolat.modules.lowband.domain.LowBandPortalService;
import com.discipolat.modules.notifications.domain.NotificationRepository;
import com.discipolat.modules.payments.domain.PaymentGatewayService;
import com.discipolat.modules.payments.domain.PaymentIntent;
import com.discipolat.modules.people.domain.Person;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.prayers.domain.PrayerRepository;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.service.TenantSettingsService;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * §G5.9 — Portail basse connexion : sécurité et parcours minimal des commandes
 * WhatsApp/USSD member-first (toggle tenant, refus d'ambiguïté, pas de perte
 * silencieuse des demandes de prière, bornes de don, présence flash idempotente).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LowBandPortalServiceTest {

    @Mock private TenantSettingsService tenantSettingsService;
    @Mock private PersonRepository personRepository;
    @Mock private DressCodeRepository dressCodeRepository;
    @Mock private DressCodeRuleRepository dressCodeRuleRepository;
    // PORT Develop1 -> main : l'entite « ChurchEvent » de Develop1 a ete absorbee
    // par l'entite vivante Event de main (une seule classe mappe la table `event`).
    // Le test suit donc Event / EventRepository, sans changer une seule attente.
    @Mock private EventRepository eventRepository;
    @Mock private EventAttendanceRepository eventAttendanceRepository;
    @Mock private PaymentGatewayService paymentGatewayService;
    @Mock private UserRepository userRepository;
    @Mock private PrayerRepository prayerRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private LowBandInteractionRepository interactionRepository;

    private LowBandPortalService service;

    private final UUID tenantId = UUID.randomUUID();
    private final String phone = "+24160000000";

    @BeforeEach
    void setUp() {
        service = new LowBandPortalService(tenantSettingsService, personRepository,
                dressCodeRepository, dressCodeRuleRepository, eventRepository,
                eventAttendanceRepository, paymentGatewayService, userRepository,
                prayerRepository, notificationRepository, interactionRepository);
    }

    private void lowBandEnabled(boolean enabled) {
        TenantSettings settings = new TenantSettings();
        settings.setLowBandEnabled(enabled);
        when(tenantSettingsService.getSettings(tenantId)).thenReturn(settings);
    }

    private Person person() {
        Person p = new Person();
        p.setId(UUID.randomUUID());
        p.setTenantId(tenantId);
        p.setPhoneNormalized("24160000000");
        return p;
    }

    private Event publishedEvent() {
        Event e = new Event();
        e.setId(UUID.randomUUID());
        e.setTenantId(tenantId);
        e.setTitre("Culte de louange");
        // Vocabulaire FR du contrat main ; PLANIFIE = l'evenement publie a venir.
        e.setStatut(Event.STATUT_PLANIFIE);
        e.setVisibility("PUBLIC");
        e.setDateDebut(java.time.LocalDateTime.now(ZoneOffset.UTC).plusHours(2));
        return e;
    }

    // ==================== Toggle tenant (§G1.2) ====================

    @Test
    void refusesAllCommandsWhenTenantToggleDisabled() {
        lowBandEnabled(false);
        String reply = service.handleWhatsAppCommand(tenantId, phone, "#tenue");
        assertThat(reply).contains("pas activé");
        verify(dressCodeRepository, never())
                .findByTenantIdAndArchivedFalseAndBeginsAtAfterOrderByBeginsAtAsc(any(), any());
    }

    @Test
    void missingSettingsFailsClosed() {
        when(tenantSettingsService.getSettings(tenantId)).thenThrow(new RuntimeException("no row"));
        assertThat(service.isLowBandEnabled(tenantId)).isFalse();
    }

    // ==================== #DON ====================

    @Test
    void donWithoutAmountReturnsUsage() {
        lowBandEnabled(true);
        assertThat(service.handleWhatsAppCommand(tenantId, phone, "#don"))
                .contains("Usage : #don 5000");
        verify(paymentGatewayService, never()).initiate(any());
    }

    @Test
    void donAboveCeilingIsRefused() {
        lowBandEnabled(true);
        assertThat(service.handleWhatsAppCommand(tenantId, phone, "#don 9999999"))
                .contains("hors limites");
        verify(paymentGatewayService, never()).initiate(any());
    }

    @Test
    void donWithinBoundsInitiatesPaymentIntent() {
        lowBandEnabled(true);
        PaymentIntent stored = new PaymentIntent();
        stored.setCheckoutUrl("https://pay.example/confirm");
        when(paymentGatewayService.initiate(any(PaymentIntent.class))).thenReturn(stored);

        String reply = service.handleWhatsAppCommand(tenantId, phone, "#don 5000");

        assertThat(reply).contains("5000").contains("https://pay.example/confirm");
        ArgumentCaptor<PaymentIntent> captor = ArgumentCaptor.forClass(PaymentIntent.class);
        verify(paymentGatewayService).initiate(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo("XAF");
        assertThat(captor.getValue().getPurpose()).isEqualTo(PaymentIntent.Purpose.DIME);
        assertThat(captor.getValue().getPhoneNumber()).isEqualTo("24160000000");
    }

    // ==================== #PRESENCE (flash) ====================

    @Test
    void presenceUnknownNumberIsRefused() {
        lowBandEnabled(true);
        when(personRepository.findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());
        assertThat(service.handleWhatsAppCommand(tenantId, phone, "#presence"))
                .contains("non reconnu");
    }

    @Test
    void presenceFlashRegistersAttendanceOnce() {
        lowBandEnabled(true);
        Person p = person();
        when(personRepository.findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(tenantId, "24160000000"))
                .thenReturn(Optional.of(p));
        when(eventRepository.findByTenantIdAndDeletedAtIsNullAndDateDebutBetween(any(), any(), any()))
                .thenReturn(List.of(publishedEvent()));
        when(eventAttendanceRepository.findByChurchEventIdAndPersonId(any(), any()))
                .thenReturn(Optional.empty());

        String reply = service.handleWhatsAppCommand(tenantId, phone, "#presence");

        assertThat(reply).contains("Présence confirmée").contains("Culte de louange");
        ArgumentCaptor<EventAttendance> captor = ArgumentCaptor.forClass(EventAttendance.class);
        verify(eventAttendanceRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PRESENT");
        assertThat(captor.getValue().getCheckInMethod()).isEqualTo("LOW_BAND");
        assertThat(captor.getValue().getPersonId()).isEqualTo(p.getId());
    }

    @Test
    void presenceFlashIsIdempotent() {
        lowBandEnabled(true);
        Person p = person();
        when(personRepository.findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(tenantId, "24160000000"))
                .thenReturn(Optional.of(p));
        when(eventRepository.findByTenantIdAndDeletedAtIsNullAndDateDebutBetween(any(), any(), any()))
                .thenReturn(List.of(publishedEvent()));
        when(eventAttendanceRepository.findByChurchEventIdAndPersonId(any(), any()))
                .thenReturn(Optional.of(new EventAttendance()));

        assertThat(service.handleWhatsAppCommand(tenantId, phone, "#presence"))
                .contains("déjà enregistrée");
        verify(eventAttendanceRepository, never()).save(any());
    }

    // ==================== Résolution USSD multi-tenants ====================

    @Test
    void resolvesTenantWhenNumberInSingleDirectory() {
        Person p = person();
        when(personRepository.findLowBandByPhoneVariants(any())).thenReturn(List.of(p));
        assertThat(service.resolveTenantByPhone(phone)).isEqualTo(tenantId);
    }

    @Test
    void refusesAmbiguousNumberAcrossTenants() {
        Person inAnother = person();
        inAnother.setTenantId(UUID.randomUUID());
        when(personRepository.findLowBandByPhoneVariants(any())).thenReturn(List.of(person(), inAnother));
        assertThat(service.resolveTenantByPhone(phone)).isNull();
    }

    // ==================== Prière USSD : jamais de perte silencieuse ====================

    @Test
    void ussdPrayerTooShortIsRejectedWithoutJournaling() {
        lowBandEnabled(true);
        assertThat(service.ussdPrayer(tenantId, phone, "abc")).contains("trop courte");
        verify(interactionRepository, never()).save(any());
    }

    @Test
    void ussdPrayerAlwaysJournalsEvenWithoutAccount() {
        lowBandEnabled(true);
        when(personRepository.findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());
        assertThat(service.ussdPrayer(tenantId, phone, "Merci de prier pour ma guérison"))
                .contains("enregistree");
        verify(interactionRepository).save(any());
        verify(prayerRepository, never()).save(any());
    }

    // ==================== Divers ====================

    @Test
    void unknownCommandFallsThroughToLegacyHandler() {
        lowBandEnabled(true);
        assertThat(service.handleWhatsAppCommand(tenantId, phone, "#rejoindre famille Paul")).isNull();
    }

    @Test
    void journalingNeverBreaksTheMainFlow() {
        lowBandEnabled(true);
        when(interactionRepository.save(any())).thenThrow(new RuntimeException("db down"));
        assertThatCode(() -> service.journal(tenantId, "USSD", phone, "TEST", "detail"))
                .doesNotThrowAnyException();
    }

    @Test
    void ussdResponsesStayWithinSmsLength() {
        lowBandEnabled(true);
        when(dressCodeRepository.findByTenantIdAndArchivedFalseAndBeginsAtAfterOrderByBeginsAtAsc(any(), any()))
                .thenReturn(java.util.List.of());
        when(notificationRepository.findByDestinataireIdAndLuFalseOrderByCreatedAtDesc(any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        assertThat(service.ussdDressCode(tenantId)).hasSizeLessThanOrEqualTo(180);
    }
}
