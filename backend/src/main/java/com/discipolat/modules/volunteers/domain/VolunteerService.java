package com.discipolat.modules.volunteers.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class VolunteerService {

    private final VolunteerRepository repo;

    public VolunteerService(VolunteerRepository repo) { this.repo = repo; }

    public List<Volunteer> listActive() {
        return repo.findByTenantIdAndStatutOrderByInscritLeDesc(
                TenantContext.getCurrentTenantId(), Volunteer.Statut.ACTIF);
    }

    public List<Volunteer> listAll() {
        return repo.findByTenantIdAndStatutOrderByInscritLeDesc(
                TenantContext.getCurrentTenantId(), Volunteer.Statut.ACTIF);
    }

    public Volunteer get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Volunteer", id));
    }

    public Volunteer create(Volunteer v) {
        v.setTenantId(TenantContext.getCurrentTenantId());
        return repo.save(v);
    }

    public Volunteer update(UUID id, Volunteer updates) {
        Volunteer v = get(id);
        if (updates.getStatut() != null) v.setStatut(updates.getStatut());
        if (updates.getDisponibilite() != null) v.setDisponibilite(updates.getDisponibilite());
        if (updates.getCompetencesJson() != null) v.setCompetencesJson(updates.getCompetencesJson());
        if (updates.getDomainesInteretJson() != null) v.setDomainesInteretJson(updates.getDomainesInteretJson());
        v.setHeuresMois(updates.getHeuresMois());
        return repo.save(v);
    }

    public void delete(UUID id) { repo.deleteById(id); }

    /** Match volunteers to an event based on skills and availability */
    public List<Volunteer> matchForEvent(String skill, Volunteer.Disponibilite disponibilite) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return repo.findByTenantIdAndStatutOrderByInscritLeDesc(tenantId, Volunteer.Statut.ACTIF)
                .stream()
                .filter(v -> skill == null || v.getCompetencesJson().contains(skill))
                .filter(v -> disponibilite == null || v.getDisponibilite() == disponibilite)
                .sorted(Comparator.comparingInt(Volunteer::getNbEvenements).reversed())
                .toList();
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", repo.countByTenantIdAndStatut(tenantId, Volunteer.Statut.ACTIF));
        stats.put("inactifs", repo.countByTenantIdAndStatut(tenantId, Volunteer.Statut.INACTIF));
        stats.put("enAttente", repo.countByTenantIdAndStatut(tenantId, Volunteer.Statut.EN_ATTENTE));
        return stats;
    }
}
