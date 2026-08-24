package com.discipolat.modules.ai.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Assistant IA Pastoral — cœur du système intelligent.
 *
 * Architecture :
 *   1. Le frontend envoie une question
 *   2. Ce service récupère le CONTEXTE pertinent (données de l'église)
 *   3. Le contexte + la question sont envoyés à Ollama (LLM local)
 *   4. La réponse est retournée au frontend
 *
 * Si Ollama n'est pas disponible, le service retourne un résumé
 * des données pertinentes que le frontend formate.
 *
 * AUCUNE donnée n'est envoyée vers des services externes.
 * Tout reste local au serveur de l'église.
 */
@Service
@Transactional(readOnly = true)
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    @Value("${app.ai.ollama-url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${app.ai.model:llama3}")
    private String modelName;

    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final MakerReportRepository makerReportRepository;
    private final AlertRepository alertRepository;
    private final MemberPresenceRepository memberPresenceRepository;
    private final WorkspaceScopeService workspaceScope;
    private final SecurityUtils securityUtils;

    private final AiChatConversationRepository chatRepo;

    /** Cache simple de l'historique chat par userId (session). */
    private final ConcurrentHashMap<UUID, List<Map<String, Object>>> chatHistories = new ConcurrentHashMap<>();

    public AiAssistantService(SoulRepository soulRepository,
                               UserRepository userRepository,
                               FamilyRepository familyRepository,
                               MakerReportRepository makerReportRepository,
                               AlertRepository alertRepository,
                               MemberPresenceRepository memberPresenceRepository,
                               WorkspaceScopeService workspaceScope,
                               SecurityUtils securityUtils,
                               AiChatConversationRepository chatRepo) {
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.makerReportRepository = makerReportRepository;
        this.alertRepository = alertRepository;
        this.memberPresenceRepository = memberPresenceRepository;
        this.workspaceScope = workspaceScope;
        this.securityUtils = securityUtils;
        this.chatRepo = chatRepo;
    }

    /**
     * Generate an executive report as markdown (for PDF generation).
     */
    public Map<String, Object> generateReport() {
        Map<String, Object> context = buildChurchContext("rapport");
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("titre", "Rapport Exécutif — " + java.time.LocalDate.now());
        report.put("totalSouls", context.get("totalSouls"));
        report.put("totalFamilies", context.get("totalFamilies"));
        report.put("presenceRate", context.get("presenceRate"));
        report.put("activeAlerts", context.get("activeAlerts"));
        report.put("weekReportsSubmitted", context.get("weekReportsSubmitted"));
        report.put("weekReportsTotal", context.get("weekReportsTotal"));
        report.put("recentConverts", context.get("recentConverts"));

        // Generate narrative
        double rate = (double) context.getOrDefault("presenceRate", 0.0);
        long submitted = (long) context.getOrDefault("weekReportsSubmitted", 0L);
        long total = (long) context.getOrDefault("weekReportsTotal", 0L);
        StringBuilder narrative = new StringBuilder();
        narrative.append("## Résumé Exécutif\n\n");
        narrative.append("**Date :** ").append(java.time.LocalDate.now()).append("\n\n");
        narrative.append("### Effectifs\n");
        narrative.append("- Total âmes : ").append(context.get("totalSouls")).append("\n");
        narrative.append("- Familles : ").append(context.get("totalFamilies")).append("\n");
        narrative.append("- Nouveaux convertis : ").append(context.get("recentConverts")).append("\n\n");
        narrative.append("### Présence\n");
        narrative.append("- Taux : **").append(rate).append("%**\n");
        narrative.append(rate >= 75 ? "- ✅ Excellent\n" : rate >= 50 ? "- ⚠️ À améliorer\n" : "- 🔴 Critique\n");
        narrative.append("\n### Rapports\n");
        narrative.append("- Soumis : ").append(submitted).append("/").append(total).append("\n");
        narrative.append("- Complétion : ").append(total > 0 ? Math.round((double) submitted / total * 100) : 0).append("%\n\n");
        narrative.append("### Alertes\n");
        narrative.append("- Actives : ").append(context.get("activeAlerts")).append("\n");
        report.put("narrative", narrative.toString());
        return report;
    }

    /**
     * RAG context: enrich question with relevant member/family data.
     */
    public Map<String, Object> getRagContext(String query) {
        Map<String, Object> rag = new LinkedHashMap<>();
        String q = query.toLowerCase();

        // Search for matching souls
        if (q.contains("membre") || q.contains("âme") || q.contains("personne")) {
            List<Soul> souls = soulRepository.findAll().stream()
                    .filter(s -> s.getNomComplet() != null && q.contains(s.getNomComplet().toLowerCase()))
                    .limit(5)
                    .toList();
            if (!souls.isEmpty()) {
                rag.put("matchedSouls", souls.stream()
                        .map(s -> Map.of("id", s.getId(), "nom", s.getNomComplet(),
                                "statut", s.getStatut().name()))
                        .toList());
            }
        }
        return rag;
    }

    /**
     * Point d'entrée principal : chat IA pastoral.
     */
    public Map<String, Object> chat(String message, UUID userId) {
        // 1. Construire le contexte depuis les données de l'église
        Map<String, Object> context = buildChurchContext(message);

        // 2. Construire le prompt système
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(message, context);

        // 3. Essayer d'appeler Ollama
        String reply = callOllama(systemPrompt, userPrompt);

        // 4. Si Ollama échoue, générer une réponse contextuelle
        if (reply == null || reply.isBlank()) {
            reply = generateContextualReply(message, context);
        }

        // 5. Sauvegarder dans l'historique
        Map<String, Object> userMsg = Map.of(
                "id", UUID.randomUUID().toString(),
                "role", "user",
                "content", message,
                "timestamp", java.time.Instant.now().toString()
        );
        Map<String, Object> assistantMsg = Map.of(
                "id", UUID.randomUUID().toString(),
                "role", "assistant",
                "content", reply,
                "timestamp", java.time.Instant.now().toString()
        );

        chatHistories.computeIfAbsent(userId, k -> new ArrayList<>()).add(userMsg);
        chatHistories.get(userId).add(assistantMsg);

        // Persist to DB
        UUID sessionId = chatHistories.containsKey(userId) && !chatHistories.get(userId).isEmpty()
                ? UUID.fromString((String) chatHistories.get(userId).get(0).getOrDefault("sessionId", UUID.randomUUID().toString()))
                : UUID.randomUUID();
        AiChatConversation userEntity = new AiChatConversation();
        userEntity.setTenantId(securityUtils.getCurrentTenantId());
        userEntity.setUserId(userId);
        userEntity.setSessionId(sessionId);
        userEntity.setRole(AiChatConversation.Role.USER);
        userEntity.setContent(message);
        chatRepo.save(userEntity);

        AiChatConversation assistantEntity = new AiChatConversation();
        assistantEntity.setTenantId(securityUtils.getCurrentTenantId());
        assistantEntity.setUserId(userId);
        assistantEntity.setSessionId(sessionId);
        assistantEntity.setRole(AiChatConversation.Role.ASSISTANT);
        assistantEntity.setContent(reply);
        assistantEntity.setSourcesJson(context.getOrDefault("sources", List.of()).toString());
        chatRepo.save(assistantEntity);

        return Map.of("reply", reply, "sources", context.getOrDefault("sources", List.of()),
                "sessionId", sessionId.toString());
    }

    /**
     * Vérifie la santé d'Ollama.
     */
    public Map<String, Object> checkHealth() {
        try {
            RestTemplate rt = new RestTemplate();
            ResponseEntity<String> resp = rt.getForEntity(ollamaUrl + "/api/tags", String.class);
            boolean available = resp.getStatusCode() == HttpStatus.OK;
            return Map.of("ollama", available, "url", ollamaUrl, "model", modelName);
        } catch (Exception e) {
            return Map.of("ollama", false, "url", ollamaUrl, "model", modelName, "error", e.getMessage());
        }
    }

    /**
     * Historique du chat pour un utilisateur.
     */
    public List<Map<String, Object>> getChatHistory(UUID userId) {
        // Return from DB first, fall back to in-memory
        List<AiChatConversation> dbHistory = chatRepo.findByUserIdOrderByCreatedAtDesc(userId);
        if (!dbHistory.isEmpty()) {
            return dbHistory.stream().map(c -> Map.<String, Object>of(
                    "id", c.getId().toString(),
                    "role", c.getRole().name().toLowerCase(),
                    "content", c.getContent(),
                    "timestamp", c.getCreatedAt().toString()
            )).toList();
        }
        return chatHistories.getOrDefault(userId, List.of());
    }

    /**
     * Effacer l'historique.
     */
    public void clearChatHistory(UUID userId) {
        chatHistories.remove(userId);
    }

    /**
     * Retourne le contexte pertinent pour une question donnée.
     */
    public Map<String, Object> getContextForQuery(String query) {
        return buildChurchContext(query);
    }

    /* ==================== INTERNAL ==================== */

    /**
     * Construit le contexte complet de l'église pour la question posée.
     */
    private Map<String, Object> buildChurchContext(String query) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        List<String> sources = new ArrayList<>();

        LocalDate currentWeek = LocalDate.now().with(DayOfWeek.MONDAY);

        // Stats générales
        long totalSouls = soulRepository.count();
        long totalFamilies = familyRepository.count();
        long activeAlerts = alertRepository.countByStatut(com.discipolat.common.enums.StatutAlerte.ACTIVE);
        ctx.put("totalSouls", totalSouls);
        ctx.put("totalFamilies", totalFamilies);
        ctx.put("activeAlerts", activeAlerts);
        sources.add("stats");

        // Familles à risque
        List<Family> atRiskFamilies = familyRepository.findAll().stream()
                .filter(f -> f.getNiveauRisque() != null
                        && f.getNiveauRisque() != com.discipolat.common.enums.NiveauRisque.NORMAL)
                .toList();
        if (!atRiskFamilies.isEmpty()) {
            ctx.put("atRiskFamilies", atRiskFamilies.stream()
                    .map(f -> Map.of("id", f.getId(), "nom", f.getNom(),
                            "risque", f.getNiveauRisque().name()))
                    .toList());
            sources.add("families_at_risk");
        }

        // Âmes actives
        List<Soul> activeSouls = soulRepository.findByStatut(com.discipolat.common.enums.StatutAme.ACTIF,
                org.springframework.data.domain.PageRequest.of(0, 20)).getContent();
        ctx.put("activeSoulsSample", activeSouls.stream()
                .map(s -> Map.of("id", s.getId(), "nom", s.getNomComplet(),
                        "statut", s.getStatut().name(),
                        "etatSpirituel", s.getEtatSpirituel() != null ? s.getEtatSpirituel() : ""))
                .toList());

        // Nouveaux convertis récents
        List<Soul> newConverts = soulRepository.findByTypeDisciple(
                com.discipolat.common.enums.TypeDisciple.NOUVEAU_CONVERTI,
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        ctx.put("recentConverts", newConverts.size());
        if (!newConverts.isEmpty()) {
            ctx.put("recentConvertsList", newConverts.stream()
                    .map(s -> Map.of("id", s.getId(), "nom", s.getNomComplet(),
                            "dateIntegration", s.getDateIntegration() != null ? s.getDateIntegration().toString() : ""))
                    .toList());
            sources.add("new Converts");
        }

        // Rapports de la semaine
        List<MakerReport> weekReports = makerReportRepository.findBySemaine(currentWeek,
                org.springframework.data.domain.PageRequest.of(0, 100)).getContent();
        long submitted = weekReports.stream().filter(MakerReport::isSoumis).count();
        ctx.put("weekReportsTotal", weekReports.size());
        ctx.put("weekReportsSubmitted", submitted);
        ctx.put("weekReportsPending", weekReports.size() - submitted);
        sources.add("weekly_reports");

        // Présences de la semaine
        int totalPresent = 0, totalPossible = 0;
        for (MakerReport r : weekReports) {
            if (r.getPresencesParCulte() != null) {
                for (Boolean p : r.getPresencesParCulte().values()) {
                    totalPossible++;
                    if (p) totalPresent++;
                }
            }
        }
        double presenceRate = totalPossible > 0
                ? Math.round((double) totalPresent / totalPossible * 1000.0) / 10.0
                : 0.0;
        ctx.put("presenceRate", presenceRate);
        ctx.put("presenceTotal", totalPresent);
        ctx.put("presencePossible", totalPossible);

        ctx.put("sources", sources);
        return ctx;
    }

    /**
     * Prompt système pour Ollama — définit le rôle de l'assistant.
     */
    private String buildSystemPrompt() {
        return """
                Tu es l'Assistant IA Pastoral de Discipolat, une application de gestion d'église.
                
                Tu as accès aux données de l'église (anonymisées pour la confidentialité).
                Tu dois répondre de manière :
                - **Concise** : pas de blabla, va à l'essentiel
                - **Actionnable** : propose des actions concrètes
                - **Bienveillant** : tu parles à des pasteurs et responsables d'église
                - **Francophone** : réponds toujours en français
                
                Tu peux :
                - Analyser les données de présence et identifier les tendances
                - Suggérer des actions pastoral basées sur les données
                - Résumer les rapports hebdomadaires
                - Identifier les familles et disciples à risque
                - Proposer des plans d'action personnalisés
                
                Tu ne dois JAMAIS :
                - Inventer des données
                - Partager des informations personnelles sensibles
                - Donner des conseils médicaux ou juridiques
                """;
    }

    /**
     * Construit le prompt utilisateur avec le contexte de l'église.
     */
    private String buildUserPrompt(String question, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Contexte de l'église\n\n");

        sb.append("- Total âmes : ").append(context.get("totalSouls")).append("\n");
        sb.append("- Total familles : ").append(context.get("totalFamilies")).append("\n");
        sb.append("- Alertes actives : ").append(context.get("activeAlerts")).append("\n");
        sb.append("- Taux de présence cette semaine : ").append(context.get("presenceRate")).append("%\n");
        sb.append("- Rapports soumis : ").append(context.get("weekReportsSubmitted"))
                .append("/").append(context.get("weekReportsTotal")).append("\n");
        sb.append("- Nouveaux convertis récents : ").append(context.get("recentConverts")).append("\n");

        if (context.containsKey("atRiskFamilies")) {
            sb.append("\n### Familles à risque\n");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> families = (List<Map<String, Object>>) context.get("atRiskFamilies");
            for (Map<String, Object> f : families) {
                sb.append("- ").append(f.get("nom")).append(" (").append(f.get("risque")).append(")\n");
            }
        }

        sb.append("\n## Question\n").append(question);
        return sb.toString();
    }

    /**
     * Appelle Ollama pour générer une réponse IA.
     */
    private String callOllama(String systemPrompt, String userPrompt) {
        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelName);
            body.put("stream", false);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = rt.postForEntity(ollamaUrl + "/api/chat", request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = (Map<String, Object>) response.getBody().get("message");
                if (msg != null && msg.containsKey("content")) {
                    return (String) msg.get("content");
                }
            }
        } catch (Exception e) {
            log.info("Ollama not available ({}), using contextual fallback", e.getMessage());
        }
        return null;
    }

    /**
     * Génère une réponse contextuelle quand Ollama n'est pas disponible.
     */
    private String generateContextualReply(String query, Map<String, Object> context) {
        String q = query.toLowerCase();

        if (q.contains("famil") && (q.contains("risque") || q.contains("danger"))) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> atRisk = (List<Map<String, Object>>) context.getOrDefault("atRiskFamilies", List.of());
            if (atRisk.isEmpty()) {
                return "✅ **Bonne nouvelle !**\n\nAucune famille n'est actuellement marquée comme étant à risque.\n\nCependant, je recommande de :\n• Maintenir un suivi régulier de toutes les familles\n• Surveiller le taux de présence qui est de " + context.get("presenceRate") + "%\n•.Continuer à encourager les chefs de famille";
            }
            StringBuilder sb = new StringBuilder("📊 **Familles à risque identifiées** (" + atRisk.size() + ")\n\n");
            for (Map<String, Object> f : atRisk) {
                sb.append("• **").append(f.get("nom")).append("** — Risque : ").append(f.get("risque")).append("\n");
            }
            sb.append("\n**Recommandations :**\n");
            sb.append("1. Planifier des visites pastorales prioritaires\n");
            sb.append("2. Activer les faiseurs pour un suivi rapproché\n");
            sb.append("3. Organiser un temps de prière pour ces familles\n");
            return sb.toString();
        }

        if (q.contains("présence") || q.contains("taux")) {
            double rate = (double) context.getOrDefault("presenceRate", 0.0);
            return "📈 **Analyse des présences**\n\n" +
                    "• Taux de présence cette semaine : **" + rate + "%**\n" +
                    "• Présences : " + context.get("presenceTotal") + "/" + context.get("presencePossible") + "\n\n" +
                    (rate >= 75 ? "✅ Excellent taux de présence !" :
                            rate >= 50 ? "⚠️ Taux correct, mais des améliorations sont possibles." :
                                    "🔴 Taux faible — action immédiate recommandée.") + "\n\n" +
                    "**Actions suggérées :**\n" +
                    "• Identifier les absents récurrents\n" +
                    "• Contacter les familles avec 0 présence\n" +
                    "• Organiser un événement spécial pour relancer l'engagement";
        }

        if (q.contains("rapport") || q.contains("résumé")) {
            long submitted = (long) context.getOrDefault("weekReportsSubmitted", 0L);
            long total = (long) context.getOrDefault("weekReportsTotal", 0L);
            return "📋 **Résumé de la semaine**\n\n" +
                    "• Rapports soumis : " + submitted + "/" + total + "\n" +
                    "• Taux de complétion : " + (total > 0 ? Math.round((double) submitted / total * 100) : 0) + "%\n" +
                    "• Nouveaux convertis : " + context.get("recentConverts") + "\n" +
                    "• Alertes actives : " + context.get("activeAlerts") + "\n\n" +
                    (submitted < total ?
                            "⚠️ " + (total - submitted) + " rapports restent à soumettre. Contactez les faiseurs en retard." :
                            "✅ Tous les rapports de la semaine ont été soumis !");
        }

        if (q.contains("nouveau") || q.contains("convertis")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> converts = (List<Map<String, Object>>) context.getOrDefault("recentConvertsList", List.of());
            StringBuilder sb = new StringBuilder("🌱 **Suivi des nouveaux convertis**\n\n");
            sb.append("• Nouveaux convertis récents : ").append(context.get("recentConverts")).append("\n\n");
            if (!converts.isEmpty()) {
                for (Map<String, Object> c : converts) {
                    sb.append("• ").append(c.get("nom"));
                    if (c.get("dateIntegration") != null && !c.get("dateIntegration").toString().isEmpty()) {
                        sb.append(" — Intégré le ").append(c.get("dateIntegration"));
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n**Parcours recommandé (90 premiers jours) :**\n");
            sb.append("1. Semaine 1-2 : Accueil + RDV pasteur\n");
            sb.append("2. Mois 1 : Intégration groupe de maison\n");
            sb.append("3. Mois 2 : Formation fondamentales\n");
            sb.append("4. Mois 3 : Identification talents + engagement\n");
            return sb.toString();
        }

        // Réponse par défaut
        return "🤖 **Assistant IA Pastoral**\n\n" +
                "Merci pour votre question. Voici un résumé rapide :\n\n" +
                "• **Total âmes** : " + context.get("totalSouls") + "\n" +
                "• **Familles** : " + context.get("totalFamilies") + "\n" +
                "• **Présence** : " + context.get("presenceRate") + "%\n" +
                "• **Alertes** : " + context.get("activeAlerts") + "\n\n" +
                "💡 Pour une analyse complète, posez-moi une question précise sur :\n" +
                "• Les familles à risque\n" +
                "• Les présences\n" +
                "• Les rapports\n" +
                "• Les nouveaux convertis\n" +
                "• Les faiseurs et leur performance";
    }
}
