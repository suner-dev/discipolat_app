package com.discipolat.modules.ussd.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.currency.domain.CurrencyConfigRepository;
import com.discipolat.modules.payments.domain.PaymentGatewayService;
import com.discipolat.modules.payments.domain.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class UssdService {

    private static final Logger log = LoggerFactory.getLogger(UssdService.class);

    private final UssdSessionRepository sessionRepository;
    private final UssdProperties properties;
    private final PaymentGatewayService paymentGatewayService;
    private final SecurityUtils securityUtils;
    private final CurrencyConfigRepository currencyConfigRepository;
    /** §G5.9 — portail basse connexion : tenue/planning/présence/prière RÉELS. */
    private final org.springframework.beans.factory.ObjectProvider<com.discipolat.modules.lowband.domain.LowBandPortalService> lowBandProvider;

    public UssdService(UssdSessionRepository sessionRepository,
                       UssdProperties properties,
                       PaymentGatewayService paymentGatewayService,
                       SecurityUtils securityUtils,
                       CurrencyConfigRepository currencyConfigRepository,
                       org.springframework.beans.factory.ObjectProvider<com.discipolat.modules.lowband.domain.LowBandPortalService> lowBandProvider) {
        this.sessionRepository = sessionRepository;
        this.properties = properties;
        this.paymentGatewayService = paymentGatewayService;
        this.securityUtils = securityUtils;
        this.currencyConfigRepository = currencyConfigRepository;
        this.lowBandProvider = lowBandProvider;
    }

    public String handleUssdCallback(String sessionId, String phoneNumber, String text, String serviceCode) {
        if (!properties.isConfigured()) {
            return "END Service USSD non configuré. Contactez votre église.";
        }
        String cleanText = text != null ? text.trim().replaceAll("[*#]", "") : "";
        UssdSession session = findOrCreateSession(sessionId, phoneNumber, serviceCode);
        return processMenuInput(session, cleanText);
    }

    private UssdSession findOrCreateSession(String sessionId, String phoneNumber, String serviceCode) {
        return sessionRepository.findBySessionIdAndEndedFalse(sessionId)
                .orElseGet(() -> {
                    log.info("[USSD] Nouvelle session — phone={}, service={}", phoneNumber, serviceCode);
                    UssdSession newSession = UssdSession.builder()
                            .sessionId(sessionId)
                            .phoneNumber(phoneNumber)
                            .tenantId(resolveTenant(phoneNumber, serviceCode))
                            .currentMenu("main")
                            .step(0)
                            .ended(false)
                            .build();
                    return sessionRepository.save(newSession);
                });
    }

    /**
     * A3 (M9) — Devise du tenant pour le canal USSD : devise primaire déclarée,
     * à défaut XOF (défaut historique des marchés opérateurs intégrés). Le tenant
     * configure sa devise, l'affichage et l'intention de paiement suivent.
     */
    private String deviseDuTenant(String tenantId) {
        try {
            if (tenantId != null) {
                return currencyConfigRepository
                        .findByTenantIdAndIsPrimaryTrue(UUID.fromString(tenantId))
                        .map(com.discipolat.modules.currency.domain.CurrencyConfig::getCurrencyCode)
                        .orElse("XOF");
            }
        } catch (RuntimeException malformedOrUnreachable) {
            // UUID invalide ou repository indisponible : défaut historique assumé.
        }
        return "XOF";
    }

    /**
     * §G5.9 — Résolution RÉELLE du tenant : le point d'entrée USSD est partagé,
     * c'est le NUMÉRO APPELANT qui identifie l'église (fiche Person du répertoire,
     * ambiguïté multi-tenants refusée). L'ancien fallback stockait le serviceCode
     * dans tenantId → UUID.fromString échouait plus tard (paiement « erreur »).
     * Sans résolution par numéro, on retombe sur le comportement de main.
     */
    private String resolveTenant(String phoneNumber, String serviceCode) {
        var portal = lowBandProvider.getIfAvailable();
        if (portal != null) {
            java.util.UUID tenant = portal.resolveTenantByPhone(phoneNumber);
            if (tenant != null) return tenant.toString();
        }
        return resolveTenantFromServiceCode(serviceCode);
    }

    private String resolveTenantFromServiceCode(String serviceCode) {
        try {
            return securityUtils.getCurrentTenantId().toString();
        } catch (Exception e) {
            return serviceCode != null ? serviceCode : "default";
        }
    }

    private String processMenuInput(UssdSession session, String text) {
        return switch (session.getCurrentMenu()) {
            case "main" -> handleMainMenu(session, text);
            case "giving" -> handleGivingMenu(session, text);
            case "giving_amount" -> handleGivingAmount(session, text);
            case "giving_phone" -> handleGivingPhone(session, text);
            case "giving_confirm" -> handleGivingConfirm(session, text);
            case "prayer" -> handlePrayerMenu(session, text);
            case "events" -> handleEventsMenu(session, text);
            case "dresscode" -> handleDressCodeMenu(session, text);
            case "presence" -> handlePresenceMenu(session, text);
            case "notifications" -> handleNotificationsMenu(session, text);
            case "more" -> handleMoreMenu(session, text);
            case "account" -> handleAccountMenu(session, text);
            default -> {
                session.setCurrentMenu("main");
                session.setStep(0);
                sessionRepository.save(session);
                yield showMainMenu();
            }
        };
    }

    /** §G5.9 — menu principal du contrat : 1-Événements · 2-Ma tenue · 3-Présence · 4-Dons · 5-Plus. */
    private String handleMainMenu(UssdSession session, String text) {
        if (text.isEmpty()) return showMainMenu();
        return switch (text) {
            case "1" -> {
                session.setCurrentMenu("events");
                session.setStep(1);
                sessionRepository.save(session);
                yield showEvents(session);
            }
            case "2" -> {
                session.setCurrentMenu("dresscode");
                session.setStep(1);
                sessionRepository.save(session);
                yield showDressCode(session);
            }
            case "3" -> {
                session.setCurrentMenu("presence");
                session.setStep(1);
                sessionRepository.save(session);
                yield showPresence(session);
            }
            case "4" -> {
                session.setCurrentMenu("giving");
                session.setStep(1);
                sessionRepository.save(session);
                yield showGivingMenu();
            }
            case "5" -> {
                session.setCurrentMenu("more");
                session.setStep(1);
                sessionRepository.save(session);
                yield showMoreMenu();
            }
            default -> "CON Choix invalide.\n" + showMainMenu();
        };
    }

    private String showMainMenu() {
        return "CON Discipolat\n" +
                "1. Evenements\n" +
                "2. Ma tenue\n" +
                "3. Presence (flash)\n" +
                "4. Dime & Offrande\n" +
                "5. Plus (priere, notifications, compte)";
    }

    private String handleMoreMenu(UssdSession session, String text) {
        if (text.isEmpty()) return showMoreMenu();
        return switch (text) {
            case "1" -> {
                session.setCurrentMenu("prayer");
                session.setStep(1);
                sessionRepository.save(session);
                yield showPrayerPrompt();
            }
            case "2" -> {
                session.setCurrentMenu("notifications");
                session.setStep(1);
                sessionRepository.save(session);
                yield showNotifications(session);
            }
            case "3" -> {
                session.setCurrentMenu("account");
                session.setStep(1);
                sessionRepository.save(session);
                yield showAccount();
            }
            case "0" -> backToMain(session);
            default -> "CON Choix invalide.\n" + showMoreMenu();
        };
    }

    private String showMoreMenu() {
        return "CON Plus:\n1. Demande de priere\n2. Notifications\n3. Mon compte\n0. Retour";
    }

    private String backToMain(UssdSession session) {
        session.setCurrentMenu("main");
        session.setStep(0);
        sessionRepository.save(session);
        return showMainMenu();
    }

    private String handleGivingMenu(UssdSession session, String text) {
        if (text.isEmpty()) return showGivingMenu();
        String operator = switch (text) {
            case "1" -> "ORANGE_MONEY";
            case "2" -> "MTN_MOMO";
            case "3" -> "M_PESA";
            case "4" -> "WAVE";
            default -> null;
        };
        if (operator == null) return "CON Choix invalide.\n" + showGivingMenu();
        session.setCurrentMenu("giving_amount");
        session.setStep(2);
        session.setContextData("{\"operator\":\"" + operator + "\"}");
        sessionRepository.save(session);
        return "CON Montant (ex: 5000):\n0. Retour";
    }

    private String showGivingMenu() {
        return "CON Choisissez operateur:\n1. Orange Money\n2. MTN MoMo\n3. M-Pesa\n4. Wave\n0. Retour";
    }

    private String handleGivingAmount(UssdSession session, String text) {
        if ("0".equals(text)) {
            session.setCurrentMenu("main");
            session.setStep(0);
            sessionRepository.save(session);
            return showMainMenu();
        }
        if (text.isEmpty()) return "CON Montant (ex: 5000):\n0. Retour";

        BigDecimal amount;
        try {
            amount = new BigDecimal(text);
            if (amount.compareTo(BigDecimal.ONE) < 0 || amount.compareTo(new BigDecimal("1000000")) > 0)
                return "CON Montant invalide (1-1000000).\nReessayez:";
        } catch (NumberFormatException e) {
            return "CON Montant invalide.\nReessayez:";
        }

        String ctx = session.getContextData();
        String newCtx = ctx != null ? ctx.replace("}", ",\"amount\":\"" + amount + "\"}")
                : "{\"amount\":\"" + amount + "\"}";
        session.setContextData(newCtx);
        session.setCurrentMenu("giving_phone");
        session.setStep(3);
        sessionRepository.save(session);
        return "CON Numero de telephone:\n(ex: 0712345678)\n0. Retour";
    }

    private String handleGivingPhone(UssdSession session, String text) {
        if ("0".equals(text)) {
            session.setCurrentMenu("giving");
            session.setStep(1);
            sessionRepository.save(session);
            return showGivingMenu();
        }
        if (text.isEmpty()) return "CON Numero de telephone:\n(ex: 0712345678)\n0. Retour";

        String phone = text.replaceAll("\\s+", "");
        if (phone.length() < 8 || phone.length() > 15)
            return "CON Numero invalide.\nReessayez:";

        String ctx = session.getContextData();
        String newCtx = ctx != null ? ctx.replace("}", ",\"phone\":\"" + phone + "\"}")
                : "{\"phone\":\"" + phone + "\"}";
        session.setContextData(newCtx);
        session.setCurrentMenu("giving_confirm");
        session.setStep(4);
        sessionRepository.save(session);

        String operator = extractFromJson(ctx, "operator");
        String amount = extractFromJson(ctx, "amount");
        return "CON Confirmer:\nMontant: " + amount + " " + deviseDuTenant(session.getTenantId())
                + "\nOp: " + operator + "\nTel: " + phone + "\n1. Confirmer\n2. Annuler";
    }

    private String handleGivingConfirm(UssdSession session, String text) {
        if ("2".equals(text)) {
            session.setEnded(true);
            sessionRepository.save(session);
            return "END Paiement annule. Merci!";
        }
        if (!"1".equals(text))
            return "CON Choix invalide.\n1. Confirmer\n2. Annuler";

        try {
            String ctx = session.getContextData();
            // §G5.9 — tenant résolu par le numéro appelant ; sans église reconnue,
            // aucun débit n'est initié (l'ancien code produisait une erreur opaque).
            if (!isUuid(session.getTenantId())) {
                session.setEnded(true);
                sessionRepository.save(session);
                return "END Numero non enregistre dans une eglise.\nInscrivez-vous via l'app ou votre responsable.";
            }
            String operator = extractFromJson(ctx, "operator");
            String amount = extractFromJson(ctx, "amount");
            String phone = extractFromJson(ctx, "phone");

            PaymentIntent intent = new PaymentIntent();
            intent.setOperator(PaymentIntent.Operator.valueOf(operator));
            intent.setAmount(new BigDecimal(amount));
            // A3 (M9) — la devise affichée/saisie est celle du tenant, pas un XOF
            // figé : un canal USSD kényan (M-Pesa) ne peut pas facturer en francs CFA.
            intent.setCurrency(deviseDuTenant(session.getTenantId()));
            intent.setPhoneNumber(phone);
            intent.setPurpose(PaymentIntent.Purpose.DIME);
            intent.setTenantId(UUID.fromString(session.getTenantId()));

            PaymentIntent saved = paymentGatewayService.initiate(intent);
            session.setEnded(true);
            sessionRepository.save(session);

            return "END Paiement initie!\nRef: " + saved.getProviderReference() + "\nConfirmez sur votre telephone.\nMerci!";
        } catch (Exception e) {
            log.error("[USSD] Erreur initiation paiement", e);
            session.setEnded(true);
            sessionRepository.save(session);
            return "END Erreur lors du paiement.\nReessayez plus tard.";
        }
    }

    private String handlePrayerMenu(UssdSession session, String text) {
        if (text.isEmpty()) return showPrayerPrompt();
        if (text.length() < 5) return "CON Demande trop courte.\nDecrivez votre besoin:";
        // §G5.9 — la demande est RÉELLEMENT persistée (Prayer si compte lié,
        // journal lowband_interaction toujours) — plus de simple log().
        var portal = lowBandProvider.getIfAvailable();
        String reply = portal != null
                ? portal.ussdPrayer(tenantUuid(session), session.getPhoneNumber(), text)
                : "END Service indisponible. Reessayez plus tard.";
        session.setEnded(true);
        sessionRepository.save(session);
        return "END " + reply;
    }

    private String showPrayerPrompt() {
        return "CON Demande de priere\nDecrivez votre besoin:\n(min 5 caracteres)\n0. Retour";
    }

    private String handleEventsMenu(UssdSession session, String text) {
        if ("0".equals(text)) return backToMain(session);
        return showEvents(session);
    }

    /** §G5.9 — événements RÉELS du calendrier (7 prochains jours), plus de liste figée. */
    private String showEvents(UssdSession session) {
        var portal = lowBandProvider.getIfAvailable();
        if (portal == null || !isUuid(session.getTenantId())) return "CON Service indisponible.";
        return "CON " + portal.ussdPlanning(java.util.UUID.fromString(session.getTenantId())) + "\n0. Retour";
    }

    private String handleDressCodeMenu(UssdSession session, String text) {
        if ("0".equals(text)) return backToMain(session);
        return showDressCode(session);
    }

    private String showDressCode(UssdSession session) {
        var portal = lowBandProvider.getIfAvailable();
        if (portal == null || !isUuid(session.getTenantId())) return "CON Service indisponible.";
        return "CON " + portal.ussdDressCode(java.util.UUID.fromString(session.getTenantId())) + "\n0. Retour";
    }

    private String handlePresenceMenu(UssdSession session, String text) {
        if ("0".equals(text)) return backToMain(session);
        return showPresence(session);
    }

    /** « Présence flash » : confirmation au culte en cours / prochain, par numéro. */
    private String showPresence(UssdSession session) {
        var portal = lowBandProvider.getIfAvailable();
        if (portal == null || !isUuid(session.getTenantId())) return "END Service indisponible.";
        String reply = portal.ussdPresence(java.util.UUID.fromString(session.getTenantId()),
                session.getPhoneNumber());
        session.setEnded(true);
        sessionRepository.save(session);
        return "END " + reply;
    }

    private String handleNotificationsMenu(UssdSession session, String text) {
        if ("0".equals(text)) return backToMain(session);
        return showNotifications(session);
    }

    private String showNotifications(UssdSession session) {
        var portal = lowBandProvider.getIfAvailable();
        if (portal == null || !isUuid(session.getTenantId())) return "CON Service indisponible.";
        return "CON " + portal.ussdNotifications(java.util.UUID.fromString(session.getTenantId()),
                session.getPhoneNumber()) + "\n0. Retour";
    }

    private static boolean isUuid(String s) {
        if (s == null) return false;
        try {
            java.util.UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private java.util.UUID tenantUuid(UssdSession session) {
        return isUuid(session.getTenantId()) ? java.util.UUID.fromString(session.getTenantId()) : null;
    }

    private String handleAccountMenu(UssdSession session, String text) {
        return switch (text) {
            case "1" -> "CON Mon compte\n1. Historique dons\n2. Info eglise\n0. Retour\n(Historique disponible dans l'app Discipolat)";
            case "2" -> showContact();
            case "0" -> backToMain(session);
            default -> "CON Mon compte\n1. Historique dons\n2. Info eglise\n0. Retour";
        };
    }

    private String showAccount() {
        return "CON Mon compte\n1. Historique dons\n2. Info eglise\n0. Retour";
    }

    private String showContact() {
        return "END Contact Eglise:\nTel: +225 07 00 00 00\nEmail: contact@discipolat.com\nDiscipolat";
    }

    private String extractFromJson(String json, String key) {
        if (json == null) return "";
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "";
        return json.substring(start, end);
    }

    @Transactional
    public void cleanupInactiveSessions() {
        int cleaned = sessionRepository.endInactiveSessions(java.time.LocalDateTime.now().minusMinutes(10));
        if (cleaned > 0) log.info("[USSD] {} sessions inactives terminées", cleaned);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(String tenantId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeSessions", sessionRepository.countByTenantIdAndEndedFalse(tenantId));
        stats.put("totalSessions", sessionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).size());
        return stats;
    }
}
