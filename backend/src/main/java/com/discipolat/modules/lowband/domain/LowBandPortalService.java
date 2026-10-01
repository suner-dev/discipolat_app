package com.discipolat.modules.lowband.domain;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.dresscode.domain.DressCode;
import com.discipolat.modules.dresscode.domain.DressCodeRepository;
import com.discipolat.modules.dresscode.domain.DressCodeRule;
import com.discipolat.modules.dresscode.domain.DressCodeRuleRepository;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventAttendance;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.events.repository.EventAttendanceRepository;
import com.discipolat.modules.payments.domain.PaymentGatewayService;
import com.discipolat.modules.payments.domain.PaymentIntent;
import com.discipolat.modules.people.domain.Person;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.notifications.domain.NotificationRepository;
import com.discipolat.modules.prayers.domain.Prayer;
import com.discipolat.modules.prayers.domain.PrayerRepository;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.service.TenantSettingsService;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * §G5.9 — Portail basse connexion : commandes member-first (« ma tenue », « planning »,
 * « don », « présence flash ») partagées entre le webhook WhatsApp entrant et le menu
 * USSD. Aucune donnée sensible (pastoral, finance personnelle) ne transite par ces
 * canaux : uniquement le strict nécessaire au membre, vers son propre profil résolu
 * par numéro. Le tenant doit avoir {@code low_band_enabled} (toggle §G1.2).
 */
@Service
@Transactional
public class LowBandPortalService {

    private static final Logger log = LoggerFactory.getLogger(LowBandPortalService.class);

    private final TenantSettingsService tenantSettingsService;
    private final PersonRepository personRepository;
    private final DressCodeRepository dressCodeRepository;
    private final DressCodeRuleRepository dressCodeRuleRepository;
    private final EventRepository eventRepository;
    private final EventAttendanceRepository eventAttendanceRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final UserRepository userRepository;
    private final PrayerRepository prayerRepository;
    private final NotificationRepository notificationRepository;
    private final LowBandInteractionRepository interactionRepository;

    public LowBandPortalService(TenantSettingsService tenantSettingsService,
                                PersonRepository personRepository,
                                DressCodeRepository dressCodeRepository,
                                DressCodeRuleRepository dressCodeRuleRepository,
                                EventRepository eventRepository,
                                EventAttendanceRepository eventAttendanceRepository,
                                PaymentGatewayService paymentGatewayService,
                                UserRepository userRepository,
                                PrayerRepository prayerRepository,
                                NotificationRepository notificationRepository,
                                LowBandInteractionRepository interactionRepository) {
        this.tenantSettingsService = tenantSettingsService;
        this.personRepository = personRepository;
        this.dressCodeRepository = dressCodeRepository;
        this.dressCodeRuleRepository = dressCodeRuleRepository;
        this.eventRepository = eventRepository;
        this.eventAttendanceRepository = eventAttendanceRepository;
        this.paymentGatewayService = paymentGatewayService;
        this.userRepository = userRepository;
        this.prayerRepository = prayerRepository;
        this.notificationRepository = notificationRepository;
        this.interactionRepository = interactionRepository;
    }

    /** Toggle §G1.2 : le canal basse connexion est activé/désactivé par le tenant. */
    public boolean isLowBandEnabled(UUID tenantId) {
        try {
            TenantSettings settings = tenantSettingsService.getSettings(tenantId);
            return Boolean.TRUE.equals(settings.getLowBandEnabled());
        } catch (Exception e) {
            return false;
        }
    }

    private static final String DISABLED_REPLY =
            "Le portail basse connexion n'est pas activé pour cette église.";

