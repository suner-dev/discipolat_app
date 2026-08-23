package com.discipolat.modules.calendar.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CalendarService {

    private final CalendarEventRepository repository;

    public CalendarService(CalendarEventRepository repository) {
        this.repository = repository;
    }

    public Page<CalendarEvent> list(Pageable pageable) {
        return repository.findByTenantIdOrderByDateDebutAsc(TenantContext.getCurrentTenantId(), pageable);
    }

    public List<CalendarEvent> getBetween(LocalDateTime start, LocalDateTime end) {
        return repository.findByTenantIdAndDateDebutBetween(TenantContext.getCurrentTenantId(), start, end);
    }

    public CalendarEvent getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("CalendarEvent", id));
    }

    public CalendarEvent create(String titre, String description, LocalDateTime dateDebut, LocalDateTime dateFin,
                                String lieu, String categorie, UUID userId) {
        CalendarEvent event = new CalendarEvent();
        event.setTenantId(TenantContext.getCurrentTenantId());
        event.setTitre(titre);
        event.setDescription(description);
        event.setDateDebut(dateDebut);
        event.setDateFin(dateFin);
        event.setLieu(lieu);
        event.setCategorie(categorie);
        event.setCreePar(userId);
        return repository.save(event);
    }

    public CalendarEvent update(UUID id, String titre, String description, LocalDateTime dateDebut,
                                LocalDateTime dateFin, String lieu) {
        CalendarEvent event = getById(id);
        if (titre != null) event.setTitre(titre);
        if (description != null) event.setDescription(description);
        if (dateDebut != null) event.setDateDebut(dateDebut);
        if (dateFin != null) event.setDateFin(dateFin);
        if (lieu != null) event.setLieu(lieu);
        return repository.save(event);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
