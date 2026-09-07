package com.discipolat.modules.ussd.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
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

    public UssdService(UssdSessionRepository sessionRepository,
                       UssdProperties properties,
                       PaymentGatewayService paymentGatewayService,
                       SecurityUtils securityUtils) {
        this.sessionRepository = sessionRepository;
        this.properties = properties;
        this.paymentGatewayService = paymentGatewayService;
        this.securityUtils = securityUtils;
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
                            .tenantId(resolveTenantFromServiceCode(serviceCode))
                            .currentMenu("main")
                            .step(0)
                            .ended(false)
                            .build();
                    return sessionRepository.save(newSession);
                });
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
            case "account" -> handleAccountMenu(session, text);
            default -> {
                session.setCurrentMenu("main");
                session.setStep(0);
                sessionRepository.save(session);
                yield showMainMenu();
            }
        };
    }

    private String handleMainMenu(UssdSession session, String text) {
        if (text.isEmpty()) return showMainMenu();
        return switch (text) {
            case "1" -> {
                session.setCurrentMenu("giving");
                session.setStep(1);
                sessionRepository.save(session);
                yield showGivingMenu();
            }
            case "2" -> {
                session.setCurrentMenu("prayer");
                session.setStep(1);
                sessionRepository.save(session);
                yield showPrayerPrompt();
            }
            case "3" -> {
                session.setCurrentMenu("events");
                session.setStep(1);
                sessionRepository.save(session);
                yield showEvents();
            }
            case "4" -> {
                session.setCurrentMenu("account");
                session.setStep(1);
                sessionRepository.save(session);
                yield showAccount();
            }
            case "5" -> showContact();
            default -> "CON Choix invalide.\n" + showMainMenu();
        };
    }

    private String showMainMenu() {
        return "CON Bienvenue chez Discipolat\n" +
                "1. Dime & Offrande\n" +
                "2. Demande de priere\n" +
                "3. Evenements\n" +
                "4. Mon compte\n" +
                "5. Contact eglise";
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
        return "CON Confirmer:\nMontant: " + amount + " XOF\nOp: " + operator + "\nTel: " + phone + "\n1. Confirmer\n2. Annuler";
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
            String operator = extractFromJson(ctx, "operator");
            String amount = extractFromJson(ctx, "amount");
            String phone = extractFromJson(ctx, "phone");

            PaymentIntent intent = new PaymentIntent();
            intent.setOperator(PaymentIntent.Operator.valueOf(operator));
            intent.setAmount(new BigDecimal(amount));
            intent.setCurrency("XOF");
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
        log.info("[USSD] Demande de prière — tenant={}, phone={}", session.getTenantId(), session.getPhoneNumber());
        session.setEnded(true);
        sessionRepository.save(session);
        return "END Demande de priere recue!\nL'equipe prierera pour vous.\nQue Dieu vous benisse!";
    }

    private String showPrayerPrompt() {
        return "CON Demande de priere\nDecrivez votre besoin:\n(min 5 caracteres)\n0. Retour";
    }

    private String handleEventsMenu(UssdSession session, String text) {
        if ("0".equals(text)) {
            session.setCurrentMenu("main");
            session.setStep(0);
            sessionRepository.save(session);
            return showMainMenu();
        }
        return showEvents();
    }

    private String showEvents() {
        return "CON Prochains evenements:\n1. Culte dimanche 10h\n2. Reunion priere mardi\n3. Formation samedi\n0. Retour";
    }

    private String handleAccountMenu(UssdSession session, String text) {
        if ("0".equals(text)) {
            session.setCurrentMenu("main");
            session.setStep(0);
            sessionRepository.save(session);
            return showMainMenu();
        }
        return showAccount();
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
