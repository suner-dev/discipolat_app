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
        return formatICalSingleEvent(event);
    }

    /**
     * Génère un flux iCal complet (RFC 5545) avec tous les événements actifs.
     * Compatible Google Calendar, Outlook, Apple Calendar (subscription URL).
     */
    public String generateICalFeed() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<CalendarEvent> events = repository.findByTenantIdOrderByDébutAsc(tenantId);
        StringBuilder ical = new StringBuilder();
        ical.append("BEGIN:VCALENDAR\r\n");
        ical.append("VERSION:2.0\r\n");
        ical.append("PRODID:-//Discipolat//Calendar Feed//FR\r\n");
        ical.append("CALSCALE:GREGORIAN\r\n");
        ical.append("METHOD:PUBLISH\r\n");
        ical.append("X-WR-CALNAME:Discipolat - Église\r\n");
        ical.append("X-WR-TIMEZONE:UTC\r\n");
        for (CalendarEvent event : events) {
            if (event.getStatut() != CalendarEvent.Statut.ANNULÉ) {
                ical.append(formatICalEvent(event));
            }
        }
        ical.append("END:VCALENDAR");
        return ical.toString();
    }

    /**
     * Génère un fichier iCal pour un seul événement (RFC 5545 complet).
     */
    private String formatICalSingleEvent(CalendarEvent event) {
        StringBuilder ical = new StringBuilder();
        ical.append("BEGIN:VCALENDAR\r\n");
        ical.append("VERSION:2.0\r\n");
        ical.append("PRODID:-//Discipolat//Calendar//FR\r\n");
        ical.append("CALSCALE:GREGORIAN\r\n");
        ical.append(formatICalEvent(event));
        ical.append("END:VCALENDAR");
        return ical.toString();
    }

    private String formatICalEvent(CalendarEvent event) {
        StringBuilder e = new StringBuilder();
        e.append("BEGIN:VEVENT\r\n");
        e.append("UID:").append(event.getId()).append("@discipolat.com\r\n");
        e.append("DTSTAMP:").append(java.time.Instant.now().toString().replace("-", "").replace(":", "").replace(".", "Z") + "\r\n");
        e.append("DTSTART:").append(formatICalDate(event.getDébut())).append("\r\n");
        e.append("DTEND:").append(formatICalDate(event.getFin())).append("\r\n");
        e.append("SUMMARY:").append(escapeICal(event.getTitre())).append("\r\n");
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            e.append("DESCRIPTION:").append(escapeICal(event.getDescription())).append("\r\n");
        }
        if (event.getLieu() != null && !event.getLieu().isBlank()) {
            e.append("LOCATION:").append(escapeICal(event.getLieu())).append("\r\n");
        }
        if (event.isRappelActivé()) {
            e.append("BEGIN:VALARM\r\n");
            e.append("TRIGGER:-PT").append(event.getRappelMinutesAvant()).append("M\r\n");
            e.append("ACTION:DISPLAY\r\n");
            e.append("DESCRIPTION:Rappel: ").append(escapeICal(event.getTitre())).append("\r\n");
            e.append("END:VALARM\r\n");
        }
        e.append("STATUS:").append(mapStatut(event.getStatut())).append("\r\n");
        e.append("END:VEVENT\r\n");
        return e.toString();
    }

    private String formatICalDate(LocalDateTime dt) {
        return dt.atZone(java.time.ZoneId.of("UTC")).format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    }

    private String escapeICal(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace(";", "\\;")
                   .replace(",", "\\,")
                   .replace("\n", "\\n");
    }

    private String mapStatut(CalendarEvent.Statut statut) {
        return switch (statut) {
            case CONFIRMÉ -> "CONFIRMED";
            case EN_ATTENTE -> "TENTATIVE";
            case ANNULÉ -> "CANCELLED";
        };
    }
}
