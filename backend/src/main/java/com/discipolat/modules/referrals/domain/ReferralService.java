package com.discipolat.modules.referrals.domain;

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
public class ReferralService {

    private final ReferralRepository repository;

    public ReferralService(ReferralRepository repository) {
        this.repository = repository;
    }

    public Page<Referral> list(Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    public Referral getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Referral", id));
    }

    public Referral create(String nomComplet, String telephone, String email, String notes, UUID userId) {
        Referral r = new Referral();
        r.setTenantId(TenantContext.getCurrentTenantId());
        r.setNomComplet(nomComplet);
        r.setTelephone(telephone);
        r.setEmail(email);
        r.setNotes(notes);
        r.setParrainId(userId);
        return repository.save(r);
    }

    public Referral updateStatut(UUID id, String statut) {
        Referral r = getById(id);
        r.setStatut(Referral.Statut.valueOf(statut));
        if (Referral.Statut.valueOf(statut) == Referral.Statut.INSCRIT) {
            r.setPoints(r.getPoints() + 10);
            r.setConvertedAt(LocalDateTime.now());
        }
        r.setPoints(r.getPoints() + 5);
        return repository.save(r);
    }

    public Map<String, Object> getStats(UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", repository.countByTenantId(tenantId));
        stats.put("inscrits", repository.countByTenantIdAndStatut(tenantId, Referral.Statut.INSCRIT));
        stats.put("baptemes", repository.countByTenantIdAndStatut(tenantId, Referral.Statut.BAPTEME));
        stats.put("mesParrainages", repository.findByParrainId(userId).size());
        return stats;
    }
}
