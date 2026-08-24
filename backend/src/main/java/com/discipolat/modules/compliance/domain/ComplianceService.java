package com.discipolat.modules.compliance.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.gdpr.domain.GdprRequest;
import com.discipolat.modules.gdpr.domain.GdprRequestRepository;
import com.discipolat.modules.gdpr.domain.GdprRequestStatus;
import com.discipolat.modules.gdpr.domain.GdprRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Service conformité : délègue les demandes RGPD au module gdpr (source de
 * vérité unique sur la table gdpr_requests) et gère le journal des
 * consentements.
 *
 * FIX: ce service dupliquait auparavant l'entité GdprRequest avec un mapping
 * incompatible sur la même table, ce qui empêchait le démarrage de
 * l'application (colonnes logiques en conflit tenant_id / tenantId).
 */
@Service
@Transactional
public class ComplianceService {

    private final GdprRequestRepository gdprRepository;
    private final ConsentLogRepository consentRepository;

    public ComplianceService(GdprRequestRepository gdprRepository, ConsentLogRepository consentRepository) {
        this.gdprRepository = gdprRepository;
        this.consentRepository = consentRepository;
    }

    /** Convertit une clé métier française/angulaire vers l'enum du module gdpr. */
    private static GdprRequestType toType(String raw) {
        if (raw == null || raw.isBlank()) return GdprRequestType.DATA_EXPORT;
        switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "SUPPRESSION", "DATA_DELETION", "DELETION": return GdprRequestType.DATA_DELETION;
            case "PORTABILITE", "PORTABILITY", "DATA_PORTABILITY": return GdprRequestType.DATA_PORTABILITY;
            default: return GdprRequestType.DATA_EXPORT;
        }
    }

    /** Convertit un statut texte vers l'enum du module gdpr. */
    private static GdprRequestStatus toStatus(String raw) {
        if (raw == null || raw.isBlank()) return GdprRequestStatus.PENDING;
        switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "EN_COURS", "PROCESSING", "IN_PROGRESS": return GdprRequestStatus.PROCESSING;
            case "TRAITE", "COMPLETED", "DONE": return GdprRequestStatus.COMPLETED;
            case "REJETE", "REJECTED": return GdprRequestStatus.REJECTED;
            default: return GdprRequestStatus.PENDING;
        }
    }

    public Page<GdprRequest> listRequests(Pageable pageable) {
        var all = gdprRepository.findByTenantIdOrderByRequestedAtDesc(TenantContext.getCurrentTenantId());
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    public GdprRequest getRequest(UUID id) {
        return gdprRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("GdprRequest", id));
    }

    public GdprRequest createRequest(String typeDemande, UUID demandeurId, UUID concerneId, String motif) {
        GdprRequest req = GdprRequest.builder()
                .tenantId(TenantContext.getCurrentTenantId())
                .requesterUserId(demandeurId)
                .requestType(toType(typeDemande))
                .status(GdprRequestStatus.PENDING)
                .notes(motif != null ? motif + (concerneId != null ? " [concerne: " + concerneId + "]" : "") : null)
                .build();
        return gdprRepository.save(req);
    }

    public GdprRequest processRequest(UUID id, String statut, String resultat, UUID traiteurId) {
        GdprRequest req = getRequest(id);
        req.setStatus(toStatus(statut));
        req.setProcessedAt(LocalDateTime.now());
        req.setProcessedBy(traiteurId);
        if (resultat != null && !resultat.isBlank()) {
            req.setNotes((req.getNotes() != null ? req.getNotes() + "\n" : "") + "Résultat: " + resultat);
        }
        return gdprRepository.save(req);
    }

    public void logConsent(UUID utilisateurId, String typeConsentement, boolean accorde, String details) {
        ConsentLog log = new ConsentLog();
        log.setTenantId(TenantContext.getCurrentTenantId());
        log.setUtilisateurId(utilisateurId);
        log.setTypeConsentement(typeConsentement);
        log.setAccorde(accorde);
        log.setDetails(details);
        consentRepository.save(log);
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        var requests = gdprRepository.findByTenantIdOrderByRequestedAtDesc(tenantId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDemandes", requests.size());
        stats.put("enAttente", requests.stream().filter(r -> r.getStatus() == GdprRequestStatus.PENDING).count());
        stats.put("traites", requests.stream().filter(r -> r.getStatus() == GdprRequestStatus.COMPLETED).count());
        stats.put("totalConsentements", consentRepository.findByTenantId(tenantId).size());
        return stats;
    }
}

