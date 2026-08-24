package com.discipolat.modules.personalObjectives.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class PersonalObjectiveService {

    private final PersonalObjectiveRepository repository;

    public PersonalObjectiveService(PersonalObjectiveRepository repository) {
        this.repository = repository;
    }

    public List<PersonalObjective> listByMember(UUID membreId) {
        return repository.findByMembreIdOrderByCreatedAtDesc(membreId);
    }

    public PersonalObjective getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PersonalObjective", id));
    }

    public PersonalObjective create(UUID membreId, String titre, String description, String catégorie,
                                     int objectifCible, LocalDateTime deadline) {
        PersonalObjective obj = new PersonalObjective();
        obj.setTenantId(TenantContext.getCurrentTenantId());
        obj.setMembreId(membreId);
        obj.setTitre(titre);
        obj.setDescription(description);
        obj.setCatégorie(PersonalObjective.Catégorie.valueOf(catégorie != null ? catégorie : "AUTRE"));
        obj.setObjectifCible(objectifCible > 0 ? objectifCible : 1);
        obj.setDeadline(deadline);
        return repository.save(obj);
    }

    public PersonalObjective progress(UUID id) {
        PersonalObjective obj = getById(id);
        obj.setProgressionActuelle(obj.getProgressionActuelle() + 1);
        if (obj.getProgressionActuelle() >= obj.getObjectifCible()) {
            obj.setStatut(PersonalObjective.Statut.ATTEINT);
            obj.setCompletedAt(LocalDateTime.now());
        }
        return repository.save(obj);
    }

    public PersonalObjective updateStatut(UUID id, String statut) {
        PersonalObjective obj = getById(id);
        obj.setStatut(PersonalObjective.Statut.valueOf(statut));
        if (statut.equals("ATTEINT")) {
            obj.setCompletedAt(LocalDateTime.now());
        }
        return repository.save(obj);
    }

    public Map<String, Object> getStats(UUID membreId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", repository.countByTenantIdAndMembreId(tenantId, membreId));
        stats.put("enCours", repository.countByTenantIdAndMembreIdAndStatut(tenantId, membreId, PersonalObjective.Statut.EN_COURS));
        stats.put("atteints", repository.countByTenantIdAndMembreIdAndStatut(tenantId, membreId, PersonalObjective.Statut.ATTEINT));
        return stats;
    }
}
