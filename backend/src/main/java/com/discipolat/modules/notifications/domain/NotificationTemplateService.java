package com.discipolat.modules.notifications.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.notifications.api.NotificationEventInfo;
import com.discipolat.modules.notifications.api.NotificationTemplateRequest;
import com.discipolat.modules.notifications.api.NotificationTemplateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gestion des modèles de notification configurables (centre de configuration admin).
 *
 * <p>Un modèle définit, pour un événement ({@link TypeNotification}), le titre et le
 * message rendus à l'émission, les canaux de diffusion et les rôles destinataires
 * recommandés. Le {@link NotificationService} consomme ces modèles : une notification
 * d'un type disposant d'un modèle actif en reprend le texte rendu.
 *
 * <p>Toute modification est tracée dans le journal d'audit.
 */
@Service
@Transactional
public class NotificationTemplateService {

    private static final Pattern VAR = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)\\}\\}");

    private final NotificationTemplateRepository templateRepository;
    private final AuditService auditService;

    public NotificationTemplateService(NotificationTemplateRepository templateRepository,
                                       AuditService auditService) {
        this.templateRepository = templateRepository;
        this.auditService = auditService;
    }

    private UUID tenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessRuleException("Aucun contexte tenant actif — configuration impossible");
        }
        return tenantId;
    }

    /* ============================ Catalogue d'événements ============================ */

    /** Catalogue des événements configurables, avec libellés FR et modèles suggérés. */
    public List<NotificationEventInfo> eventCatalog() {
        List<NotificationEventInfo> out = new ArrayList<>();
        for (NotificationEventCatalog.Entry e : NotificationEventCatalog.entries()) {
            out.add(new NotificationEventInfo(e.event(), e.label(), e.titre(), e.message(),
                    e.canaux(), e.variables()));
        }
        for (TypeNotification t : TypeNotification.values()) {
            if (NotificationEventCatalog.find(t) == null) {
                out.add(new NotificationEventInfo(t, t.name(), t.name(), "Notification " + t.name(),
                        List.of(CanalNotification.IN_APP), List.of("{{type}}", "{{entiteType}}")));
            }
        }
        return out;
    }

    /* ============================ CRUD ============================ */

    public List<NotificationTemplateResponse> list() {
        return templateRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId()).stream()
                .map(NotificationTemplateResponse::from)
                .toList();
    }

    public NotificationTemplateResponse create(NotificationTemplateRequest request) {
        UUID tenantId = tenantId();
        if (request.event() == null) {
            throw new BusinessRuleException("L'événement est obligatoire");
        }
        if (templateRepository.findByTenantIdAndEvent(tenantId, request.event()).isPresent()) {
            throw new BusinessRuleException("Un modèle existe déjà pour l'événement " + request.event());
        }
        NotificationTemplate template = NotificationTemplate.builder()
                .tenantId(tenantId)
                .event(request.event())
                .titre(request.titre())
                .message(request.message())
                .canaux(request.canaux() != null ? new ArrayList<>(request.canaux())
                        : new ArrayList<>(List.of(CanalNotification.IN_APP)))
                .rolesDestinataires(request.rolesDestinataires() != null
                        ? new ArrayList<>(request.rolesDestinataires()) : new ArrayList<>())
                .actif(request.actif() == null || request.actif())
                .build();
        NotificationTemplate saved = templateRepository.save(template);
        auditService.logSimple("NOTIFICATION_TEMPLATE_CREATED", "NOTIFICATION_TEMPLATE", saved.getId());
        return NotificationTemplateResponse.from(saved);
    }

    public NotificationTemplateResponse update(UUID id, NotificationTemplateRequest request) {
        NotificationTemplate template = find(id);
        if (request.titre() != null) template.setTitre(request.titre());
        if (request.message() != null) template.setMessage(request.message());
        if (request.canaux() != null) template.setCanaux(new ArrayList<>(request.canaux()));
        if (request.rolesDestinataires() != null)
            template.setRolesDestinataires(new ArrayList<>(request.rolesDestinataires()));
        if (request.actif() != null) template.setActif(request.actif());
        NotificationTemplate saved = templateRepository.save(template);
        auditService.logSimple("NOTIFICATION_TEMPLATE_UPDATED", "NOTIFICATION_TEMPLATE", id);
        return NotificationTemplateResponse.from(saved);
    }

    public NotificationTemplateResponse toggle(UUID id, boolean actif) {
        NotificationTemplate template = find(id);
        template.setActif(actif);
        NotificationTemplate saved = templateRepository.save(template);
        auditService.logSimple(actif ? "NOTIFICATION_TEMPLATE_ENABLED" : "NOTIFICATION_TEMPLATE_DISABLED",
                "NOTIFICATION_TEMPLATE", id);
        return NotificationTemplateResponse.from(saved);
    }

    public void delete(UUID id) {
        NotificationTemplate template = find(id);
        templateRepository.delete(template);
        auditService.logSimple("NOTIFICATION_TEMPLATE_DELETED", "NOTIFICATION_TEMPLATE", id);
    }

    private NotificationTemplate find(UUID id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("NotificationTemplate", id));
        if (!template.getTenantId().equals(tenantId())) {
            throw new EntityNotFoundException("NotificationTemplate", id);
        }
        return template;
    }

    /* ============================ Rendu ============================ */

    /**
     * Rend un modèle en substituant les variables {@code {{...}}}.
     * Variables natives : {@code {{type}}}, {@code {{event}}}, {@code {{entiteType}}}.
     * Toute variable restante inconnue est laissée telle quelle.
     */
    public static String render(String pattern, TypeNotification event, String entiteType) {
        if (pattern == null) return null;
        Map<String, String> vars = new HashMap<>();
        vars.put("type", event != null ? event.name() : "");
        vars.put("event", event != null ? event.name() : "");
        vars.put("entiteType", entiteType != null ? entiteType : "");
        Matcher m = VAR.matcher(pattern);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            m.appendReplacement(sb, Matcher.quoteReplacement(vars.getOrDefault(key, m.group(0))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Canal préféré d'un modèle (priorité IN_APP → PUSH → EMAIL), fallback donné. */
    public static CanalNotification preferredCanal(List<CanalNotification> canaux, CanalNotification fallback) {
        if (canaux == null || canaux.isEmpty()) return fallback;
        if (canaux.contains(fallback)) return fallback;
        if (canaux.contains(CanalNotification.IN_APP)) return CanalNotification.IN_APP;
        if (canaux.contains(CanalNotification.PUSH)) return CanalNotification.PUSH;
        return canaux.get(0);
    }
}
