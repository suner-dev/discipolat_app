package com.discipolat.modules.notifications.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * P21 — Passerelle SMS Twilio, activée uniquement si configurée
 * (app.sms.twilio.enabled=true + credentials). Sinon, mode journal :
 * les envois sont logués sans appel réseau (démarrage sans dépendance externe).
 */
@Component
@Slf4j
public class SmsGateway {

    private final boolean enabled;
    private final String sid;
    private final String token;
    private final String from;
    private final RestClient restClient;

    public SmsGateway(@Value("${app.sms.twilio.enabled:false}") boolean enabled,
                      @Value("${app.sms.twilio.sid:}") String sid,
                      @Value("${app.sms.twilio.token:}") String token,
                      @Value("${app.sms.twilio.from:}") String from) {
        this.enabled = enabled;
        this.sid = sid;
        this.token = token;
        this.from = from;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5000);
        f.setReadTimeout(8000);
        this.restClient = RestClient.builder().requestFactory(f).build();
        if (!enabled) {
            log.info("[SmsGateway] Twilio désactivé (app.sms.twilio.enabled=false) — mode journal");
        }
    }

    public boolean isEnabled() { return enabled; }

    /** Envoie un SMS ; retourne true si accepté par l'opérateur. Best-effort. */
    public boolean send(String toE164, String message) {
        if (!enabled || sid.isBlank() || from.isBlank()) {
            log.info("[SmsGateway:journal] → {} : {}", toE164, message);
            return false;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", toE164);
            form.add("From", from);
            form.add("Body", message);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(sid, token);
            restClient.post()
                    .uri("https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json", sid)
                    .headers(h -> h.addAll(headers))
                    .body(new HttpEntity<>(form, headers))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("[SmsGateway] Échec envoi vers {} : {}", toE164, e.getMessage());
            return false;
        }
    }
}
