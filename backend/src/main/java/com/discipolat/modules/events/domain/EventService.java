package com.discipolat.modules.events.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final WeeklyProgramTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;
    private final WorkspaceScopeService workspaceScope;

    public EventService(EventRepository eventRepository,
                        EventRegistrationRepository registrationRepository,
                        WeeklyProgramTemplateRepository templateRepository,
                        UserRepository userRepository,
                        NotificationService notificationService,
                        SecurityUtils securityUtils,
                        WorkspaceScopeService workspaceScope) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
        this.workspaceScope = workspaceScope;
    }

    public Event create(Event event) {
        // Espace métier : on ne crée un événement de famille que pour une famille visible.
        if (event.getFamilleId() != null && !workspaceScope.isSuperUser()
                && !workspaceScope.canAccessFamily(event.getFamilleId())) {
            throw new AccessDeniedException(
                    "Cet événement ne concerne pas votre espace métier");
        }
        UUID currentUserId = securityUtils.getCurrentUserId();
        event.setOrganisateurId(currentUserId);
        event.setStatut("PLANIFIE");
        event.setNbInscrits(0);
        Event saved = eventRepository.save(event);

        // Notify all PASTEUR users when a non-pasteur creates an event
        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (currentUser != null && !currentUser.getRoles().contains(UserRole.PASTEUR) && !currentUser.getRoles().contains(UserRole.ADMIN)) {
            String userName = currentUser.getFirstName() + " " + currentUser.getLastName();
            userRepository.findByRole(UserRole.PASTEUR).forEach(pasteur -> {
                notificationService.create(
                        pasteur.getId(),
                        TypeNotification.INFORMATION,
                        CanalNotification.IN_APP,
                        "Nouvel événement créé",
                        userName + " a créé l'événement \"" + saved.getTitre() + "\" le " +
                                saved.getDateDebut().toLocalDate().toString() + ".",
                        saved.getId(),
                        "EVENT"
                );
            });
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public Event findById(UUID id) {
        Event event = eventRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Event", id));
        // Espace métier : un rôle opérationnel ne lit que les événements de ses familles
        // (les événements d'église sans famille restent visibles par tous).
        if (!canAccessEvent(event)) {
            throw new AccessDeniedException(
                    "Accès refusé à cet événement dans l'espace métier courant");
        }
        return event;
    }

    @Transactional(readOnly = true)
    public Page<Event> findAll(Pageable pageable) {
        if (workspaceScope.isSuperUser()) {
            return eventRepository.findByStatutAndDeletedFalse("PLANIFIE", pageable);
        }
        return scopeEvents(eventRepository.findByStatutAndDeletedFalse("PLANIFIE", pageable).getContent(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Event> findByFamilleId(UUID familleId, Pageable pageable) {
        if (!workspaceScope.isSuperUser() && !workspaceScope.canAccessFamily(familleId)) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        return eventRepository.findByFamilleIdAndDeletedFalse(familleId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Event> findByStatut(String statut, Pageable pageable) {
        if (workspaceScope.isSuperUser()) {
            return eventRepository.findByStatutAndDeletedFalse(statut, pageable);
        }
        return scopeEvents(eventRepository.findByStatutAndDeletedFalse(statut, pageable).getContent(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Event> findByTypeEvenement(String typeEvenement, Pageable pageable) {
        if (workspaceScope.isSuperUser()) {
            return eventRepository.findByTypeEvenementAndDeletedFalse(typeEvenement, pageable);
        }
        return scopeEvents(eventRepository.findByTypeEvenementAndDeletedFalse(typeEvenement, pageable).getContent(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Event> findUpcoming(Pageable pageable) {
        if (workspaceScope.isSuperUser()) {
            return eventRepository.findByDateDebutBetweenAndDeletedFalse(
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30), pageable);
        }
        return scopeEvents(eventRepository.findByDateDebutBetweenAndDeletedFalse(
                LocalDateTime.now(), LocalDateTime.now().plusDays(30), pageable).getContent(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Event> findUpcomingByFamille(UUID familleId) {
        if (!workspaceScope.isSuperUser() && !workspaceScope.canAccessFamily(familleId)) {
            return List.of();
        }
        return eventRepository.findByFamilleIdAndStatutAndDeletedFalse(familleId, "PLANIFIE");
    }

    public Event update(UUID id, Event updated) {
        Event event = findById(id);
        if (!canManageEvent(event)) {
            throw new AccessDeniedException("Vous ne pouvez pas modifier cet événement");
        }
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
        if (!canManageEvent(event)) {
            throw new AccessDeniedException("Vous ne pouvez pas supprimer cet événement");
        }
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
        findById(eventId); // contrôle d'accès à l'événement
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
        findById(eventId); // contrôle d'accès à l'événement
        return registrationRepository.findByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public long countByFamilleId(UUID familleId) {
        if (!workspaceScope.isSuperUser() && !workspaceScope.canAccessFamily(familleId)) {
            return 0;
        }
        return eventRepository.countByFamilleIdAndDeletedFalse(familleId);
    }

    // ======================== CONSOLIDATED VIEW (US-06) ========================

    /**
     * Get all upcoming events consolidated for the Pasteur — across all departments and families.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getConsolidatedUpcoming(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(days);
        List<Event> events = eventRepository.findByDateDebutBetweenAndDeletedFalse(now, end);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Event e : events) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", e.getId());
            entry.put("titre", e.getTitre());
            entry.put("typeEvenement", e.getTypeEvenement());
            entry.put("dateDebut", e.getDateDebut());
            entry.put("dateFin", e.getDateFin());
            entry.put("lieu", e.getLieu());
            entry.put("statut", e.getStatut());
            entry.put("nbInscrits", e.getNbInscrits());
            entry.put("limitePlaces", e.getLimitePlaces());
            entry.put("familleId", e.getFamilleId());

            // Organiser info
            userRepository.findById(e.getOrganisateurId()).ifPresent(org -> {
                entry.put("organisateurNom", org.getFirstName() + " " + org.getLastName());
                entry.put("organisateurRole", org.getActiveRole() != null ? org.getActiveRole().name() : org.getRole().name());
            });

            result.add(entry);
        }
        result.sort(Comparator.comparing(m -> (LocalDateTime) m.get("dateDebut")));
        return result;
    }

    /**
     * Get events grouped by family for consolidated view.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getConsolidatedByFamily(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(days);
        List<Event> events = eventRepository.findByDateDebutBetweenAndDeletedFalse(now, end);

        Map<UUID, List<Map<String, Object>>> byFamille = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> byType = new LinkedHashMap<>();

        for (Event e : events) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", e.getId());
            entry.put("titre", e.getTitre());
            entry.put("typeEvenement", e.getTypeEvenement());
            entry.put("dateDebut", e.getDateDebut());
            entry.put("dateFin", e.getDateFin());
            entry.put("lieu", e.getLieu());
            entry.put("statut", e.getStatut());
            entry.put("nbInscrits", e.getNbInscrits());
            entry.put("familleId", e.getFamilleId());
            entry.put("organisateurId", e.getOrganisateurId());

            userRepository.findById(e.getOrganisateurId()).ifPresent(org -> {
                entry.put("organisateurNom", org.getFirstName() + " " + org.getLastName());
            });

            // Group by type
            byType.computeIfAbsent(e.getTypeEvenement(), k -> new ArrayList<>()).add(entry);

            // Group by family
            if (e.getFamilleId() != null) {
                byFamille.computeIfAbsent(e.getFamilleId(), k -> new ArrayList<>()).add(entry);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", (long) events.size());
        result.put("events", events.stream().map(e -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", e.getId());
            entry.put("titre", e.getTitre());
            entry.put("typeEvenement", e.getTypeEvenement());
            entry.put("dateDebut", e.getDateDebut());
            return entry;
        }).toList());
        result.put("parType", byType.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey, e -> (long) e.getValue().size())));
        result.put("parFamille", byFamille.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey, e -> (long) e.getValue().size())));

        return result;
    }

    // ======================== WEEKLY PROGRAM TEMPLATES (US-50) ========================

    /**
     * Create a weekly program template (Pasteur only)
     */
    public WeeklyProgramTemplate createTemplate(WeeklyProgramTemplate template) {
        template.setCreatedBy(securityUtils.getCurrentUserId());
        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<WeeklyProgramTemplate> getActiveTemplates() {
        return templateRepository.findByActifTrueOrderByJourSemaineAscHeureDebutAsc();
    }

    @Transactional(readOnly = true)
    public List<WeeklyProgramTemplate> getMyTemplates() {
        return templateRepository.findByCreatedByOrderByJourSemaineAsc(securityUtils.getCurrentUserId());
    }

    public WeeklyProgramTemplate updateTemplate(UUID id, WeeklyProgramTemplate updated) {
        WeeklyProgramTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WeeklyProgramTemplate", id));
        if (updated.getTitre() != null) template.setTitre(updated.getTitre());
        if (updated.getDescription() != null) template.setDescription(updated.getDescription());
        if (updated.getTypeEvenement() != null) template.setTypeEvenement(updated.getTypeEvenement());
        if (updated.getJourSemaine() != null) template.setJourSemaine(updated.getJourSemaine());
        if (updated.getHeureDebut() != null) template.setHeureDebut(updated.getHeureDebut());
        if (updated.getHeureFin() != null) template.setHeureFin(updated.getHeureFin());
        if (updated.getLieu() != null) template.setLieu(updated.getLieu());
        if (updated.getDureeMinutes() != null) template.setDureeMinutes(updated.getDureeMinutes());
        if (updated.getCouleur() != null) template.setCouleur(updated.getCouleur());
        template.setActif(updated.isActif());
        return templateRepository.save(template);
    }

    public void deleteTemplate(UUID id) {
        templateRepository.deleteById(id);
    }

    public void toggleTemplateActif(UUID id, boolean actif) {
        WeeklyProgramTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WeeklyProgramTemplate", id));
        template.setActif(actif);
        templateRepository.save(template);
    }

    /**
     * Generate events for a specific week from the active templates.
     * Creates one Event per template for the given week's dates.
     * Skips dates in the past.
     */
    public List<Event> generateWeekProgram(LocalDate weekStart) {
        if (weekStart == null) {
            weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }

        UUID currentUserId = securityUtils.getCurrentUserId();
        List<WeeklyProgramTemplate> activeTemplates = templateRepository.findByActifTrueOrderByJourSemaineAscHeureDebutAsc();
        List<Event> createdEvents = new ArrayList<>();
        LocalDate weekEnd = weekStart.plusDays(6);

        for (WeeklyProgramTemplate template : activeTemplates) {
            LocalDate eventDate = getDateForDayOfWeek(weekStart, template.getJourSemaine());
            if (eventDate == null || eventDate.isBefore(LocalDate.now())) continue;

            LocalDateTime dateDebut = LocalDateTime.of(eventDate, template.getHeureDebut());
            LocalDateTime dateFin = template.getHeureFin() != null
                    ? LocalDateTime.of(eventDate, template.getHeureFin())
                    : (template.getDureeMinutes() != null
                        ? dateDebut.plusMinutes(template.getDureeMinutes())
                        : dateDebut.plusHours(2));

            // Check if event already exists for this date + time to avoid duplicates
            boolean exists = eventRepository.findByDateDebutBetweenAndDeletedFalse(
                    dateDebut.minusMinutes(30), dateDebut.plusMinutes(30))
                    .stream()
                    .anyMatch(e -> e.getTitre().equals(template.getTitre())
                            && e.getTypeEvenement().equals(template.getTypeEvenement()));
            if (exists) continue;

            Event event = Event.builder()
                    .organisateurId(currentUserId)
                    .typeEvenement(template.getTypeEvenement())
                    .titre(template.getTitre())
                    .description(template.getDescription())
                    .lieu(template.getLieu())
                    .dateDebut(dateDebut)
                    .dateFin(dateFin)
                    .statut("PLANIFIE")
                    .nbInscrits(0)
                    .build();
            createdEvents.add(eventRepository.save(event));
        }

        return createdEvents;
    }

    /**
     * Generate events for a full month (4 weeks) from the active templates.
     */
    public List<Event> generateMonthProgram() {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Event> allEvents = new ArrayList<>();
        for (int w = 0; w < 4; w++) {
            allEvents.addAll(generateWeekProgram(thisMonday.plusWeeks(w)));
        }
        return allEvents;
    }

    /**
     * Get the program for a specific week (all planned events in that week).
     */
    @Transactional(readOnly = true)
    public List<Event> getWeekProgram(LocalDate weekStart) {
        if (weekStart == null) {
            weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();
        return eventRepository.findByDateDebutBetweenAndDeletedFalse(start, end).stream()
                .filter(this::canAccessEvent)
                .sorted(Comparator.comparing(Event::getDateDebut))
                .toList();
    }

    // ========================================================================
    // Isolation des espaces métiers (rôle actif)
    // ========================================================================

    /**
     * Un événement est visible si l'utilisateur est super-utilisateur (Admin/Pasteur
     * actifs), si c'est un événement d'église (sans famille), ou si sa famille
     * appartient à l'espace métier courant.
     */
    private boolean canAccessEvent(Event event) {
        if (workspaceScope.isSuperUser()) return true;
        if (event.getFamilleId() == null) return true;
        return workspaceScope.canAccessFamily(event.getFamilleId());
    }

    /**
     * Modification / suppression : super-utilisateur, organisateur de l'événement,
     * ou responsable/chef dont la famille (ou le département) gère l'événement.
     */
    private boolean canManageEvent(Event event) {
        if (workspaceScope.isSuperUser()) return true;
        UUID userId = securityUtils.getCurrentUserId();
        if (event.getOrganisateurId() != null && event.getOrganisateurId().equals(userId)) return true;
        if (event.getFamilleId() != null && workspaceScope.canAccessFamily(event.getFamilleId())) {
            return securityUtils.hasActiveRole("RESPONSABLE", "CHEF_DE_FAMILLE", "PASTEUR", "ADMIN");
        }
        return false;
    }

    /**
     * Pagination en mémoire des événements déjà filtrés par l'espace métier.
     * Les familles accessibles sont précalculées UNE fois (évite N requêtes
     * canAccessFamily en boucle sur chaque événement).
     */
    private Page<Event> scopeEvents(List<Event> candidates, Pageable pageable) {
        Set<UUID> accessibleFamilies = workspaceScope.accessibleFamilyIds();
        List<Event> scoped = candidates.stream()
                .filter(e -> e.getFamilleId() == null || accessibleFamilies.contains(e.getFamilleId()))
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), scoped.size());
        List<Event> content = start < scoped.size() ? scoped.subList(start, end) : List.of();
        return new PageImpl<>(content, pageable, scoped.size());
    }

    private LocalDate getDateForDayOfWeek(LocalDate weekStart, String jourSemaine) {
        DayOfWeek targetDay = switch (jourSemaine.toUpperCase()) {
            case "LUNDI" -> DayOfWeek.MONDAY;
            case "MARDI" -> DayOfWeek.TUESDAY;
            case "MERCREDI" -> DayOfWeek.WEDNESDAY;
            case "JEUDI" -> DayOfWeek.THURSDAY;
            case "VENDREDI" -> DayOfWeek.FRIDAY;
            case "SAMEDI" -> DayOfWeek.SATURDAY;
            case "DIMANCHE" -> DayOfWeek.SUNDAY;
            default -> null;
        };
        if (targetDay == null) return null;
        return weekStart.with(TemporalAdjusters.nextOrSame(targetDay));
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