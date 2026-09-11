package com.discipolat.modules.ai.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class LlmProviderService {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ai.groq-api-key:}") private String groqApiKey;
    @Value("${app.ai.gemini-api-key:}") private String geminiApiKey;
    @Value("${app.ai.mistral-api-key:}") private String mistralApiKey;
    @Value("${app.ai.huggingface-api-key:}") private String huggingfaceApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private static final String MISTRAL_URL = "https://api.mistral.ai/v1/chat/completions";

    public String generateResponse(String systemPrompt, String userPrompt) {
        String fullPrompt = systemPrompt + "\n\n" + userPrompt;

        if (isConfigured(groqApiKey)) {
            try { String r = callGroq(fullPrompt); if (r != null && !r.isBlank()) return r; }
            catch (Exception e) { log.warn("[LLM] Groq: {}", e.getMessage()); }
        }

        if (isConfigured(geminiApiKey)) {
            try { String r = callGemini(fullPrompt); if (r != null && !r.isBlank()) return r; }
            catch (Exception e) { log.warn("[LLM] Gemini: {}", e.getMessage()); }
        }

        if (isConfigured(mistralApiKey)) {
            try { String r = callMistral(fullPrompt); if (r != null && !r.isBlank()) return r; }
            catch (Exception e) { log.warn("[LLM] Mistral: {}", e.getMessage()); }
        }

        // HuggingFace : avec clé OU en mode anonyme (sans clé, très limité mais 100% gratuit)
        try {
            String r = callHuggingFace(fullPrompt);
            if (r != null && !r.isBlank()) return r;
        } catch (Exception e) {
            log.warn("[LLM] HF: {}", e.getMessage());
        }

        log.info("[LLM] Using deterministic fallback");
        return generateFallback(userPrompt);
    }

    public Map<String, Boolean> getAvailableProviders() {
        Map<String, Boolean> p = new LinkedHashMap<>();
        p.put("groq", isConfigured(groqApiKey));
        p.put("gemini", isConfigured(geminiApiKey));
        p.put("mistral", isConfigured(mistralApiKey));
        p.put("huggingface", isConfigured(huggingfaceApiKey) || true); // Anonyme possible
        p.put("ollama", getOllamaAvailability());
        p.put("local", false); // Déterministe — toujours dispo mais pas un LLM
        p.put("fallback", true);
        return p;
    }

    private boolean isConfigured(String key) { return key != null && !key.isBlank(); }

    // ==================== GROQ (Llama 3.1 70B, Mixtral) ====================
    private String callGroq(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(groqApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", "llama-3.1-70b-versatile");
        body.put("messages", List.of(
            Map.of("role", "system", "content", "Tu es un assistant IA pastoral pour Discipolat. Réponds en français, de manière concise et utile."),
            Map.of("role", "user", "content", prompt)));
        body.put("max_tokens", 1024);
        body.put("temperature", 0.7);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(GROQ_URL, req, Map.class);
        if (resp.getBody() != null && resp.getBody().containsKey("choices")) {
            var choices = (List<Map<String, Object>>) resp.getBody().get("choices");
            if (!choices.isEmpty()) {
                var msg = (Map<String, Object>) choices.get(0).get("message");
                return (String) msg.get("content");
            }
        }
        return null;
    }

    // ==================== GOOGLE GEMINI (1.5 Flash) ====================
    private String callGemini(String prompt) {
        String url = GEMINI_URL + "?key=" + geminiApiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of("parts", List.of(Map.of("text",
            "Tu es un assistant IA pastoral pour Discipolat. Réponds en français.\n\n" + prompt)))));
        body.put("generationConfig", Map.of("maxOutputTokens", 1024, "temperature", 0.7));
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, req, Map.class);
        if (resp.getBody() != null && resp.getBody().containsKey("candidates")) {
            var cands = (List<Map<String, Object>>) resp.getBody().get("candidates");
            if (!cands.isEmpty()) {
                var content = (Map<String, Object>) cands.get(0).get("content");
                var parts = (List<Map<String, Object>>) content.get("parts");
                if (!parts.isEmpty()) return (String) parts.get(0).get("text");
            }
        }
        return null;
    }

    // ==================== MISTRAL (Mistral 7B) ====================
    private String callMistral(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(mistralApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", "mistral-latest");
        body.put("messages", List.of(
            Map.of("role", "system", "content", "Tu es un assistant IA pastoral pour Discipolat. Réponds en français."),
            Map.of("role", "user", "content", prompt)));
        body.put("max_tokens", 1024);
        body.put("temperature", 0.7);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(MISTRAL_URL, req, Map.class);
        if (resp.getBody() != null && resp.getBody().containsKey("choices")) {
            var choices = (List<Map<String, Object>>) resp.getBody().get("choices");
            if (!choices.isEmpty()) {
                var msg = (Map<String, Object>) choices.get(0).get("message");
                return (String) msg.get("content");
            }
        }
        return null;
    }

    // ==================== HUGGING FACE (Llama 3, Qwen 2, Deepseek) ====================
    // Modèles essayés dans l'ordre — avec clé API : accès complet + quota 30k req/mois.
    // Sans clé : mode anonyme (limité mais 100% gratuit, aucune inscription).
    private static final String[] HF_MODELS = {
        "https://api-inference.huggingface.co/models/meta-llama/Meta-Llama-3-8B-Instruct",
        "https://api-inference.huggingface.co/models/Qwen/Qwen2.5-7B-Instruct",
        "https://api-inference.huggingface.co/models/deepseek-ai/DeepSeek-R1-Distill-Qwen-7B",
        "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.3"
    };

    private String callHuggingFace(String prompt) {
        int startIdx = isConfigured(huggingfaceApiKey) ? 0 : 0;
        for (int i = startIdx; i < HF_MODELS.length; i++) {
            try {
                String r = callHuggingFaceModel(HF_MODELS[i], prompt);
                if (r != null && !r.isBlank()) {
                    log.debug("[LLM] HF model[{}] ok, configured={}", i, isConfigured(huggingfaceApiKey));
                    return r;
                }
            } catch (Exception e) {
                log.warn("[LLM] HF model[{}] failed: {}", i, e.getMessage());
            }
        }
        return null;
    }

    private String callHuggingFaceModel(String modelUrl, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        if (isConfigured(huggingfaceApiKey)) {
            headers.setBearerAuth(huggingfaceApiKey);   // Mode clé (comme Groq/Gemini/Mistral)
        }
        // Sans header Authorization = mode ANONYME (limité mais sans clé)
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-wait-for-model", "true");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", "<|system|>Tu es un assistant IA pastoral pour Discipolat. Réponds en français, de manière concise et utile.<|end|><|user|>" + prompt + "<|end|><|assistant|>");
        body.put("parameters", Map.of(
                "max_new_tokens", isConfigured(huggingfaceApiKey) ? 512 : 256,
                "temperature", 0.7,
                "return_full_text", false
        ));

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<?> resp = restTemplate.postForEntity(modelUrl, req, Object.class);
        if (resp.getBody() != null) {
            if (resp.getBody() instanceof List<?> list && !list.isEmpty()) {
                var result = (Map<String, Object>) list.get(0);
                Object gt = result.get("generated_text");
                if (gt != null) return gt.toString().trim();
            } else if (resp.getBody() instanceof Map<?, ?> map && map.containsKey("generated_text")) {
                return map.get("generated_text").toString().trim();
            }
        }
        return null;
    }

    // ==================== OLLAMA (local, aucune clé nécessaire) ====================
    private boolean getOllamaAvailability() {
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity("http://localhost:11434/api/tags", String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== FALLBACK DETERMINISTE ====================
    private String generateFallback(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("famille") && lower.contains("risque"))
            return "🤖 Analyse IA (mode local)\n\nPour identifier les familles à risque :\n• Taux de présence < 50%\n• Aucun rapport depuis 2 semaines\n• Alertes actives non traitées\n\n💡 Page Familles > Filtre 'Présence faible'";
        if (lower.contains("croissance") || lower.contains("prédiction"))
            return "🤖 Prévision IA (mode local)\n\nBasé sur vos données :\n• Taux d'intégration 90 jours = projection\n• Présence > 75% = croissance saine\n• Rapports hebdomadaires = suivi engagement\n\n💡 Page Prédictions IA pour détails";
        if (lower.contains("sermon") || lower.contains("prédication"))
            return "🤖 Assistant Sermon (mode local)\n\nPour générer un plan de sermon :\n• Passage biblique (ex: Psaume 23)\n• Thème (ex: confiance en Dieu)\n• Audience (Jeunes, Familles, Faiseurs)\n\n💡 3-5 structures avec illustrations";
        return "🤖 Assistant IA Pastoral (mode local)\n\nAucun LLM configuré. Pour activer les IA gratuites, ajoutez une clé API dans application.yml :\n• app.ai.groq-api-key (Llama 3.1 70B - Groq)\n• app.ai.gemini-api-key (Gemini 1.5 Flash - Google)\n• app.ai.mistral-api-key (Mistral)\n• app.ai.huggingface-api-key (HuggingFace)\n\nTous gratuits avec limites généreuses.";
    }
}