    /**
     * Point d'entrée WhatsApp entrant (commandes prefixées #). Retourne la réponse
     * textuelle à renvoyer au membre, ou null si la commande n'est pas gérée ici
     * (les commandes famille/#rejoindre/#stop restent dans WhatsAppService).
     */
    public String handleWhatsAppCommand(UUID tenantId, String phone, String normalizedBody) {
        if (!isLowBandEnabled(tenantId)) return DISABLED_REPLY;
        String reply;
        String action;
        if (normalizedBody.startsWith("#tenue") || normalizedBody.startsWith("#ma")) {
            reply = dressCodeFor(tenantId, phone); action = "TENUE";
        } else if (normalizedBody.startsWith("#planning") || normalizedBody.startsWith("#evenements")) {
            reply = planningFor(tenantId, phone); action = "PLANNING";
        } else if (normalizedBody.startsWith("#don")) {
            reply = donationFor(tenantId, phone, normalizedBody); action = "DON";
        } else if (normalizedBody.startsWith("#presence")) {
            reply = presenceFor(tenantId, phone); action = "PRESENCE";
        } else if (normalizedBody.startsWith("#aide")) {
            reply = "Commandes membre :\n"
                    + "#tenue — ma tenue des prochains cultes\n"
                    + "#planning — les événements à venir\n"
                    + "#don 5000 — initier un don Mobile Money\n"
                    + "#presence — confirmer ma présence (flash)\n"
                    + "#rejoindre famille <nom> / #afamille / #stop — annonces famille";
            action = "AIDE";
        } else {
            return null;
        }
        journal(tenantId, "WHATSAPP", phone, action, normalizedBody);
        return reply;
    }

    // ==================== #TENUE ====================

    /** « Ma tenue ce dimanche » : prochains dress codes + règles du groupe de l'intéressé. */
    public String dressCodeFor(UUID tenantId, String phone) {
        Instant now = Instant.now();
        List<DressCode> upcoming = dressCodeRepository
                .findByTenantIdAndArchivedFalseAndBeginsAtAfterOrderByBeginsAtAsc(
                        tenantId, now.minusMillis(24L * 3600 * 1000));
        if (upcoming.isEmpty()) {
            return "Aucune tenue programmée pour le moment.";
        }
        StringBuilder sb = new StringBuilder("👕 Tenues à venir :\n");
        int shown = 0;
        for (DressCode dc : upcoming) {
            if (shown >= 3 || sb.length() > 900) break; // USSD/WhatsApp : message court
            String when = dc.getBeginsAt() != null
                    ? DateFmt.shortFr(dc.getBeginsAt()) : "—";
            sb.append("• ").append(when).append(" — ")
                    .append(dc.getTitle() != null ? dc.getTitle() : dc.getServiceName()).append("\n");
            shown++;
            // Minimalisme de divulgation : uniquement les règles publiques de la
            // tenue ; jamais d'affectation personnelle déduite du numéro (le binding
            // SIM frauduleux ne doit pas révéler un groupe).
            for (DressCodeRule r : dressCodeRuleRepository.findByDressCodeId(dc.getId())) {
                if (r.getGroupName() != null) {
                    sb.append("   ").append(r.getGroupName()).append(": ")
                            .append(r.getDescription() != null ? r.getDescription() : "").append("\n");
                }
                if (sb.length() > 900) break;
            }
        }
        return sb.toString().trim();
    }

    // ==================== #PLANNING ====================

    /** « Planning du jour » : 5 prochains événements publics du tenant. */
    public String planningFor(UUID tenantId, String phone) {
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime to = from.plusDays(7);
        // Arbitrage D1 de main : la table vivante `event` (entite Event) est
        // l'unique source — le portail basse connexion n'a pas de doublon
        // « ChurchEvent » a interroger.
        List<Event> events = eventRepository
                .findByTenantIdAndDeletedAtIsNullAndDateDebutBetween(
                        tenantId, from.toLocalDateTime(), to.toLocalDateTime());
        if (events.isEmpty()) {
            return "Aucun événement dans les 7 prochains jours.";
        }
        StringBuilder sb = new StringBuilder("📅 Prochains événements :\n");
        int shown = 0;
        for (Event e : events) {
            String statut = e.getStatut();
            if (statut == null
                    || Event.STATUT_ANNULE.equals(statut)
                    || Event.STATUT_TERMINE.equals(statut)) continue;
            sb.append("• ").append(DateFmt.shortFr(e.getDateDebut().atOffset(ZoneOffset.UTC)))
                    .append(" — ").append(e.getTitre()).append("\n");
            if (++shown >= 5 || sb.length() > 900) break;
        }
        return shown == 0 ? "Aucun événement dans les 7 prochains jours." : sb.toString().trim();
    }

