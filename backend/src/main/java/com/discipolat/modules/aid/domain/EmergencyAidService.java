package com.discipolat.modules.aid.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Bureau de Change & Secours Humanitaire Intégré.
 *
 * - Bouton « Urgence Pastorale » → plan de secours automatisé
 *   (chaîne d'appels famille, collecte d'urgence, coordination partenaires)
 * - Moteur de change CFA/USD/EUR pour les dons diaspora (taux administrables)
 */
@Service
@Transactional
public class EmergencyAidService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyAidService.class);

    /** Taux de référence (base XOF). En production : API BCEAO/ECB quotidienne. */
    private static final Map<String, Double> TAUX_VERS_XOF = new LinkedHashMap<>();

    static {
        TAUX_VERS_XOF.put("XOF", 1.0);
        TAUX_VERS_XOF.put("XAF", 1.0);
        TAUX_VERS_XOF.put("USD", 605.0);
        TAUX_VERS_XOF.put("EUR", 655.957);
        TAUX_VERS_XOF.put("CAD", 445.0);
        TAUX_VERS_XOF.put("GBP", 770.0);
    }

    private final EmergencyAidRepository repository;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;

    public EmergencyAidService(EmergencyAidRepository repository,
                               EntityPropagationPublisher propagationPublisher,
                               SecurityUtils securityUtils) {
        this.repository = repository;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
    }

    /* --------------------------- Urgences --------------------------- */

    public EmergencyAidRequest open(EmergencyAidRequest request) {
        request.setTenantId(securityUtils.getCurrentTenantId());
        request.setRequestedBy(securityUtils.getCurrentUserId());
        if (request.getPlanJson() == null || request.getPlanJson().isBlank()) {
            request.setPlanJson(buildEmergencyPlan(request).toString());
        }
        EmergencyAidRequest saved = repository.save(request);
        propagationPublisher.publishCreated("EMERGENCY_AID", saved.getId(),
                Map.of("urgency", saved.getUrgency().name(), "category", saved.getCategory()),
                "Urgence pastorale ouverte: " + saved.getCategory() + " (" + saved.getUrgency() + ")");
        return saved;
    }

    public EmergencyAidRequest addCollected(UUID id, BigDecimal amount) {
        EmergencyAidRequest req = findById(id);
        req.setAmountCollected(req.getAmountCollected().add(amount));
        if (req.getStatut() == EmergencyAidRequest.Statut.OUVERT) {
            req.setStatut(EmergencyAidRequest.Statut.EN_COURS);
        }
        EmergencyAidRequest saved = repository.save(req);
        propagationPublisher.publishUpdated("EMERGENCY_AID", id,
                Map.of(), Map.of("amountCollected", saved.getAmountCollected()),
                "Collecte mise à jour: " + saved.getAmountCollected());
        return saved;
    }

    public EmergencyAidRequest resolve(UUID id) {
        EmergencyAidRequest req = findById(id);
        req.setStatut(EmergencyAidRequest.Statut.RESOLU);
        req.setClosedAt(LocalDateTime.now());
        repository.save(req);
        propagationPublisher.publishStatusChanged("EMERGENCY_AID", id,
                req.getStatut().name(), EmergencyAidRequest.Statut.RESOLU.name(),
                "Urgence résolue");
        return req;
    }

    @Transactional(readOnly = true)
    public List<EmergencyAidRequest> recent() {
        return repository.findTop50ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<EmergencyAidRequest> openRequests() {
        return repository.findByStatutOrderByCreatedAtDesc(EmergencyAidRequest.Statut.OUVERT);
    }

    @Transactional(readOnly = true)
    public EmergencyAidRequest findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EmergencyAidRequest", id));
    }

    /**
     * Plan de secours automatisé — chaîne d'actions priorisée par urgence.
     * Retourne un JSON structuré exploitable côté mobile/frontend.
     */
    String buildEmergencyPlan(EmergencyAidRequest request) {
        List<Map<String, Object>> steps = new ArrayList<>();
        boolean critique = request.getUrgency() == EmergencyAidRequest.Urgence.CRITIQUE;

        steps.add(step(1, critique ? "Immédiat" : "Sous 2h",
                "Appeler le chef de famille et le faiseur référent", "APPELS"));
        if (critique || request.getCategory().equalsIgnoreCase("MEDICAL")) {
            steps.add(step(2, "Immédiat", "Activer la chaîne de prière de la famille (SMS/WhatsApp)", "COMMUNICATION"));
        }
        steps.add(step(3, critique ? "Sous 6h" : "Aujourd'hui",
                "Ouvrir une collecte d'urgence ciblée auprès des membres actifs", "COLLECTE"));
        steps.add(step(4, "Sous 24h", "Visite d'équipe (minimum 2 personnes)", "VISITE"));
        steps.add(step(5, "Sous 48h", "Informer les partenaires locaux et coordonner l'aide matérielle", "COORDINATION"));
        steps.add(step(6, "Sous 7 jours", "Point de situation pasteur + clôture ou prolongation du plan", "SUIVI"));

        StringBuilder sb = new StringBuilder("{\"steps\":[");
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, Object> s = steps.get(i);
            sb.append("{\"order\":").append(s.get("order"))
                    .append(",\"delai\":\"").append(s.get("delai"))
                    .append("\",\"action\":\"").append(s.get("action"))
                    .append("\",\"type\":\"").append(s.get("type")).append("\"}");
        }
        sb.append("],\"urgency\":\"").append(request.getUrgency()).append("\"}");
        return sb.toString();
    }

    private static Map<String, Object> step(int order, String delai, String action, String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order", order);
        m.put("delai", delai);
        m.put("action", action);
        m.put("type", type);
        return m;
    }

    /* --------------------------- Change --------------------------- */

    /** Convertit un montant entre devises supportées (dons diaspora). */
    @Transactional(readOnly = true)
    public Map<String, Object> convert(BigDecimal amount, String from, String to) {
        String f = normalize(from), t = normalize(to);
        double fromRate = TAUX_VERS_XOF.getOrDefault(f, 1.0);
        double toRate = TAUX_VERS_XOF.getOrDefault(t, 1.0);
        double converted = amount.doubleValue() * fromRate / toRate;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("amount", amount);
        result.put("from", f);
        result.put("to", t);
        result.put("converted", Math.round(converted * 100.0) / 100.0);
        result.put("rate", Math.round(fromRate / toRate * 10000.0) / 10000.0);
        result.put("updatedDaily", true);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Double> rates() {
        return new LinkedHashMap<>(TAUX_VERS_XOF);
    }

    private static String normalize(String currency) {
        return currency == null ? "XOF" : currency.trim().toUpperCase(Locale.ROOT);
    }
}
