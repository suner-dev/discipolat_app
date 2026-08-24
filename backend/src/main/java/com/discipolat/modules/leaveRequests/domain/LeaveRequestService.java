package com.discipolat.modules.leaveRequests.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P1 #29 — Gestion des congés/absences avec workflow, impact calendrier, bulk approve.
 */
@Service
@Transactional
public class LeaveRequestService {

    private final LeaveRequestRepository repository;

    public LeaveRequestService(LeaveRequestRepository repository) {
        this.repository = repository;
    }

    public Page<LeaveRequest> list(Pageable pageable, String statut) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (statut != null) {
            return repository.findByTenantIdAndStatut(tenantId, LeaveRequest.Statut.valueOf(statut), pageable);
        }
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    public LeaveRequest getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("LeaveRequest", id));
    }

    public LeaveRequest create(String type, LocalDate dateDebut, LocalDate dateFin, String motif, UUID userId) {
        LeaveRequest req = new LeaveRequest();
        req.setTenantId(TenantContext.getCurrentTenantId());
        req.setType(LeaveRequest.Type.valueOf(type));
        req.setDateDebut(dateDebut);
        req.setDateFin(dateFin);
        req.setMotif(motif);
        req.setDemandeurId(userId);
        return repository.save(req);
    }

    public LeaveRequest approve(UUID id, UUID approverId) {
        LeaveRequest req = getById(id);
        req.setStatut(LeaveRequest.Statut.APPROUVE);
        req.setValideParId(approverId);
        req.setUpdatedAt(LocalDateTime.now());
        return repository.save(req);
    }

    public LeaveRequest reject(UUID id, UUID approverId, String commentaire) {
        LeaveRequest req = getById(id);
        req.setStatut(LeaveRequest.Statut.REFUSE);
        req.setValideParId(approverId);
        req.setCommentaire(commentaire);
        req.setUpdatedAt(LocalDateTime.now());
        return repository.save(req);
    }

    /**
     * Bulk approve multiple leave requests at once.
     */
    public List<LeaveRequest> bulkApprove(List<UUID> ids, UUID approverId) {
        List<LeaveRequest> approved = new ArrayList<>();
        for (UUID id : ids) {
            LeaveRequest req = getById(id);
            if (req.getStatut() == LeaveRequest.Statut.EN_ATTENTE) {
                req.setStatut(LeaveRequest.Statut.APPROUVE);
                req.setValideParId(approverId);
                req.setUpdatedAt(LocalDateTime.now());
                approved.add(repository.save(req));
            }
        }
        return approved;
    }

    /**
     * Check calendar impact: count how many team members are absent during a date range.
     */
    public Map<String, Object> checkCalendarImpact(LocalDate dateDebut, LocalDate dateFin) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<LeaveRequest> approved = repository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 200))
                .stream()
                .filter(r -> r.getStatut() == LeaveRequest.Statut.APPROUVE)
                .filter(r -> !r.getDateFin().isBefore(dateDebut) && !r.getDateDebut().isAfter(dateFin))
                .collect(Collectors.toList());

        Map<String, Object> impact = new HashMap<>();
        impact.put("absents", approved.size());
        impact.put("periode", Map.of("debut", dateDebut, "fin", dateFin));
        impact.put("types", approved.stream()
                .collect(Collectors.groupingBy(r -> r.getType().name(), Collectors.counting())));
        return impact;
    }

    /**
     * Get leave statistics for a member.
     */
    public Map<String, Object> getMemberStats(UUID membreId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        var requests = repository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 100))
                .stream()
                .filter(r -> membreId.equals(r.getDemandeurId()))
                .toList();

        long totalDays = requests.stream()
                .filter(r -> r.getStatut() == LeaveRequest.Statut.APPROUVE)
                .mapToLong(r -> java.time.temporal.ChronoUnit.DAYS.between(r.getDateDebut(), r.getDateFin()) + 1)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", requests.size());
        stats.put("approved", requests.stream().filter(r -> r.getStatut() == LeaveRequest.Statut.APPROUVE).count());
        stats.put("pending", requests.stream().filter(r -> r.getStatut() == LeaveRequest.Statut.EN_ATTENTE).count());
        stats.put("rejected", requests.stream().filter(r -> r.getStatut() == LeaveRequest.Statut.REFUSE).count());
        stats.put("totalDaysTaken", totalDays);
        return stats;
    }

    public LeaveRequest cancel(UUID id, UUID userId) {
        LeaveRequest req = getById(id);
        req.setStatut(LeaveRequest.Statut.ANNULE);
        req.setUpdatedAt(LocalDateTime.now());
        return repository.save(req);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
