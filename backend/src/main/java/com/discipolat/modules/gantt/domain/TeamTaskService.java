package com.discipolat.modules.gantt.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.api.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class TeamTaskService {

    private final TeamTaskRepository repository;

    public TeamTaskService(TeamTaskRepository repository) {
        this.repository = repository;
    }

    public List<TeamTask> list() {
        return repository.findByTenantIdOrderByDateDebutAsc(TenantContext.getCurrentTenantId());
    }

    public TeamTask getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("TeamTask", id));
    }

    public TeamTask create(String titre, String description, String priorite, LocalDate dateDebut,
                           LocalDate dateFin, UUID assigneA, UUID departmentId, UUID userId) {
        TeamTask task = new TeamTask();
        task.setTenantId(TenantContext.getCurrentTenantId());
        task.setTitre(titre);
        task.setDescription(description);
        task.setPriorite(TeamTask.Priorite.valueOf(priorite));
        task.setDateDebut(dateDebut);
        task.setDateFin(dateFin);
        task.setAssigneA(assigneA);
        task.setDepartmentId(departmentId);
        task.setCreePar(userId);
        return repository.save(task);
    }

    public TeamTask updateStatut(UUID id, String statut) {
        TeamTask task = getById(id);
        task.setStatut(TeamTask.Statut.valueOf(statut));
        return repository.save(task);
    }

    public TeamTask updateProgression(UUID id, int progression) {
        TeamTask task = getById(id);
        task.setProgression(Math.min(100, Math.max(0, progression)));
        if (progression >= 100) task.setStatut(TeamTask.Statut.TERMINE);
        else if (progression > 0) task.setStatut(TeamTask.Statut.EN_COURS);
        return repository.save(task);
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("aFaire", repository.countByTenantIdAndStatut(tenantId, TeamTask.Statut.A_FAIRE));
        stats.put("enCours", repository.countByTenantIdAndStatut(tenantId, TeamTask.Statut.EN_COURS));
        stats.put("termine", repository.countByTenantIdAndStatut(tenantId, TeamTask.Statut.TERMINE));
        stats.put("enRetard", repository.countByTenantIdAndStatut(tenantId, TeamTask.Statut.EN_RETARD));
        return stats;
    }
}
