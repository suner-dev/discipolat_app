package com.discipolat.modules.gantt.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class TeamAssignmentService {

    private final TeamAssignmentRepository repository;

    public TeamAssignmentService(TeamAssignmentRepository repository) {
        this.repository = repository;
    }

    public List<TeamAssignment> listByRange(LocalDateTime start, LocalDateTime end) {
        return repository.findByTenantIdAndDébutBetweenOrderByDébutAsc(
                TenantContext.getCurrentTenantId(), start, end);
    }

    public List<TeamAssignment> listByTeam(UUID équipeId) {
        return repository.findByÉquipeId(équipeId);
    }

    public TeamAssignment getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TeamAssignment", id));
    }

    public TeamAssignment create(UUID équipeId, UUID événementId, String rôle,
                                   UUID membreId, LocalDateTime début, LocalDateTime fin, String notes) {
        TeamAssignment assignment = new TeamAssignment();
        assignment.setTenantId(TenantContext.getCurrentTenantId());
        assignment.setÉquipeId(équipeId);
        assignment.setÉvénementId(événementId);
        assignment.setRôle(rôle);
        assignment.setMembreId(membreId);
        assignment.setDébut(début);
        assignment.setFin(fin);
        assignment.setNotes(notes);
        return repository.save(assignment);
    }

    public TeamAssignment updateStatut(UUID id, String statut) {
        TeamAssignment assignment = getById(id);
        assignment.setStatut(TeamAssignment.Statut.valueOf(statut));
        return repository.save(assignment);
    }

    public void delete(UUID id) {
        repository.delete(getById(id));
    }

    public Map<String, Object> detectOverloads(UUID équipeId, LocalDateTime start, LocalDateTime end) {
        List<TeamAssignment> assignments = listByRange(start, end);
        Map<UUID, Long> countByMember = new HashMap<>();
        for (TeamAssignment a : assignments) {
            if (a.getMembreId() != null) {
                countByMember.merge(a.getMembreId(), 1L, Long::sum);
            }
        }
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> overloads = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : countByMember.entrySet()) {
            if (entry.getValue() > 5) { // threshold
                overloads.add(Map.of("membreId", entry.getKey(), "assignations", entry.getValue()));
            }
        }
        result.put("overloads", overloads);
        result.put("totalAssignments", assignments.size());
        return result;
    }
}
