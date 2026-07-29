package com.discipolat.modules.alerts.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AlertService {

    private final AlertRepository alertRepository;
    private final SecurityUtils securityUtils;

    public AlertService(AlertRepository alertRepository, SecurityUtils securityUtils) {
        this.alertRepository = alertRepository;
        this.securityUtils = securityUtils;
    }

    public Alert create(Alert alert) {
        alert.setDateDeclenchement(LocalDateTime.now());
        alert.setStatut(StatutAlerte.ACTIVE);
        return alertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public Alert findById(UUID id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alert", id));
    }

    @Transactional(readOnly = true)
    public Page<Alert> findAll(String statut, UUID familleId, Pageable pageable) {
        if (statut != null) {
            return alertRepository.findByStatut(StatutAlerte.valueOf(statut.toUpperCase()), pageable);
        }
        if (familleId != null) {
            return alertRepository.findByFamilleId(familleId, pageable);
        }
        return alertRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Alert> findActive(Pageable pageable) {
        return alertRepository.findByStatut(StatutAlerte.ACTIVE, pageable);
    }

    public Alert resolve(UUID id) {
        Alert alert = findById(id);
        alert.setStatut(StatutAlerte.RESOLUE);
        alert.setDateResolution(LocalDateTime.now());
        alert.setResoluPar(securityUtils.getCurrentUserId());
        return alertRepository.save(alert);
    }

    public Alert acknowledge(UUID id) {
        Alert alert = findById(id);
        alert.setStatut(StatutAlerte.TRAITEE);
        return alertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public List<Alert> findActiveByAme(UUID ameId) {
        return alertRepository.findByAmeIdAndStatut(ameId, StatutAlerte.ACTIVE);
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return alertRepository.countByStatut(StatutAlerte.ACTIVE);
    }
}
