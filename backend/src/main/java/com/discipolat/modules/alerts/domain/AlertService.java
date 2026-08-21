package com.discipolat.modules.alerts.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AlertService {

    private final AlertRepository alertRepository;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;

    public AlertService(AlertRepository alertRepository, EntityPropagationPublisher propagationPublisher,
                        SecurityUtils securityUtils) {
        this.alertRepository = alertRepository;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
    }

    public Alert create(Alert alert) {
        alert.setDateDeclenchement(LocalDateTime.now());
        alert.setStatut(StatutAlerte.ACTIVE);
        Alert saved = alertRepository.save(alert);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishCreated("ALERT", saved.getId(),
                Map.of("typeAlerte", saved.getTypeAlerte() != null ? saved.getTypeAlerte() : "",
                        "titre", saved.getTitre() != null ? saved.getTitre() : ""),
                "Alerte créée: " + (saved.getTitre() != null ? saved.getTitre() : ""));
        return saved;
    }

    /**
     * Création manuelle d'une alerte ciblée (personne, département, famille, groupe, église).
     */
    public Alert createManual(com.discipolat.modules.alerts.api.CreateAlertRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Alert alert = Alert.builder()
                .typeAlerte("MANUEL")
                .typeAlerteManuel(request.typeAlerteManuel())
                .titre(request.titre())
                .message(request.message())
                .cible(request.cible())
                .priorite(request.priorite() != null ? request.priorite() : "MOYENNE")
                .ameId(request.ameId())
                .faiseurId(request.faiseurId())
                .familleId(request.familleId())
                .departmentId(request.departmentId())
                .dateDeclenchement(LocalDateTime.now())
                .statut(StatutAlerte.ACTIVE)
                .build();
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
        String oldStatut = alert.getStatut().name();
        alert.setStatut(StatutAlerte.RESOLUE);
        alert.setDateResolution(LocalDateTime.now());
        alert.setResoluPar(securityUtils.getCurrentUserId());
        Alert saved = alertRepository.save(alert);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishStatusChanged("ALERT", id,
                oldStatut, StatutAlerte.RESOLUE.name(),
                "Alerte résolue: " + (alert.getTitre() != null ? alert.getTitre() : ""));
        return saved;
    }

    public Alert acknowledge(UUID id) {
        Alert alert = findById(id);
        String oldStatut = alert.getStatut().name();
        alert.setStatut(StatutAlerte.TRAITEE);
        Alert saved = alertRepository.save(alert);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishStatusChanged("ALERT", id,
                oldStatut, StatutAlerte.TRAITEE.name(),
                "Alerte traitée: " + (alert.getTitre() != null ? alert.getTitre() : ""));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Alert> findActiveByAme(UUID ameId) {
        return alertRepository.findByAmeIdAndStatut(ameId, StatutAlerte.ACTIVE);
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return alertRepository.countByStatut(StatutAlerte.ACTIVE);
    }

    /**
     * Résolution en lot : marque toutes les alertes données comme résolues.
     */
    public int resolveBatch(List<UUID> ids) {
        int count = 0;
        UUID userId = securityUtils.getCurrentUserId();
        for (UUID id : ids) {
            Alert alert = alertRepository.findById(id).orElse(null);
            if (alert != null && alert.getStatut() == StatutAlerte.ACTIVE) {
                alert.setStatut(StatutAlerte.RESOLUE);
                alert.setDateResolution(LocalDateTime.now());
                alert.setResoluPar(userId);
                alertRepository.save(alert);
                count++;
            }
        }
        return count;
    }

    /**
     * Statistiques d'alertes (pour tendances/graphiques).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAlertStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long active = alertRepository.countByStatut(StatutAlerte.ACTIVE);
        long traitees = alertRepository.countByStatut(StatutAlerte.TRAITEE);
        long resolues = alertRepository.countByStatut(StatutAlerte.RESOLUE);
        stats.put("actives", active);
        stats.put("traitees", traitees);
        stats.put("resolues", resolues);
        stats.put("total", active + traitees + resolues);
        return stats;
    }
}