    // ==================== #DON ====================

    /** « Don 1 000 » : initiation Mobile Money sécurisée au nom du membre résolu. */
    public String donationFor(UUID tenantId, String phone, String body) {
        String amountPart = body.replaceAll("[^0-9 ]", "").trim();
        if (amountPart.isEmpty()) {
            return "Usage : #don 5000 (montant en FCFA).";
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountPart.split("\\s+")[0]);
        } catch (NumberFormatException ex) {
            return "Montant invalide. Usage : #don 5000";
        }
        if (amount.compareTo(BigDecimal.ONE) < 0 || amount.compareTo(new BigDecimal("1000000")) > 0) {
            return "Montant hors limites (1 – 1 000 000).";
        }
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() < 8) {
            return "Numéro non reconnu.";
        }
        PaymentIntent intent = new PaymentIntent();
        intent.setAmount(amount);
        intent.setCurrency("XAF");
        intent.setPhoneNumber(cleanPhone);
        intent.setPurpose(PaymentIntent.Purpose.DIME);
        intent.setTenantId(tenantId);
        try {
            PaymentIntent saved = paymentGatewayService.initiate(intent);
            String link = saved.getCheckoutUrl();
            return "🙏 Don de " + amount + " FCFA initié.\n"
                    + (link != null && !link.isBlank()
                        ? "Confirmez ici : " + link
                        : "Vous allez recevoir une demande de confirmation Mobile Money.");
        } catch (Exception e) {
            log.warn("[G5.9] Initiation don échouée (tenant={}, phone={}): {}",
                    tenantId, cleanPhone, e.getMessage());
            return "Le don n'a pas pu être initié pour le moment. Réessayez plus tard.";
        }
    }

    // ==================== #PRESENCE ====================

    /**
     * « Ma présence » (flash) : confirme la présence du membre au culte en cours ou
     * au prochain événement publié. Résolution par téléphone normalisé vers la fiche
     * Person du tenant — aucune donnée d'autrui n'est exposée.
     */
    public String presenceFor(UUID tenantId, String phone) {
        Optional<Person> person = resolvePerson(tenantId, phone);
        if (person.isEmpty()) {
            return "Numéro non reconnu dans cette église. Contactez votre responsable.";
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<Event> upcoming = eventRepository
                .findByTenantIdAndDeletedAtIsNullAndDateDebutBetween(
                        tenantId, now.minusHours(6).toLocalDateTime(), now.plusDays(2).toLocalDateTime());
        Event target = null;
        for (Event e : upcoming) {
            String statut = e.getStatut();
            if (Event.STATUT_PLANIFIE.equals(statut) || "EN_COURS".equals(statut)) {
                target = e;
                break;
            }
        }
        if (target == null) {
            return "Aucun culte/événement en cours ou à moins de 48 h.";
        }
        String pId = person.get().getId().toString();
        var existing = eventAttendanceRepository
                .findByChurchEventIdAndPersonId(target.getId(), person.get().getId());
        if (existing.isPresent()) {
            return "✅ Présence déjà enregistrée pour « " + target.getTitre() + " ».";
        }
        EventAttendance attendance = new EventAttendance();
        attendance.setTenantId(tenantId);
        attendance.setChurchEventId(target.getId());
        attendance.setPersonId(person.get().getId());
        attendance.setStatus("PRESENT");
        attendance.setCheckInAt(now);
        attendance.setCheckInMethod("LOW_BAND");
        eventAttendanceRepository.save(attendance);
        log.info("[G5.9] Présence flash — tenant={}, person={}, event={}",
                tenantId, pId, target.getId());
        return "✅ Présence confirmée pour « " + target.getTitre() + " ».";
    }

    // ==================== USSD (menu §G5.9) ====================

    /**
     * §G5.9 — Résolution du tenant depuis le numéro appelant (point d'entrée USSD
     * unique, partagé par toutes les églises). Ambiguïté refusée : un numéro présent
     * dans plusieurs annuaires ne reçoit AUCUNE donnée (sécurité > commodité).
     */
    public UUID resolveTenantByPhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        List<String> variants = phoneVariants(phone);
        List<Person> matches = personRepository.findLowBandByPhoneVariants(variants);
        UUID tenant = null;
        for (Person p : matches) {
            if (tenant != null && !tenant.equals(p.getTenantId())) {
                log.info("[G5.9] Numéro ambigu (plusieurs tenants) — refus de résolution");
                return null;
            }
            tenant = p.getTenantId();
        }
        return tenant;
    }

    private static List<String> phoneVariants(String phone) {
        String clean = phone.replaceAll("[^0-9+]", "");
        String digits = clean.replaceAll("\\+", "");
        java.util.LinkedHashSet<String> v = new java.util.LinkedHashSet<>();
        v.add(clean);
        v.add(digits);
        if (digits.startsWith("00")) v.add(digits.substring(2));
        if (digits.length() > 10) v.add(digits.substring(digits.length() - 10));
        v.add("+" + digits);
        return new ArrayList<>(v);
    }

    /** Réponse USSD « Ma tenue » (menu 2) — texte court, < 182 caractères. */
    public String ussdDressCode(UUID tenantId) {
        if (!isLowBandEnabled(tenantId)) return DISABLED_REPLY;
        return truncateUssd(dressCodeFor(tenantId, null));
    }

    public String ussdPlanning(UUID tenantId) {
        if (!isLowBandEnabled(tenantId)) return DISABLED_REPLY;
        return truncateUssd(planningFor(tenantId, null));
    }

    public String ussdPresence(UUID tenantId, String phone) {
        if (!isLowBandEnabled(tenantId)) return truncateUssd(DISABLED_REPLY);
        String reply = presenceFor(tenantId, phone);
        journal(tenantId, "USSD", phone, "PRESENCE", reply);
        return truncateUssd(reply);
    }

    /**
     * « Demande de prière » USSD : JAMAIS de perte silencieuse — la demande est
     * toujours journalisée (lowband_interaction) ; si le numéro correspond à un
     * compte, une vraie Prayer est créée dans le module prière.
     */
    public String ussdPrayer(UUID tenantId, String phone, String text) {
        if (!isLowBandEnabled(tenantId)) return truncateUssd(DISABLED_REPLY);
        if (text == null || text.trim().length() < 5) {
            return "Demande trop courte (min 5 caracteres).";
        }
        String trimmed = text.trim();
        boolean persistedPrayer = false;
        try {
            Optional<Person> person = resolvePerson(tenantId, phone);
            if (person.isPresent() && person.get().getEmailNormalized() != null) {
                Optional<com.discipolat.modules.users.domain.User> user =
                        userRepository.findByTenantIdAndEmailIgnoreCase(
                                tenantId, person.get().getEmailNormalized());
                if (user.isPresent()) {
                    Prayer prayer = new Prayer();
                    prayer.setTenantId(tenantId);
                    prayer.setAuteurId(user.get().getId());
                    prayer.setTitre(trimmed.length() > 90 ? trimmed.substring(0, 90) + "…" : trimmed);
                    prayer.setDescription("Demande recue par USSD — " + phone);
                    prayer.setCategorie("INTERCESSION");
                    prayer.setPriorite("MOYENNE");
                    prayerRepository.save(prayer);
                    persistedPrayer = true;
                }
            }
        } catch (Exception e) {
            log.warn("[G5.9] Prière USSD non convertie en Prayer : {}", e.getMessage());
        }
        journal(tenantId, "USSD", phone, "PRIERE", trimmed);
        return persistedPrayer
                ? "Demande de priere transmise a l'equipe. Que Dieu vous benisse !"
                : "Demande enregistree. Un intercedant vous recontactera.";
    }

    /** « Notifications » USSD : 3 dernières non lues du compte lié au numéro. */
    public String ussdNotifications(UUID tenantId, String phone) {
        if (!isLowBandEnabled(tenantId)) return truncateUssd(DISABLED_REPLY);
        Optional<com.discipolat.modules.users.domain.User> user = resolveUser(tenantId, phone);
        if (user.isEmpty()) return "Aucun compte lie a ce numero.";
        var page = notificationRepository.findByDestinataireIdAndLuFalseOrderByCreatedAtDesc(
                user.get().getId(), org.springframework.data.domain.PageRequest.of(0, 3));
        if (page.isEmpty()) return "Aucune notification en attente.";
        StringBuilder sb = new StringBuilder("Notifications :\n");
        page.forEach(n -> {
            sb.append("• ").append(n.getTitre() != null ? n.getTitre() : n.getType()).append("\n");
        });
        journal(tenantId, "USSD", phone, "NOTIFICATIONS", String.valueOf(page.getTotalElements()));
        return truncateUssd(sb.toString().trim());
    }

    private Optional<com.discipolat.modules.users.domain.User> resolveUser(UUID tenantId, String phone) {
        return resolvePerson(tenantId, phone)
                .filter(p -> p.getEmailNormalized() != null)
                // Resolution scopee tenant : findByEmail (supprime dans main) serait
                // sensible a la casse et non scope.
                .flatMap(p -> userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, p.getEmailNormalized()));
    }

    /** §G5.9 — journal d'interactions (traçabilité complète, jamais bloquant). */
    public void journal(UUID tenantId, String channel, String phone, String action, String detail) {
        try {
            LowBandInteraction i = new LowBandInteraction();
            i.setTenantId(tenantId);
            i.setChannel(channel);
            i.setPhone(phone != null ? phone.replaceAll("[^0-9+]", "") : "inconnu");
            i.setAction(action);
            i.setDetail(detail != null && detail.length() > 1000 ? detail.substring(0, 1000) : detail);
            interactionRepository.save(i);
        } catch (Exception e) {
            log.warn("[G5.9] Journal d'interaction échoué : {}", e.getMessage());
        }
    }

    private static String truncateUssd(String s) {
        if (s == null) return "";
        return s.length() <= 180 ? s : s.substring(0, 177) + "...";
    }

    private Optional<Person> resolvePerson(UUID tenantId, String phone) {
        if (phone == null || phone.isBlank()) return Optional.empty();
        String clean = phone.replaceAll("[^0-9]", "");
        // Les numéros sont stockés normalisés avec indicatif pays ; on tente les
        // variantes 10 chiffres locaux et le numéro tel quel.
        Optional<Person> direct = personRepository.findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(tenantId, clean);
        if (direct.isPresent()) return direct;
        if (clean.length() > 10) {
            String local = clean.substring(clean.length() - 10);
            direct = personRepository.findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(tenantId, local);
            if (direct.isPresent()) return direct;
        }
        return Optional.empty();
    }

    /** Formatage court localisé, sans dépendance ICU. */
    static final class DateFmt {
        private static final String[] DAYS = {"dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."};
        private static final String[] MONTHS = {"janv.", "févr.", "mars", "avr.", "mai", "juin",
                "juil.", "août", "sept.", "oct.", "nov.", "déc."};

        static String shortFr(Instant instant) {
            OffsetDateTime odt = instant.atOffset(ZoneOffset.UTC);
            return shortFr(odt);
        }

        static String shortFr(OffsetDateTime odt) {
            return DAYS[odt.getDayOfWeek().getValue() % 7] + " "
                    + odt.getDayOfMonth() + " " + MONTHS[odt.getMonthValue() - 1]
                    + " " + odt.getHour() + "h";
        }
    }
}
