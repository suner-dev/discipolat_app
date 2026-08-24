package com.discipolat.modules.rewards.domain;

import com.discipolat.common.multitenancy.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * P3 #108 — Récompenses avancées : défis hebdomadaires (module weeklyChallenges),
 * certificats et mentions tangibles.
 */
@Service
@Transactional
public class CertificateService {

    private final CertificateRepository repository;

    @PersistenceContext
    private EntityManager em;

    public CertificateService(CertificateRepository repository) {
        this.repository = repository;
    }

    /** Certificats émis pour l'église courante. */
    @Transactional(readOnly = true)
    public List<Certificate> list() {
        return repository.findByTenantIdOrderByIssuedAtDesc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public List<Certificate> mine(UUID userId) {
        return repository.findByUserIdOrderByIssuedAtDesc(userId);
    }

    /** Émet un certificat manuellement (admin/pasteur). */
    public Certificate issue(Certificate c) {
        c.setId(null);
        c.setTenantId(TenantContext.getCurrentTenantId());
        c.setIssuedAt(LocalDateTime.now());
        c.setReference(generateReference(c));
        return repository.save(c);
    }

    /**
     * Certificats automatiquement mérités mais pas encore émis, calculés sur
     * des indicateurs réels : âmes suivies, présences aux événements, interactions.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> eligible(UUID userId) {
        long soulsFollowed = count("SELECT count(*) FROM souls WHERE faiseur_id = :p AND deleted = false", userId);
        long presences = count(
                "SELECT count(*) FROM event_registrations WHERE utilisateur_id = :p AND statut_inscription = 'PRESENT'", userId);
        long interactions = count("SELECT count(*) FROM soul_interactions WHERE auteur_id = :p", userId);

        List<Map<String, Object>> eligible = new ArrayList<>();
        if (soulsFollowed >= 5) {
            eligible.add(cert("Faiseur fidèle", "FIDELITE",
                    soulsFollowed + " âmes accompagnées avec constance."));
        }
        if (presences >= 10) {
            eligible.add(cert("Assiduité remarquable", "EXCELLENCE",
                    presences + " présences enregistrées aux événements."));
        }
        if (interactions >= 20) {
            eligible.add(cert("Évangélisateur actif", "HONNEUR",
                    interactions + " interactions de suivi réalisées."));
        }
        return eligible;
    }

    private static Map<String, Object> cert(String title, String mention, String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("mention", mention);
        m.put("description", description);
        return m;
    }

    private long count(String sql, UUID param) {
        return ((Number) em.createNativeQuery(sql).setParameter("p", param).getSingleResult()).longValue();
    }

    private String generateReference(Certificate c) {
        String prefix = c.getTitle() == null ? "CERT" : c.getTitle().replaceAll("[^A-Za-z]", "").substring(0, Math.min(4, c.getTitle().length())).toUpperCase();
        return prefix + "-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    }
}
