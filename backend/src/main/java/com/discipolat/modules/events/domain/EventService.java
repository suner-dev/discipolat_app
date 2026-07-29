package com.discipolat.modules.events.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final SecurityUtils securityUtils;

    public EventService(EventRepository eventRepository,
                        EventRegistrationRepository registrationRepository,
                        SecurityUtils securityUtils) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.securityUtils = securityUtils;
    }

    public Event create(Event event) {
        event.setOrganisateurId(securityUtils.getCurrentUserId());
        event.setStatut("PLANIFIE");
        event.setNbInscrits(0);
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Event findById(UUID id) {
        return eventRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Event", id));
    }

    @Transactional(readOnly = true)
    public Page<Event> findAll(Pageable pageable) {
        return eventRepository.findByStatutAndDeletedFalse("PLANIFIE", pageable);
    }

    @Transactional(readOnly = true)
    public Page<Event> findByFamilleId(UUID familleId, Pageable pageable) {
        return eventRepository.findByFamilleIdAndDeletedFalse(familleId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Event> findByStatut(String statut, Pageable pageable) {
        return eventRepository.findByStatutAndDeletedFalse(statut, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Event> findByTypeEvenement(String typeEvenement, Pageable pageable) {
        return eventRepository.findByTypeEvenementAndDeletedFalse(typeEvenement, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Event> findUpcoming(Pageable pageable) {
        return eventRepository.findByDateDebutBetweenAndDeletedFalse(
                LocalDateTime.now(), LocalDateTime.now().plusDays(30), pageable);
    }

    @Transactional(readOnly = true)
    public List<Event> findUpcomingByFamille(UUID familleId) {
        return eventRepository.findByFamilleIdAndStatutAndDeletedFalse(familleId, "PLANIFIE");
    }

    public Event update(UUID id, Event updated) {
        Event event = findById(id);
        if (updated.getTitre() != null) event.setTitre(updated.getTitre());
        if (updated.getDescription() != null) event.setDescription(updated.getDescription());
        if (updated.getLieu() != null) event.setLieu(updated.getLieu());
        if (updated.getDateDebut() != null) event.setDateDebut(updated.getDateDebut());
        if (updated.getDateFin() != null) event.setDateFin(updated.getDateFin());
        if (updated.getLimitePlaces() != null) event.setLimitePlaces(updated.getLimitePlaces());
        if (updated.getTypeEvenement() != null) event.setTypeEvenement(updated.getTypeEvenement());
        if (updated.getStatut() != null) event.setStatut(updated.getStatut());
        if (updated.getCompteRendu() != null) event.setCompteRendu(updated.getCompteRendu());
        return eventRepository.save(event);
    }

    public void delete(UUID id) {
        Event event = findById(id);
        event.setDeleted(true);
        eventRepository.save(event);
    }

    public EventRegistration register(UUID eventId) {
        Event event = findById(eventId);
        UUID userId = securityUtils.getCurrentUserId();
        if (registrationRepository.findByEventIdAndUtilisateurId(eventId, userId).isPresent()) {
            throw new IllegalArgumentException("Already registered for this event");
        }
        if (event.getLimitePlaces() != null && event.getNbInscrits() >= event.getLimitePlaces()) {
            EventRegistration registration = EventRegistration.builder()
                    .eventId(eventId)
                    .utilisateurId(userId)
                    .statutInscription("EN_ATTENTE")
                    .build();
            return registrationRepository.save(registration);
        }
        EventRegistration registration = EventRegistration.builder()
                .eventId(eventId)
                .utilisateurId(userId)
                .statutInscription("INSCRIT")
                .build();
        event.setNbInscrits(event.getNbInscrits() + 1);
        eventRepository.save(event);
        return registrationRepository.save(registration);
    }

    public void unregister(UUID eventId) {
        UUID userId = securityUtils.getCurrentUserId();
        EventRegistration registration = registrationRepository.findByEventIdAndUtilisateurId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Registration", eventId));
        Event event = findById(eventId);
        if ("PRESENT".equals(registration.getStatutInscription())) {
            event.setNbInscrits(Math.max(0, event.getNbInscrits() - 1));
        }
        registrationRepository.delete(registration);
        eventRepository.save(event);
    }

    public EventRegistration markAttendance(UUID eventId, UUID userId, boolean present) {
        EventRegistration registration = registrationRepository.findByEventIdAndUtilisateurId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Registration", eventId));
        registration.setStatutInscription(present ? "PRESENT" : "ABSENT");
        if (present) {
            registration.setDateEmargement(LocalDateTime.now());
        }
        return registrationRepository.save(registration);
    }

    @Transactional(readOnly = true)
    public List<EventRegistration> getRegistrations(UUID eventId) {
        return registrationRepository.findByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public long countByFamilleId(UUID familleId) {
        return eventRepository.countByFamilleIdAndDeletedFalse(familleId);
    }

    // ======================== US-55: EVENT STATISTICS ========================

    @Transactional(readOnly = true)
    public Map<String, Object> getEventStatistics(UUID familleId, String periodeDebut, String periodeFin) {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<Event> events;

        if (familleId != null) {
            events = eventRepository.findByFamilleIdAndStatutAndDeletedFalse(familleId, "TERMINE");
        } else {
            events = eventRepository.findAll().stream()
                    .filter(e -> !e.isDeleted())
                    .toList();
        }

        int totalEvenements = events.size();
        int totalInscrits = 0;
        int totalPresents = 0;
        int totalAbsents = 0;

        for (Event event : events) {
            List<EventRegistration> registrations = registrationRepository.findByEventId(event.getId());
            for (EventRegistration reg : registrations) {
                totalInscrits++;
                if ("PRESENT".equals(reg.getStatutInscription())) totalPresents++;
                else if ("ABSENT".equals(reg.getStatutInscription())) totalAbsents++;
            }
        }

        stats.put("totalEvenements", totalEvenements);
        stats.put("totalInscrits", totalInscrits);
        stats.put("totalPresents", totalPresents);
        stats.put("totalAbsents", totalAbsents);
        stats.put("tauxParticipation", totalInscrits > 0
                ? Math.round((double) totalPresents / totalInscrits * 1000.0) / 10.0 : 0.0);

        // Stats by event type
        Map<String, Long> parType = events.stream()
                .collect(Collectors.groupingBy(Event::getTypeEvenement, Collectors.counting()));
        stats.put("parType", parType);

        return stats;
    }
}