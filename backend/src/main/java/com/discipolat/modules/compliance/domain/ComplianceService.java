package com.discipolat.modules.compliance.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ComplianceService {

    private final GdprRepository gdprRepository;
    private final ConsentLogRepository consentRepository;

    public ComplianceService(GdprRepository gdprRepository, ConsentLogRepository consentRepository) {
        this.gdprRepository = gdprRepository;
        this.consentRepository = consentRepository;
    }

    public Page<GdprRequest> listRequests(Pageable pageable) {
        return gdprRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId(), pageable);
    }

    public GdprRequest getRequest(UUID id) {
        return gdprRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("GdprRequest", id));
    }

    public GdprRequest createRequest(String typeDemande, UUID demandeurId, UUID concerneId, String motif) {
        GdprRequest req = new GdprRequest();
        req.setTenantId(TenantContext.getCurrentTenantId());
        req.setTypeDemande(GdprRequest.TypeDemande.valueOf(typeDemande));
        req.setDemandeurId(demandeurId);
        req.setConcerneId(concerneId);
        req.setMotif(motif);
        req.setDeadlineAt(LocalDateTime.now().plusDays(30));
        return gdprRepository.save(req);
    }

    public GdprRequest processRequest(UUID id, String statut, String resultat, UUID traiteurId) {
        GdprRequest req = getRequest(id);
        req.setStatut(GdprRequest.Statut.valueOf(statut));
        req.setResultat(resultat);
        req.setTraiteLe(LocalDateTime.now());
        req.setTraitePar(traiteurId);
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
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDemandes", gdprRepository.countByTenantIdAndStatut(tenantId, GdprRequest.Statut.EN_ATTENTE)
                + gdprRepository.countByTenantIdAndStatut(tenantId, GdprRequest.Statut.EN_COURS)
                + gdprRepository.countByTenantIdAndStatut(tenantId, GdprRequest.Statut.TRAITE)
                + gdprRepository.countByTenantIdAndStatut(tenantId, GdprRequest.Statut.REJETE));
        stats.put("enAttente", gdprRepository.countByTenantIdAndStatut(tenantId, GdprRequest.Statut.EN_ATTENTE));
        stats.put("traites", gdprRepository.countByTenantIdAndStatut(tenantId, GdprRequest.Statut.TRAITE));
        stats.put("totalConsentements", consentRepository.findByTenantId(tenantId).size());
        return stats;
    }
}
