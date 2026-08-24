package com.discipolat.modules.spiritualJournal.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SpiritualJournalService {

    private final SpiritualJournalRepository repo;

    public SpiritualJournalService(SpiritualJournalRepository repo) { this.repo = repo; }

    public List<SpiritualJournal> listByAuthor(UUID authorId) {
        return repo.findByAuteurIdOrderByDateEntreeDesc(authorId);
    }

    public List<SpiritualJournal> listByType(UUID authorId, SpiritualJournal.TypeEntree type) {
        return repo.findByAuteurIdAndTypeEntreeOrderByDateEntreeDesc(authorId, type);
    }

    public List<SpiritualJournal> listFavorites(UUID authorId) {
        return repo.findByAuteurIdAndFavoriTrueOrderByDateEntreeDesc(authorId);
    }

    public SpiritualJournal get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("SpiritualJournal", id));
    }

    public SpiritualJournal create(SpiritualJournal entry) {
        entry.setTenantId(TenantContext.getCurrentTenantId());
        entry.setCreeLe(LocalDateTime.now());
        return repo.save(entry);
    }

    public SpiritualJournal update(UUID id, SpiritualJournal updates) {
        SpiritualJournal j = get(id);
        if (updates.getTitre() != null) j.setTitre(updates.getTitre());
        if (updates.getContenu() != null) j.setContenu(updates.getContenu());
        if (updates.getTypeEntree() != null) j.setTypeEntree(updates.getTypeEntree());
        if (updates.getDateEntree() != null) j.setDateEntree(updates.getDateEntree());
        j.setPublique(updates.isPublique());
        j.setFavori(updates.isFavori());
        j.setModifieLe(LocalDateTime.now());
        return repo.save(j);
    }

    public void delete(UUID id) { repo.deleteById(id); }

    public SpiritualJournal toggleFavorite(UUID id) {
        SpiritualJournal j = get(id);
        j.setFavori(!j.isFavori());
        j.setModifieLe(LocalDateTime.now());
        return repo.save(j);
    }

    public Map<String, Object> getStats(UUID authorId) {
        Map<String, Object> stats = new HashMap<>();
        List<SpiritualJournal> entries = repo.findByAuteurIdOrderByDateEntreeDesc(authorId);
        stats.put("total", entries.size());
        stats.put("favoris", entries.stream().filter(SpiritualJournal::isFavori).count());
        stats.put("types", entries.stream()
                .collect(Collectors.groupingBy(SpiritualJournal::getTypeEntree, Collectors.counting())));
        stats.put("streak", calculateStreak(entries));
        return stats;
    }

    private int calculateStreak(List<SpiritualJournal> entries) {
        Set<LocalDate> dates = entries.stream()
                .map(SpiritualJournal::getDateEntree).collect(Collectors.toSet());
        int streak = 0;
        LocalDate d = LocalDate.now();
        while (dates.contains(d)) { streak++; d = d.minusDays(1); }
        return streak;
    }
}
