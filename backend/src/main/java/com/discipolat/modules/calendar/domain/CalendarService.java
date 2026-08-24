package com.discipolat.modules.calendar.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
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

    public List<CalendarEvent> listByRange(LocalDateTime start, LocalDateTime end) {
        return repository.findByTenantIdAndDébutBetweenOrderByDébutAsc(
                TenantContext.getCurrentTenantId(), start, end);
    }

    public List<CalendarEvent> listAll() {
        return repository.findByTenantIdOrderByDébutAsc(TenantContext.getCurrentTenantId());
    }

    public CalendarEvent getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CalendarEvent", id));
    }

    public CalendarEvent create(String titre, String description, LocalDateTime début, LocalDateTime fin,
                                 String lieu, String source, UUID événementId) {
        CalendarEvent event = new CalendarEvent();
        event.setTenantId(TenantContext.getCurrentTenantId());
        event.setTitre(titre);
        event.setDescription(description);
        event.setDébut(début);
        event.setFin(fin);
        event.setLieu(lieu);
        event.setSource(CalendarEvent.Source.valueOf(source != null ? source : "INTERNE"));
        event.setÉvénementId(événementId);
        return repository.save(event);
    }

    public CalendarEvent updateStatut(UUID id, String statut) {
        CalendarEvent event = getById(id);
        event.setStatut(CalendarEvent.Statut.valueOf(statut));
        return repository.save(event);
    }

    public void delete(UUID id) {
        repository.delete(getById(id));
    }

    public String generateICal(UUID id) {
        CalendarEvent event = getById(id);
        return "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:" + event.getDébut().toString().replace("-", "").replace(":", "") + "\n" +
                "DTEND:" + event.getFin().toString().replace("-", "").replace(":", "") + "\n" +
                "SUMMARY:" + event.getTitre() + "\n" +
                "DESCRIPTION:" + (event.getDescription() != null ? event.getDescription() : "") + "\n" +
                "LOCATION:" + (event.getLieu() != null ? event.getLieu() : "") + "\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";
    }
}
