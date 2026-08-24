package com.discipolat.modules.notifications.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.notifications.domain.NotificationPreference;
import com.discipolat.modules.notifications.domain.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * P21 — Préférences de notification par utilisateur.
 * GET  /api/v1/notifications/preferences — lit (crée par défaut si absent)
 * PUT  /api/v1/notifications/preferences — met à jour les canaux acceptés
 */
@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceRepository repository;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<NotificationPreference> get() {
        UUID userId = SecurityUtils.getCurrentUserId();
        NotificationPreference pref = repository.findByUserId(userId).orElseGet(() -> {
            NotificationPreference p = new NotificationPreference();
            p.setTenantId(TenantContext.getTenantId());
            p.setUserId(userId);
            return repository.save(p);
        });
        return ResponseEntity.ok(pref);
    }

    @PutMapping
    public ResponseEntity<NotificationPreference> update(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        NotificationPreference pref = repository.findByUserId(userId).orElseGet(() -> {
            NotificationPreference p = new NotificationPreference();
            p.setTenantId(TenantContext.getTenantId());
            p.setUserId(userId);
            return p;
        });
        if (body.containsKey("emailEnabled")) pref.setEmailEnabled(Boolean.parseBoolean(body.get("emailEnabled").toString()));
        if (body.containsKey("pushEnabled")) pref.setPushEnabled(Boolean.parseBoolean(body.get("pushEnabled").toString()));
        if (body.containsKey("smsEnabled")) pref.setSmsEnabled(Boolean.parseBoolean(body.get("smsEnabled").toString()));
        if (body.containsKey("whatsappEnabled")) pref.setWhatsappEnabled(Boolean.parseBoolean(body.get("whatsappEnabled").toString()));
        if (body.containsKey("inAppEnabled")) pref.setInAppEnabled(Boolean.parseBoolean(body.get("inAppEnabled").toString()));
        if (body.containsKey("quietHoursStart")) pref.setQuietHoursStart(((Number) body.get("quietHoursStart")).intValue());
        if (body.containsKey("quietHoursEnd")) pref.setQuietHoursEnd(((Number) body.get("quietHoursEnd")).intValue());
        pref.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(repository.save(pref));
    }
}
