package com.discipolat.modules.visits.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PastoralVisitService {

    private final PastoralVisitRepository repository;

    public PastoralVisitService(PastoralVisitRepository repository) {
        this.repository = repository;
    }

    public List<PastoralVisit> listByRange(LocalDateTime start, LocalDateTime end) {
        return repository.findByTenantIdAndPrévuLeBetweenOrderByPrévuLeAsc(
                TenantContext.getCurrentTenantId(), start, end);
    }

    public List<PastoralVisit> listByVisitor(UUID visiteurId, String statut) {
        if (statut != null) {
            return repository.findByVisiteurIdAndStatut(visiteurId, PastoralVisit.Statut.valueOf(statut));
        }            return repository.findByVisiteurIdAndStatut(visiteurId, PastoralVisit.Statut.PLANIFIEE);
    }

    public PastoralVisit getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PastoralVisit", id));
    }

    public PastoralVisit create(UUID visiteurId, UUID membreId, String motif, LocalDateTime prévuLe, String notes) {
        PastoralVisit visit = new PastoralVisit();
        visit.setTenantId(TenantContext.getCurrentTenantId());
        visit.setVisiteurId(visiteurId);
        visit.setMembreId(membreId);
        visit.setMotif(PastoralVisit.Motif.valueOf(motif != null ? motif : "ROUTINE"));
        visit.setPrévuLe(prévuLe);
        visit.setNotes(notes);
        return repository.save(visit);
    }

    public PastoralVisit complete(UUID id, String notes) {
        PastoralVisit visit = getById(id);
        visit.setStatut(PastoralVisit.Statut.REALISEE);
        visit.setRéaliséLe(LocalDateTime.now());
        visit.setNotes(notes);
        return repository.save(visit);
    }

    public PastoralVisit reschedule(UUID id, LocalDateTime newDate) {
        PastoralVisit visit = getById(id);
        visit.setStatut(PastoralVisit.Statut.REPORTEE);
        PastoralVisit newVisit = create(visit.getVisiteurId(), visit.getMembreId(),
                visit.getMotif().name(), newDate, visit.getNotes());
        newVisit.setAutoGénéré(true);
        return repository.save(newVisit);
    }

    public List<PastoralVisit> generateAutoVisits(List<UUID> membreIds, UUID visiteurId, int joursAVenir) {
        List<PastoralVisit> visits = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < membreIds.size(); i++) {
            LocalDateTime date = now.plusDays((long) (i / 3 + 1)); // 3 visits per day max
            PastoralVisit visit = create(visiteurId, membreIds.get(i), "ALERTE", date,
                    "Visite 自动生成 planifiée automatiquement");
            visit.setAutoGénéré(true);
            visits.add(visit);
        }
        return visits;
    }
}
