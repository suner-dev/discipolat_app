package com.discipolat.modules.prayerJournal.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class PrayerJournalService {

    private final PrayerJournalRepository repository;

    public PrayerJournalService(PrayerJournalRepository repository) {
        this.repository = repository;
    }

    public Page<PrayerJournalEntry> listByMember(UUID membreId, Pageable pageable) {
        return repository.findByMembreIdOrderByCreatedAtDesc(membreId, pageable);
    }

    public PrayerJournalEntry getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PrayerJournalEntry", id));
    }

    public PrayerJournalEntry create(UUID membreId, String contenu, String category, String visibilité) {
        PrayerJournalEntry entry = new PrayerJournalEntry();
        entry.setTenantId(TenantContext.getCurrentTenantId());
        entry.setMembreId(membreId);
        entry.setContenu(contenu);
        entry.setCategory(category);
        entry.setVisibilité(PrayerJournalEntry.Visibilité.valueOf(visibilité != null ? visibilité : "PRIVÉE"));
        entry.setStatut(PrayerJournalEntry.Statut.EN_COURS);
        return repository.save(entry);
    }

    public PrayerJournalEntry markAnswered(UUID id, String réponse) {
        PrayerJournalEntry entry = getById(id);
        entry.setStatut(PrayerJournalEntry.Statut.EXAUCÉE);
        entry.setRéponse(réponse);
        entry.setExaucéeAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        return repository.save(entry);
    }

    public PrayerJournalEntry markRemembered(UUID id) {
        PrayerJournalEntry entry = getById(id);
        entry.setStatut(PrayerJournalEntry.Statut.MÉMORISÉE);
        entry.setUpdatedAt(LocalDateTime.now());
        return repository.save(entry);
    }

    public void delete(UUID id) {
        PrayerJournalEntry entry = getById(id);
        repository.delete(entry);
    }

    public Map<String, Object> getStats(UUID membreId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", repository.countByTenantIdAndMembreId(tenantId, membreId));
        stats.put("enCours", repository.countByTenantIdAndMembreIdAndStatut(tenantId, membreId, PrayerJournalEntry.Statut.EN_COURS));
        stats.put("exaucées", repository.countByTenantIdAndMembreIdAndStatut(tenantId, membreId, PrayerJournalEntry.Statut.EXAUCÉE));
        stats.put("mémorisées", repository.countByTenantIdAndMembreIdAndStatut(tenantId, membreId, PrayerJournalEntry.Statut.MÉMORISÉE));
        return stats;
    }
}
