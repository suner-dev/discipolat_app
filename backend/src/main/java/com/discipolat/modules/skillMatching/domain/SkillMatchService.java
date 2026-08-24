package com.discipolat.modules.skillMatching.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P35 — Matching membres ↔ compétences.
 *
 * Le moteur analyse les compétences de chaque membre (SkillsMatrix)
 * et les besoins des départements pour proposer des matches.
 *
 * Score de match = sum(niveauCompétence * poidsBésoin) / sum(poidsBésoins)
 */
@Service
@Transactional
public class SkillMatchService {

    private final SkillMatchRepository repo;

    // Niveaux de compétence → score numérique
    private static final Map<String, Integer> NIVEAU_SCORE = Map.of(
            "DEBUTANT", 1,
            "INTERMEDIAIRE", 3,
            "AVANCE", 5,
            "EXPERT", 7
    );

    public SkillMatchService(SkillMatchRepository repo) { this.repo = repo; }

    public List<SkillMatch> listAll() {
        return repo.findByTenantIdOrderByScoreMatchDesc(TenantContext.getCurrentTenantId());
    }

    public SkillMatch get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("SkillMatch", id));
    }

    /**
     * Algorithme de matching amélioré :
     * 1. Charge les compétences déclarées de chaque membre (via paramètre)
     * 2. Charge les besoins de chaque département (via paramètre)
     * 3. Calcule un score pour chaque paire membre × département
     * 4. Propose les meilleurs matches
     *
     * @param membreSkills Map<membreId, Map<compétence, niveau>> — compétences de chaque membre
     * @param deptNeeds    Map<departementId, Map<compétence, poids>> — besoins de chaque département
     * @return Liste de matches proposés, triés par score décroissant
     */
    public List<SkillMatch> runMatching(Map<UUID, Map<String, String>> membreSkills,
                                         Map<UUID, Map<String, Integer>> deptNeeds) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<SkillMatch> matches = new ArrayList<>();

        for (Map.Entry<UUID, Map<String, Integer>> deptEntry : deptNeeds.entrySet()) {
            UUID deptId = deptEntry.getKey();
            Map<String, Integer> needs = deptEntry.getValue();

            for (Map.Entry<UUID, Map<String, String>> memberEntry : membreSkills.entrySet()) {
                UUID membreId = memberEntry.getKey();
                Map<String, String> skills = memberEntry.getValue();

                // Calcul du score de match
                double totalWeight = needs.values().stream().mapToInt(Integer::intValue).sum();
                if (totalWeight == 0) continue;

                double weightedScore = 0;
                StringBuilder justification = new StringBuilder();
                int matchedSkills = 0;

                for (Map.Entry<String, Integer> need : needs.entrySet()) {
                    String competence = need.getKey();
                    int poids = need.getValue();
                    String niveau = skills.get(competence);
                    if (niveau != null) {
                        int niveauScore = NIVEAU_SCORE.getOrDefault(niveau.toUpperCase(), 1);
                        weightedScore += (double) niveauScore / 7 * poids; // Normalisé sur 7
                        matchedSkills++;
                        if (justification.length() > 0) justification.append(" ; ");
                        justification.append(competence).append(": ").append(niveau);
                    }
                }

                int scoreFinal = (int) Math.round(weightedScore / totalWeight * 100);
                if (scoreFinal > 0 && matchedSkills > 0) {
                    // Vérifier si ce match existe déjà
                    boolean exists = repo.existsByTenantIdAndMembreIdAndDepartementIdAndCompetence(
                            tenantId, membreId, deptId, "GÉNÉRAL");
                    if (!exists) {
                        SkillMatch match = new SkillMatch();
                        match.setTenantId(tenantId);
                        match.setMembreId(membreId);
                        match.setDepartementId(deptId);
                        match.setCompetence("GÉNÉRAL");
                        match.setScoreMatch(scoreFinal);
                        match.setJustification(justification.toString());
                        matches.add(match);
                    }
                }
            }
        }

        // Trier par score décroissant et limiter aux 20 meilleurs
        matches.sort(Comparator.comparingInt(SkillMatch::getScoreMatch).reversed());
        List<SkillMatch> topMatches = matches.stream().limit(20).collect(Collectors.toList());

        return repo.saveAll(topMatches);
    }

    /**
     * Version simplifiée du matching (sans paramètres — pour compatibilité backward).
     * Retourne les matches existants triés par score.
     */
    public List<SkillMatch> runMatching() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return repo.findByTenantIdOrderByScoreMatchDesc(tenantId).stream()
                .filter(m -> m.getStatut() == SkillMatch.Statut.PROPOSE)
                .sorted(Comparator.comparingInt(SkillMatch::getScoreMatch).reversed())
                .collect(Collectors.toList());
    }

    public SkillMatch create(SkillMatch match) {
        match.setTenantId(TenantContext.getCurrentTenantId());
        return repo.save(match);
    }

    public SkillMatch respond(UUID id, SkillMatch.Statut decision) {
        SkillMatch m = get(id);
        m.setStatut(decision);
        m.setReponduLe(LocalDateTime.now());
        return repo.save(m);
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        List<SkillMatch> all = repo.findByTenantIdOrderByScoreMatchDesc(tenantId);
        stats.put("total", all.size());
        stats.put("proposes", all.stream().filter(m -> m.getStatut() == SkillMatch.Statut.PROPOSE).count());
        stats.put("acceptes", all.stream().filter(m -> m.getStatut() == SkillMatch.Statut.ACCEPTE).count());
        stats.put("refuses", all.stream().filter(m -> m.getStatut() == SkillMatch.Statut.REFUSE).count());
        stats.put("scoreMoyen", all.stream().mapToInt(SkillMatch::getScoreMatch).average().orElse(0));
        return stats;
    }
}
