package com.discipolat.modules.mentoring.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service de mentorat IA pour les chefs de famille.
 *
 * Le moteur analyse le profil de chaque faiseur (performances, forces, zones de croissance)
 * et génère des suggestions d'accompagnement personnalisées.
 *
 * En production, ce service serait connecté à Ollama/LLM pour des suggestions plus riches.
 * Ici, on utilise un moteur de règles déterministe qui produit déjà des recommandations
 * pertinentes basées sur les données disponibles.
 */
@Service
@Transactional
public class MentoringService {

    private final MentorSuggestionRepository repository;

    public MentoringService(MentorSuggestionRepository repository) {
        this.repository = repository;
    }

    public Page<MentorSuggestion> listSuggestions(UUID chefDeFamilleId, Pageable pageable) {
        return repository.findByChefDeFamilleIdAndStatutOrderByPrioritéAscCreatedAtDesc(
                chefDeFamilleId, MentorSuggestion.Statut.ACTIVE, pageable);
    }

    public List<MentorSuggestion> listAllSuggestions(UUID chefDeFamilleId) {
        return repository.findByChefDeFamilleIdOrderByPrioritéAscCreatedAtDesc(chefDeFamilleId);
    }

    public MentorSuggestion getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MentorSuggestion", id));
    }

    /**
     * Génère des suggestions IA pour un chef de famille sur la base de ses faiseurs.
     *
     * @param chefDeFamilleId ID du chef de famille
     * @param faiseursData    Données simulées des faiseurs (en prod: fetch depuis SoulRepository)
     * @return Liste de suggestions générées
     */
    public List<MentorSuggestion> generateSuggestions(UUID chefDeFamilleId, List<Map<String, Object>> faiseursData) {
        List<MentorSuggestion> suggestions = new ArrayList<>();

        for (Map<String, Object> faiseur : faiseursData) {
            UUID faiseurId = UUID.fromString((String) faiseur.get("id"));
            String nom = (String) faiseur.getOrDefault("nom", "Faiseur");
            int disciples = faiseur.get("disciples") != null ? (int) faiseur.get("disciples") : 0;
            int rapports = faiseur.get("rapportsSoumis") != null ? (int) faiseur.get("rapportsSoumis") : 0;
            double scoreMoyen = faiseur.get("scoreMoyen") != null ? (double) faiseur.get("scoreMoyen") : 50;
            int formations = faiseur.get("formationsSuivies") != null ? (int) faiseur.get("formationsSuivies") : 0;
            int derniersJours = faiseur.get("joursDepuisDernierContact") != null ? (int) faiseur.get("joursDepuisDernierContact") : 0;

            // ── Règle 1 : Charge élevée ──
            if (disciples > 8) {
                suggestions.add(build(chefDeFamilleId, faiseurId,
                        MentorSuggestion.Priorité.HAUTE,
                        MentorSuggestion.Catégorie.DÉLÉGATION,
                        "Charge élevée pour " + nom,
                        nom + " suit " + disciples + " disciples, ce qui dépasse le seuil recommandé de 8.",
                        "Envisagez de déléguer 2-3 disciples à un autre faiseur pour maintenir la qualité de l'accompagnement.",
                        "La charge idéale est de 5-8 disciples par faiseur pour un suivi de qualité.",
                        0.9));
            }

            // ── Règle 2 : Rapports en retard ──
            if (rapports < 2) {
                suggestions.add(build(chefDeFamilleId, faiseurId,
                        MentorSuggestion.Priorité.HAUTE,
                        MentorSuggestion.Catégorie.ACCOMPAGNEMENT,
                        "Rapports insuffisants de " + nom,
                        nom + " n'a soumis que " + rapports + " rapports ce mois.",
                        "Planifiez un entretien avec " + nom + " pour comprendre les obstacles et l'encourager.",
                        "Les rapports réguliers sont essentiels pour le suivi pastoral.",
                        0.85));
            }

            // ── Règle 3 : Score en baisse ──
            if (scoreMoyen < 40) {
                suggestions.add(build(chefDeFamilleId, faiseurId,
                        MentorSuggestion.Priorité.HAUTE,
                        MentorSuggestion.Catégorie.MISE_EN_GARDIEN,
                        "Score spirituel bas — " + nom,
                        "Le score moyen des disciples de " + nom + " est de " + String.format("%.0f", scoreMoyen) + "/100.",
                        "Identifiez les disciples à risque et proposez un plan d'action correctif à " + nom + ".",
                        "Un score en dessous de 40 indique un risque de décrochage.",
                        0.95));
            }

            // ── Règle 4 : Formations insuffisantes ──
            if (formations < 2) {
                suggestions.add(build(chefDeFamilleId, faiseurId,
                        MentorSuggestion.Priorité.MOYENNE,
                        MentorSuggestion.Catégorie.FORMATION,
                        "Formations à suivre pour " + nom,
                        nom + " n'a suivi que " + formations + " formation(s) ce trimestre.",
                        "Recommandez-lui les formations sur l'accompagnement des nouveaux convertis.",
                        "La formation continue renforce les compétences du faiseur.",
                        0.7));
            }

            // ── Règle 5 : Contact distant ──
            if (derniersJours > 14) {
                suggestions.add(build(chefDeFamilleId, faiseurId,
                        MentorSuggestion.Priorité.MOYENNE,
                        MentorSuggestion.Catégorie.ACCOMPAGNEMENT,
                        "Pas de contact depuis " + derniersJours + " jours — " + nom,
                        nom + " n'a eu aucun contact enregistré depuis " + derniersJours + " jours.",
                        "Envoyez un message d'encouragement ou organisez un point rapide.",
                        "Un contact régulier maintient la motivation du faiseur.",
                        0.8));
            }

            // ── Règle 6 : Bonne performance → reconnaissance ──
            if (scoreMoyen >= 70 && disciples >= 5 && rapports >= 3) {
                suggestions.add(build(chefDeFamilleId, faiseurId,
                        MentorSuggestion.Priorité.BASSE,
                        MentorSuggestion.Catégorie.RECONNAISSANCE,
                        "Excellente performance de " + nom + " !",
                        nom + " maintient un score moyen de " + String.format("%.0f", scoreMoyen) + "/100 avec " + disciples + " disciples suivis.",
                        "Envoyez un message de reconnaissance ou proposez-lui de mentorer un nouveau faiseur.",
                        "La reconnaissance renforce l'engagement et la fidélité des faiseurs.",
                        0.9));
            }
        }

        return repository.saveAll(suggestions);
    }

    private MentorSuggestion build(UUID chefId, UUID faiseurId, MentorSuggestion.Priorité priorité,
                                    MentorSuggestion.Catégorie catégorie, String titre,
                                    String analyse, String action, String raison, double confiance) {
        MentorSuggestion s = new MentorSuggestion();
        s.setTenantId(TenantContext.getCurrentTenantId());
        s.setChefDeFamilleId(chefId);
        s.setFaiseurId(faiseurId);
        s.setPriorité(priorité);
        s.setCatégorie(catégorie);
        s.setTitre(titre);
        s.setAnalyse(analyse);
        s.setActionRecommandée(action);
        s.setRaisonnement(raison);
        s.setConfiance(confiance);
        return s;
    }

    public void markAsRead(UUID id) {
        MentorSuggestion s = getById(id);
        s.setStatut(MentorSuggestion.Statut.LUE);
        repository.save(s);
    }

    public void archive(UUID id) {
        MentorSuggestion s = getById(id);
        s.setStatut(MentorSuggestion.Statut.ARCHIVÉE);
        repository.save(s);
    }

    public Map<String, Object> getStats(UUID chefDeFamilleId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("actives", repository.countByChefDeFamilleIdAndStatut(chefDeFamilleId, MentorSuggestion.Statut.ACTIVE));
        stats.put("lues", repository.countByChefDeFamilleIdAndStatut(chefDeFamilleId, MentorSuggestion.Statut.LUE));
        stats.put("archivées", repository.countByChefDeFamilleIdAndStatut(chefDeFamilleId, MentorSuggestion.Statut.ARCHIVÉE));
        return stats;
    }
}
