package com.discipolat.modules.families.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.NiveauRisque;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.members.domain.MemberPresence;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Système « Famille à risque » :
 * - Le pasteur peut définir manuellement un niveau (NORMAL / SOUS_SURVEILLANCE / A_RISQUE).
 * - Un indice de risque (0-100) est calculé automatiquement à partir de :
 *   taux de présence, âmes perdues, croissance, stagnation, absences, litiges, retards.
 * - Chaque changement de niveau est historisé (FamilyRiskHistory).
 */
@Service
@Transactional
public class FamilyRiskService {

    private final FamilyRepository familyRepository;
    private final FamilyRiskHistoryRepository riskHistoryRepository;
    private final SoulRepository soulRepository;
    private final MemberPresenceRepository presenceRepository;
    private final AlertRepository alertRepository;
    private final SecurityUtils securityUtils;

    public FamilyRiskService(FamilyRepository familyRepository,
                             FamilyRiskHistoryRepository riskHistoryRepository,
                             SoulRepository soulRepository,
                             MemberPresenceRepository presenceRepository,
                             AlertRepository alertRepository,
                             SecurityUtils securityUtils) {
        this.familyRepository = familyRepository;
        this.riskHistoryRepository = riskHistoryRepository;
        this.soulRepository = soulRepository;
        this.presenceRepository = presenceRepository;
        this.alertRepository = alertRepository;
        this.securityUtils = securityUtils;
    }

    /** Indice de risque calculé automatiquement pour une famille (0-100) + détails. */
    @Transactional(readOnly = true)
    public Map<String, Object> getRiskAssessment(UUID familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new EntityNotFoundException("Family", familyId));

        List<Soul> souls = soulRepository.findAllByFamilleId(familyId).stream()
                .filter(s -> !s.isDeleted())
                .toList();
        List<UUID> soulIds = souls.stream().map(Soul::getId).toList();

        // --- Présences (4 dernières semaines) ---
        LocalDate today = LocalDate.now();
        List<MemberPresence> presences = soulIds.isEmpty() ? List.of()
                : presenceRepository.findBySoulIdInOrderBySemaineDesc(soulIds).stream()
                        .filter(p -> p.getSemaine().isAfter(today.minusWeeks(4)))
                        .toList();

        int totalPresences = 0;
        int totalPresents = 0;
        int absences = 0;
        for (MemberPresence p : presences) {
            if (p.getPresences() == null) continue;
            for (Boolean present : p.getPresences().values()) {
                totalPresences++;
                if (Boolean.TRUE.equals(present)) totalPresents++;
                else absences++;
            }
        }
        double tauxPresence = totalPresences > 0
                ? Math.round((double) totalPresents / totalPresences * 1000.0) / 10.0 : 0.0;

        // --- Âmes perdues (statut DECROCHE ou supprimées) ---
        long amesPerdues = soulRepository.findAllByFamilleId(familyId).stream()
                .filter(s -> s.isDeleted() || s.getStatut() == StatutAme.DECROCHE)
                .count();

        // --- Croissance / stagnation (30 derniers jours) ---
        long nouveaux = souls.stream()
                .filter(s -> s.getDateIntegration() != null
                        && s.getDateIntegration().isAfter(today.minusDays(30)))
                .count();
        long enVeille = souls.stream()
                .filter(s -> s.getStatut() == StatutAme.EN_VEILLE)
                .count();

        // --- Litiges & retards (alertes actives de la famille) ---
        long litiges = countAlertes(familyId, "LITIGE");
        long retards = countAlertes(familyId, "RETARD");

        // --- Score pondéré (0-100) ---
        // Une famille sans âmes suivies n'est pas évaluée (score 0).
        int score = 0;
        if (souls.isEmpty()) {
            score = 0;
        } else if (tauxPresence < 40) score += 30;
        else if (tauxPresence < 65) score += 18;
        else if (tauxPresence < 80) score += 8;

        score += (int) Math.min(amesPerdues * 12, 24);
        if (nouveaux == 0 && !souls.isEmpty()) score += 10;          // stagnation
        score += (int) Math.min(enVeille * 5, 15);
        score += (int) Math.min(litiges * 10, 20);
        score += (int) Math.min(retards * 6, 12);
        score += absences >= 4 ? 8 : 0;
        score = Math.min(score, 100);

        // --- Niveau suggéré automatiquement ---
        NiveauRisque suggere;
        if (score >= 60) suggere = NiveauRisque.A_RISQUE;
        else if (score >= 30) suggere = NiveauRisque.SOUS_SURVEILLANCE;
        else suggere = NiveauRisque.NORMAL;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("familyId", family.getId());
        result.put("nom", family.getNom());
        result.put("niveauActuel", family.getNiveauRisque());
        result.put("niveauSuggere", suggere);
        result.put("scoreRisque", score);
        result.put("tauxPresence", tauxPresence);
        result.put("amesPerdues", amesPerdues);
        result.put("nouveaux30j", nouveaux);
        result.put("enVeille", enVeille);
        result.put("absences4sem", absences);
        result.put("litiges", litiges);
        result.put("retards", retards);
        result.put("totalSouls", souls.size());
        result.put("evaluationDate", today.toString());
        return result;
    }

    /**
     * Définition manuelle du niveau de risque par le pasteur.
     * Historise automatiquement le changement.
     */
    public Family setNiveauRisque(UUID familyId, NiveauRisque nouveauNiveau, String raison) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new EntityNotFoundException("Family", familyId));
        NiveauRisque ancien = family.getNiveauRisque();

        if (ancien != nouveauNiveau) {
            family.setNiveauRisque(nouveauNiveau);
            familyRepository.save(family);

            Integer score = null;
            try {
                score = (Integer) getRiskAssessment(familyId).get("scoreRisque");
            } catch (Exception ignored) { /* lecture seule : pas bloquant */ }

            riskHistoryRepository.save(FamilyRiskHistory.builder()
                    .familyId(familyId)
                    .ancienNiveau(ancien)
                    .nouveauNiveau(nouveauNiveau)
                    .scoreRisque(score)
                    .changedBy(securityUtils.getCurrentUserId())
                    .raison(raison != null && !raison.isBlank() ? raison : "Modification manuelle par le pasteur")
                    .build());
        }
        return family;
    }

    @Transactional(readOnly = true)
    public List<FamilyRiskHistory> getRiskHistory(UUID familyId) {
        return riskHistoryRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
    }

    /** Compte les alertes actives d'une famille dont le titre/message contient le motif. */
    private long countAlertes(UUID familyId, String motif) {
        return alertRepository.findByFamilleId(familyId, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(a -> a.getStatut() == com.discipolat.common.enums.StatutAlerte.ACTIVE)
                .filter(a -> (a.getTypeAlerteManuel() != null && a.getTypeAlerteManuel().toUpperCase().contains(motif))
                        || (a.getTitre() != null && a.getTitre().toUpperCase().contains(motif))
                        || (a.getMessage() != null && a.getMessage().toUpperCase().contains(motif)))
                .count();
    }
}
