package com.discipolat.modules.churchComparison.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional
public class ChurchComparisonService {

    private final ChurchComparisonRepository repo;

    public ChurchComparisonService(ChurchComparisonRepository repo) { this.repo = repo; }

    public List<ChurchComparison> listAll() {
        return repo.findByTenantIdOrderByEffectifDesc(TenantContext.getCurrentTenantId());
    }

    public List<ChurchComparison> listByCategory(String cat) {
        return repo.findByCategorieOrderByEffectifDesc(cat);
    }

    public List<ChurchComparison> listByCountry(String pays) {
        return repo.findByPaysOrderByEffectifDesc(pays);
    }

    public ChurchComparison get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("ChurchComparison", id));
    }

    public ChurchComparison save(ChurchComparison c) {
        c.setTenantId(TenantContext.getCurrentTenantId());
        return repo.save(c);
    }

    public void delete(UUID id) { repo.deleteById(id); }

    /** Benchmark: compare our church to average of same category */
    public Map<String, Object> benchmark(UUID ourId) {
        ChurchComparison ours = get(ourId);
        List<ChurchComparison> peers = repo.findByCategorieOrderByEffectifDesc(ours.getCategorie());
        if (peers.isEmpty()) return Map.of("message", "Pas de pairs pour comparaison");

        double avgPresence = peers.stream().mapToDouble(ChurchComparison::getTauxPresence).average().orElse(0);
        double avgRetention = peers.stream().mapToDouble(ChurchComparison::getTauxRetention).average().orElse(0);
        double avgConversion = peers.stream().mapToDouble(ChurchComparison::getTauxConversion).average().orElse(0);
        double avgScore = peers.stream().mapToDouble(ChurchComparison::getScoreSpirituelMoyen).average().orElse(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("notreEglise", ours.getNomEglise());
        result.put("nbPairs", peers.size());
        result.put("presence", Map.of("nous", ours.getTauxPresence(), "moyenne", Math.round(avgPresence * 100.0) / 100.0));
        result.put("retention", Map.of("nous", ours.getTauxRetention(), "moyenne", Math.round(avgRetention * 100.0) / 100.0));
        result.put("conversion", Map.of("nous", ours.getTauxConversion(), "moyenne", Math.round(avgConversion * 100.0) / 100.0));
        result.put("scoreSpirituel", Map.of("nous", ours.getScoreSpirituelMoyen(), "moyenne", Math.round(avgScore * 100.0) / 100.0));
        return result;
    }

    // ======================== P3 #107 — BENCHMARK AMÉLIORÉ (CLUSTERING) ========================

    /**
     * Clustering anonyme des églises par taille / pays / dénomination (k-means léger
     * sur l'effectif, k = 3 : petite / moyenne / grande), avec rang percentile
     * de notre église dans son cluster.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> clusters(UUID ourId) {
        List<ChurchComparison> all = repo.findAll();
        if (all.isEmpty()) return Map.of("message", "Aucune église dans le benchmark");

        // Clustering par taille (k=3 sur effectif)
        List<Integer> sizes = all.stream().map(ChurchComparison::getEffectif).sorted().toList();
        int q1 = sizes.get(sizes.size() / 3);
        int q2 = sizes.get(2 * sizes.size() / 3);

        Map<String, List<ChurchComparison>> clusters = new LinkedHashMap<>();
        clusters.put("PETITE", new ArrayList<>());
        clusters.put("MOYENNE", new ArrayList<>());
        clusters.put("GRANDE", new ArrayList<>());
        for (ChurchComparison c : all) {
            String key = c.getEffectif() < q1 ? "PETITE" : c.getEffectif() < q2 ? "MOYENNE" : "GRANDE";
            clusters.get(key).add(c);
        }

        // Regroupement secondaire par pays et dénomination
        Map<String, Long> byCountry = all.stream()
                .collect(Collectors.groupingBy(c -> c.getPays() == null ? "?" : c.getPays(), Collectors.counting()));
        Map<String, Long> byDenomination = all.stream()
                .collect(Collectors.groupingBy(c -> c.getDenomination() == null ? "?" : c.getDenomination(),
                        Collectors.counting()));

        List<Map<String, Object>> clusterStats = new ArrayList<>();
        String ourCluster = null;
        Integer ourRank = null;
        Integer ourClusterSize = null;

        for (Map.Entry<String, List<ChurchComparison>> e : clusters.entrySet()) {
            List<ChurchComparison> group = e.getValue();
            Map<String, Object> cs = new LinkedHashMap<>();
            cs.put("cluster", e.getKey());
            cs.put("seuilEffectif", e.getKey().equals("PETITE") ? "< " + q1
                    : e.getKey().equals("MOYENNE") ? q1 + "–" + q2 : ">= " + q2);
            cs.put("nbEglises", group.size());
            cs.put("effectifMoyen", group.stream().mapToInt(ChurchComparison::getEffectif).average().orElse(0));
            cs.put("tauxPresenceMoyen", round(group.stream().mapToDouble(ChurchComparison::getTauxPresence).average().orElse(0)));
            cs.put("tauxRetentionMoyen", round(group.stream().mapToDouble(ChurchComparison::getTauxRetention).average().orElse(0)));
            cs.put("scoreSpirituelMoyen", round(group.stream().mapToDouble(ChurchComparison::getScoreSpirituelMoyen).average().orElse(0)));
            clusterStats.add(cs);

            if (ourId != null && group.stream().anyMatch(c -> c.getId().equals(ourId))) {
                ourCluster = e.getKey();
                ourClusterSize = group.size();
                long betterThanUs = group.stream()
                        .filter(c -> c.getScoreSpirituelMoyen() > get(ourId).getScoreSpirituelMoyen()).count();
                ourRank = (int) betterThanUs + 1;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clusters", clusterStats);
        result.put("repartitionParPays", byCountry);
        result.put("repartitionParDenomination", byDenomination);
        if (ourCluster != null) {
            result.put("notreEglise", get(ourId).getNomEglise());
            result.put("notreCluster", ourCluster);
            result.put("notRang", ourRank);
            result.put("tailleCluster", ourClusterSize);
            result.put("anonymat", "Comparaison strictement anonyme — aucune identité d'église n'est exposée.");
        }
        return result;
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}

