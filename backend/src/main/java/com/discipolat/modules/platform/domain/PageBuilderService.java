package com.discipolat.modules.platform.domain;

import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.StatutEntite;
import com.discipolat.common.enums.TransferStatus;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.alerts.domain.Alert;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.departments.domain.DepartmentTask;
import com.discipolat.modules.departments.domain.DepartmentTaskRepository;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.files.domain.FileEntity;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.platform.api.PageDataSource;
import com.discipolat.modules.platform.api.ResolvedBlock;
import com.discipolat.modules.platform.api.ResolvedPage;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.transfers.domain.TransferRequest;
import com.discipolat.modules.transfers.domain.TransferRequestRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Page Builder : pages personnalisées configurées par l'administrateur.
 *
 * - CRUD complet (création, modification, suppression) ;
 * - publication/dépubliage (la version augmente à chaque publication) ;
 * - versionnage systématique dans config_revisions (avant/après, auteur) ;
 * - résolution des blocs sur des données RÉELLES au moment du rendu, scopées
 *   selon l'espace métier de l'utilisateur (aucune statistique fictive) ;
 * - contrôle d'accès par rôles au rendu public.
 */
@Service
@Transactional
public class PageBuilderService {

    private static final Set<String> BLOCK_TYPES = Set.of(
            "KPI", "TABLEAU", "LISTE", "TEXTE", "LIENS", "RECHERCHE", "IMAGES",
            "GRAPHIQUE", "CALENDRIER", "TIMELINE", "CHECKLIST",
            "FICHIERS", "TACHES", "FORMULAIRE");
    private static final Set<String> LAYOUTS = Set.of("STACK", "GRID_2", "GRID_3");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_MONTH_LABEL_FMT = DateTimeFormatter.ofPattern("MMM", Locale.FRENCH);

    private final CustomPageRepository pageRepository;
    private final ConfigRevisionService revisionService;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final WorkspaceScopeService scopeService;
    private final SoulRepository soulRepository;
    private final FamilyRepository familyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;    private final AlertRepository alertRepository;
    private final TransferRequestRepository transferRepository;
    private final FileEntityRepository fileRepository;
    private final DepartmentTaskRepository taskRepository;


    public PageBuilderService(CustomPageRepository pageRepository,
                              ConfigRevisionService revisionService,
                              AuditService auditService,
                              SecurityUtils securityUtils,
                              WorkspaceScopeService scopeService,
                              SoulRepository soulRepository,
                              FamilyRepository familyRepository,
                              DepartmentRepository departmentRepository,
                              UserRepository userRepository,
                              EventRepository eventRepository,
                              AlertRepository alertRepository,
                              TransferRequestRepository transferRepository,
                              FileEntityRepository fileRepository,
                              DepartmentTaskRepository taskRepository) {
        this.pageRepository = pageRepository;
        this.revisionService = revisionService;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
        this.scopeService = scopeService;
        this.soulRepository = soulRepository;
        this.familyRepository = familyRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
        this.transferRepository = transferRepository;
        this.fileRepository = fileRepository;
        this.taskRepository = taskRepository;
    }

    /* ============================== CRUD ============================== */

    @Transactional(readOnly = true)
    public List<CustomPage> listAll() {
        return pageRepository.findAllByOrderByTitleAsc();
    }

    @Transactional(readOnly = true)
    public List<CustomPage> listPublished() {
        return pageRepository.findByEnabledTrueAndPublishedTrueOrderByTitleAsc();
    }

    public CustomPage get(UUID id) {
        return pageRepository.findById(id)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("CustomPage", id));
    }

    public CustomPage create(CustomPage request) {
        validate(request);
        if (pageRepository.existsByKey(request.getKey())) {
            throw new IllegalArgumentException("Une page avec la clé « " + request.getKey() + " » existe déjà.");
        }
        if (pageRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Une page avec l'adresse « /pages/" + request.getSlug() + " » existe déjà.");
        }
        request.setKey(request.getKey().trim().toUpperCase());
        request.setSlug(slugify(request.getSlug()));
        request.setCreatedBy(securityUtils.getCurrentUserId());
        request.setVersion(1);
        CustomPage saved = pageRepository.save(request);
        auditService.logSimple("PAGE_CREATED", "CUSTOM_PAGE", saved.getId());
        revisionService.record("CUSTOM_PAGE", saved.getKey(), "PAGE_CREATED", pagePayload(saved));
        return saved;
    }

    public CustomPage update(UUID id, CustomPage request) {
        CustomPage page = get(id);
        Map<String, Object> before = pagePayload(page);
        if (request.getTitle() != null && !request.getTitle().isBlank()) page.setTitle(request.getTitle());
        if (request.getDescription() != null) page.setDescription(request.getDescription());
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String slug = slugify(request.getSlug());
            if (!slug.equals(page.getSlug()) && pageRepository.existsBySlug(slug)) {
                throw new IllegalArgumentException("Une page avec l'adresse « /pages/" + slug + " » existe déjà.");
            }
            page.setSlug(slug);
        }
        if (request.getLayout() != null && !request.getLayout().isBlank()) page.setLayout(request.getLayout());
        if (request.getBlocks() != null) {
            validateBlocks(request.getBlocks());
            page.setBlocks(new ArrayList<>(request.getBlocks()));
        }
        if (request.getRoles() != null) page.setRoles(new ArrayList<>(request.getRoles()));
        if (request.isEnabled() != page.isEnabled()) page.setEnabled(request.isEnabled());
        pageRepository.save(page);
        auditService.logSimple("PAGE_UPDATED", "CUSTOM_PAGE", page.getId());
        revisionService.record("CUSTOM_PAGE", page.getKey(), "PAGE_UPDATED",
                Map.of("before", before, "after", pagePayload(page)));
        return page;
    }

    public void delete(UUID id) {
        CustomPage page = get(id);
        pageRepository.delete(page);
        auditService.logSimple("PAGE_DELETED", "CUSTOM_PAGE", id);
        revisionService.record("CUSTOM_PAGE", page.getKey(), "PAGE_DELETED", pagePayload(page));
    }

    /** Publie / dépublie une page. Chaque publication incrémente la version. */
    public CustomPage setPublished(UUID id, boolean published) {
        CustomPage page = get(id);
        if (page.isPublished() != published) {
            page.setPublished(published);
            if (published) {
                page.setVersion(page.getVersion() + 1);
            }
            pageRepository.save(page);
            auditService.logSimple(published ? "PAGE_PUBLISHED" : "PAGE_UNPUBLISHED", "CUSTOM_PAGE", id);
            revisionService.record("CUSTOM_PAGE", page.getKey(),
                    published ? "PAGE_PUBLISHED" : "PAGE_UNPUBLISHED",
                    Map.of("published", published, "version", page.getVersion()));
        }
        return page;
    }

    /* ============================== Rendu ============================== */

    /**
     * Rendu public d'une page par son adresse (slug). La page doit être
     * active, publiée et autorisée pour l'un des rôles de l'utilisateur.
     */
    @Transactional(readOnly = true)
    public ResolvedPage resolve(String slug) {
        CustomPage page = pageRepository.findBySlug(slug)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("CustomPage", "slug", slug));
        if (!page.isEnabled() || !page.isPublished()) {
            throw new com.discipolat.common.domain.EntityNotFoundException("CustomPage", "slug", slug);
        }
        List<String> userRoles = securityUtils.getAllUserRoles();
        boolean allowed = page.getRoles() == null || page.getRoles().isEmpty()
                || userRoles.stream().anyMatch(r -> page.isVisibleForRole(r))
                || (securityUtils.isSuperUser() && page.getRoles().contains("PASTEUR"));
        if (!allowed) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Cette page n'est pas accessible avec votre rôle actuel.");
        }
        return new ResolvedPage(page, resolveBlocks(page.getBlocks()));
    }

    /** Aperçu administrateur : résout la page même si elle n'est pas publiée. */
    @Transactional(readOnly = true)
    public ResolvedPage resolvePreview(UUID id) {
        CustomPage page = get(id);
        return new ResolvedPage(page, resolveBlocks(page.getBlocks()));
    }

    /* ============================ Sources ============================= */

    @Transactional(readOnly = true)
    public List<PageDataSource> sources() {
        return List.of(
                new PageDataSource("SOULS_TOTAL", "Âmes suivies (total)", "KPI",
                        "Nombre d'âmes non supprimées, dans votre périmètre.", false),
                new PageDataSource("SOULS_ACTIFS", "Âmes actives", "KPI",
                        "Âmes au statut ACTIF, dans votre périmètre.", false),
                new PageDataSource("FAMILIES_TOTAL", "Familles", "KPI",
                        "Nombre de familles, dans votre périmètre.", false),
                new PageDataSource("DEPARTMENTS_TOTAL", "Départements", "KPI",
                        "Nombre de départements, dans votre périmètre.", false),
                new PageDataSource("EVENTS_UPCOMING", "Événements à venir", "KPI",
                        "Événements planifiés à partir d'aujourd'hui.", false),
                new PageDataSource("ALERTS_OPEN", "Alertes ouvertes", "KPI",
                        "Alertes actives, dans votre périmètre.", false),
                new PageDataSource("TRANSFERS_PENDING", "Transferts en attente", "KPI",
                        "Demandes en attente de validation.", false),
                new PageDataSource("USERS_TOTAL", "Utilisateurs (super-utilisateurs)", "KPI",
                        "Nombre de comptes. Réservé aux super-utilisateurs.", true),
                new PageDataSource("RECENT_SOULS", "Dernières âmes", "TABLEAU",
                        "Les 10 âmes les plus récentes (nom, statut, famille).", false),
                new PageDataSource("UPCOMING_EVENTS", "Événements à venir", "TABLEAU",
                        "Les 10 prochains événements (titre, date, lieu).", false),
                new PageDataSource("RECENT_ALERTS", "Alertes récentes", "LISTE",
                        "Les 10 alertes actives les plus récentes.", false),
                new PageDataSource("RECENT_TRANSFERS", "Transferts récents", "TABLEAU",
                        "Les 10 dernières demandes de transfert.", false),
                new PageDataSource("DEPARTMENTS_LIST", "Liste des départements", "TABLEAU",
                        "Départements et leur statut, dans votre périmètre.", false),
                new PageDataSource("SOULS_BY_STATUT", "Âmes par statut", "GRAPHIQUE",
                        "Répartition des âmes par statut (camembert), dans votre périmètre.", false),
                new PageDataSource("EVENTS_BY_MONTH", "Événements par mois", "GRAPHIQUE",
                        "Nombre d'événements par mois sur les 6 prochains mois (barres).", false),
                new PageDataSource("ALERTS_BY_TYPE", "Alertes par type", "GRAPHIQUE",
                        "Alertes actives regroupées par type, dans votre périmètre.", false),
                new PageDataSource("DEPARTMENTS_BY_STATUT", "Départements par statut", "GRAPHIQUE",
                        "Répartition des départements par statut (camembert).", false),
                new PageDataSource("CALENDAR_EVENTS", "Prochains événements (calendrier)", "CALENDRIER",
                        "Les événements des 60 prochains jours (titre, date, lieu).", false),
                new PageDataSource("SOULS_TIMELINE", "Dernières âmes (timeline)", "TIMELINE",
                        "Les 10 âmes créées le plus récemment, avec leur date d'ajout.", false),
                new PageDataSource("RECENT_FILES", "Documents récents", "FICHIERS",
                        "Les 10 derniers documents versés, dans votre périmètre.", false),
                new PageDataSource("TACHES_EN_COURS", "Tâches ouvertes", "TACHES",
                        "Les 10 prochaines tâches ouvertes (par échéance), dans votre périmètre.", false));
    }

    /* ======================= Résolution des blocs ===================== */

    private List<ResolvedBlock> resolveBlocks(List<Map<String, Object>> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }
        boolean superUser = scopeService.isSuperUser();
        UUID userId = securityUtils.getCurrentUserId();
        Set<UUID> soulIds = superUser ? Set.of() : scopeService.accessibleSoulIds();
        Set<UUID> familyIds = superUser ? Set.of() : scopeService.accessibleFamilyIds();
        Set<UUID> deptIds = superUser ? Set.of() : scopeService.accessibleDepartmentIds();
        return blocks.stream()
                .map(b -> resolveBlock(b, superUser, userId, soulIds, familyIds, deptIds))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private ResolvedBlock resolveBlock(Map<String, Object> block, boolean superUser, UUID userId,
                                       Set<UUID> soulIds, Set<UUID> familyIds, Set<UUID> deptIds) {
        String type = String.valueOf(block.getOrDefault("type", "")).toUpperCase();
        Map<String, Object> config = block.get("config") instanceof Map<?, ?> m
                ? new LinkedHashMap<>((Map<String, Object>) m)
                : new LinkedHashMap<>();
        String source = config.get("source") instanceof String s ? s.toUpperCase() : null;

        Map<String, Object> data = switch (type) {
            case "KPI" -> resolveKpi(source, superUser, userId, soulIds, familyIds, deptIds);
            case "TABLEAU" -> resolveTable(source, superUser, userId, soulIds, familyIds, deptIds);
            case "LISTE" -> resolveList(source, superUser, userId, soulIds, familyIds, deptIds);
            case "GRAPHIQUE" -> resolveChart(source, superUser, soulIds, deptIds);
            case "CALENDRIER" -> resolveCalendar(source, superUser, soulIds);
            case "TIMELINE" -> resolveTimeline(source, superUser, soulIds);
            case "FICHIERS" -> resolveFiles(source, superUser, familyIds);
            case "TACHES" -> resolveTasks(source, superUser, deptIds);
            default -> null; // TEXTE / LIENS / RECHERCHE / IMAGES / CHECKLIST / FORMULAIRE : pas de source
        };
        return new ResolvedBlock(type, config, data);
    }

    private Map<String, Object> resolveKpi(String source, boolean superUser, UUID userId,
                                           Set<UUID> soulIds, Set<UUID> familyIds, Set<UUID> deptIds) {
        if (source == null) return null;
        return switch (source) {
            case "SOULS_TOTAL" -> value(superUser ? soulRepository.countByDeletedFalse() : soulIds.size());
            case "SOULS_ACTIFS" -> value(countSoulsByStatut(superUser ? null : soulIds, StatutAme.ACTIF));
            case "FAMILIES_TOTAL" -> value(superUser ? familyRepository.countByDeletedFalse() : familyIds.size());
            case "DEPARTMENTS_TOTAL" -> value(superUser ? departmentRepository.countByDeletedFalse() : deptIds.size());
            case "EVENTS_UPCOMING" -> value(eventRepository.countByDeletedFalseAndDateDebutAfter(LocalDateTime.now()));
            case "ALERTS_OPEN" -> value(superUser
                    ? alertRepository.countByStatut(StatutAlerte.ACTIVE)
                    : (soulIds.isEmpty() ? 0 : alertRepository.countByStatutAndAmeIdIn(StatutAlerte.ACTIVE, soulIds)));
            case "TRANSFERS_PENDING" -> value(superUser
                    ? transferRepository.countByStatut(TransferStatus.EN_ATTENTE_VALIDATION)
                    + transferRepository.countByStatut(TransferStatus.VALIDATION_PARTIELLE)
                    : transferRepository.countByDemandeurIdAndStatut(userId, TransferStatus.EN_ATTENTE_VALIDATION));
            case "USERS_TOTAL" -> superUser ? value(userRepository.count()) : null;
            default -> null;
        };
    }

    private Map<String, Object> resolveTable(String source, boolean superUser, UUID userId,
                                             Set<UUID> soulIds, Set<UUID> familyIds, Set<UUID> deptIds) {
        if (source == null) return null;
        return switch (source) {
            case "RECENT_SOULS" -> recentSoulsTable(superUser ? null : soulIds);
            case "UPCOMING_EVENTS" -> upcomingEventsTable();
            case "RECENT_TRANSFERS" -> recentTransfersTable(superUser ? null : userId);
            case "DEPARTMENTS_LIST" -> departmentsTable(superUser ? null : deptIds);
            default -> null;
        };
    }

    private Map<String, Object> resolveList(String source, boolean superUser, UUID userId,
                                            Set<UUID> soulIds, Set<UUID> familyIds, Set<UUID> deptIds) {
        if (source == null) return null;
        return switch (source) {
            case "RECENT_ALERTS" -> recentAlertsList(superUser ? null : soulIds);
            default -> null;
        };
    }

    private Map<String, Object> resolveChart(String source, boolean superUser,
                                             Set<UUID> soulIds, Set<UUID> deptIds) {
        if (source == null) return null;
        return switch (source) {
            case "SOULS_BY_STATUT" -> soulsByStatutChart(superUser ? null : soulIds);
            case "EVENTS_BY_MONTH" -> eventsByMonthChart();
            case "ALERTS_BY_TYPE" -> alertsByTypeChart(superUser ? null : soulIds);
            case "DEPARTMENTS_BY_STATUT" -> departmentsByStatutChart(superUser ? null : deptIds);
            default -> null;
        };
    }

    private Map<String, Object> resolveCalendar(String source, boolean superUser, Set<UUID> soulIds) {
        if (source == null) return null;
        return switch (source) {
            case "CALENDAR_EVENTS" -> calendarEvents();
            default -> null;
        };
    }

    private Map<String, Object> resolveTimeline(String source, boolean superUser, Set<UUID> soulIds) {
        if (source == null) return null;
        return switch (source) {
            case "SOULS_TIMELINE" -> soulsTimeline(superUser ? null : soulIds);
            default -> null;
        };
    }

    private Map<String, Object> resolveFiles(String source, boolean superUser, Set<UUID> familyIds) {
        if (source == null) return null;
        return switch (source) {
            case "RECENT_FILES" -> recentFiles(superUser ? null : familyIds);
            default -> null;
        };
    }

    private Map<String, Object> resolveTasks(String source, boolean superUser, Set<UUID> deptIds) {
        if (source == null) return null;
        return switch (source) {
            case "TACHES_EN_COURS" -> openTasks(superUser ? null : deptIds);
            default -> null;
        };
    }

    /* ------------------- Implémentations des sources ------------------- */

    /** Répartition des âmes par statut (données {name, value} pour camembert/barres). */
    private Map<String, Object> soulsByStatutChart(Set<UUID> soulIds) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (StatutAme statut : StatutAme.values()) {
            long count = soulIds == null
                    ? soulRepository.countByStatut(statut)
                    : countSoulsByStatut(soulIds, statut);
            if (count > 0) {
                data.add(entry(statutLabel(statut), count));
            }
        }
        return Map.of("data", data);
    }

    /** Événements par mois sur les 6 prochains mois (barres). */
    private Map<String, Object> eventsByMonthChart() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.withDayOfMonth(1);
        LocalDateTime end = now.plusMonths(6);
        List<Event> events = eventRepository.findByDateDebutBetweenAndDeletedFalse(start, end);
        Map<String, Long> counts = events.stream()
                .filter(e -> e.getDateDebut() != null)
                .collect(Collectors.groupingBy(e -> e.getDateDebut().format(DATE_MONTH_FMT), Collectors.counting()));
        List<Map<String, Object>> data = new ArrayList<>();
        LocalDateTime cursor = start;
        while (!cursor.isAfter(end)) {
            String key = cursor.format(DATE_MONTH_FMT);
            data.add(entry(cursor.format(DATE_MONTH_LABEL_FMT), counts.getOrDefault(key, 0L)));
            cursor = cursor.plusMonths(1);
        }
        return Map.of("data", data);
    }

    /** Alertes actives regroupées par type (données {name, value}). */
    private Map<String, Object> alertsByTypeChart(Set<UUID> soulIds) {
        List<Alert> alerts = soulIds == null
                ? alertRepository.findByStatut(StatutAlerte.ACTIVE)
                : (soulIds.isEmpty()
                        ? List.of()
                        : alertRepository.findByStatutAndAmeIdIn(StatutAlerte.ACTIVE, soulIds));
        Map<String, Long> counts = alerts.stream()
                .map(a -> a.getTypeAlerte() == null || a.getTypeAlerte().isBlank() ? "AUTRE" : a.getTypeAlerte())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<Map<String, Object>> data = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> entry(humanize(e.getKey()), e.getValue()))
                .toList();
        return Map.of("data", data);
    }

    /** Répartition des départements par statut (données {name, value}). */
    private Map<String, Object> departmentsByStatutChart(Set<UUID> deptIds) {
        List<Department> departments = deptIds == null
                ? departmentRepository.findByDeletedFalseOrderByNomAsc()
                : departmentRepository.findAllById(deptIds).stream()
                        .filter(d -> !d.isDeleted())
                        .toList();
        Map<StatutEntite, Long> counts = departments.stream()
                .collect(Collectors.groupingBy(d -> d.getStatut() == null ? StatutEntite.ACTIVE : d.getStatut(),
                        Collectors.counting()));
        List<Map<String, Object>> data = counts.entrySet().stream()
                .sorted(Map.Entry.<StatutEntite, Long>comparingByValue().reversed())
                .map(e -> entry(entiteLabel(e.getKey()), e.getValue()))
                .toList();
        return Map.of("data", data);
    }

    /** Événements des 60 prochains jours pour le calendrier. */
    private Map<String, Object> calendarEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> events = eventRepository.findByDateDebutBetweenAndDeletedFalse(now, now.plusDays(60));
        List<Map<String, Object>> items = events.stream()
                .filter(e -> e.getDateDebut() != null)
                .sorted(Comparator.comparing(Event::getDateDebut))
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("date", e.getDateDebut().format(DateTimeFormatter.ISO_LOCAL_DATE));
                    item.put("title", e.getTitre());
                    item.put("lieu", e.getLieu() == null || e.getLieu().isBlank() ? "" : e.getLieu());
                    item.put("type", e.getTypeEvenement() == null ? "" : e.getTypeEvenement());
                    return item;
                })
                .toList();
        return Map.of("events", items);
    }

    /** Les 10 dernières âmes créées (timeline). */
    private Map<String, Object> soulsTimeline(Set<UUID> soulIds) {
        List<Soul> souls = soulRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc();
        if (soulIds != null && !soulIds.isEmpty()) {
            souls = souls.stream().filter(s -> soulIds.contains(s.getId())).toList();
        } else if (soulIds != null) {
            souls = List.of();
        }
        List<Map<String, Object>> items = souls.stream()
                .map(s -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("date", s.getCreatedAt() != null
                            ? s.getCreatedAt().format(DATE_FMT) : "");
                    item.put("label", fullName(s));
                    item.put("value", s.getStatut() == null ? "" : statutLabel(s.getStatut()));
                    return item;
                })
                .toList();
        return Map.of("items", items);
    }

    /** Les 10 derniers documents versés (bloc FICHIERS), scopés par familles accessibles. */
    private Map<String, Object> recentFiles(Set<UUID> familyIds) {
        List<FileEntity> files = familyIds == null
                ? fileRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc()
                : (familyIds.isEmpty()
                        ? List.of()
                        : fileRepository.findTop10ByFamilleIdInAndDeletedFalseOrderByCreatedAtDesc(familyIds));
        List<Map<String, Object>> items = files.stream()
                .map(f -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("nom", f.getNom());
                    item.put("categorie", f.getCategorie() == null || f.getCategorie().isBlank()
                            ? "DOCUMENT" : humanize(f.getCategorie()));
                    item.put("typeFichier", f.getTypeFichier() == null ? "" : f.getTypeFichier());
                    item.put("taille", f.getTaille() == null ? 0L : f.getTaille());
                    item.put("date", f.getCreatedAt() != null ? f.getCreatedAt().format(DATE_FMT) : "");
                    return item;
                })
                .toList();
        return Map.of("items", items);
    }

    /** Les 10 prochaines tâches ouvertes par échéance (bloc TÂCHES), scopées par départements. */
    private Map<String, Object> openTasks(Set<UUID> deptIds) {
        List<DepartmentTask.TaskStatus> open = List.of(
                DepartmentTask.TaskStatus.A_FAIRE,
                DepartmentTask.TaskStatus.EN_COURS,
                DepartmentTask.TaskStatus.BLOQUEE);
        List<DepartmentTask> tasks = deptIds == null
                ? taskRepository.findTop10ByStatutInOrderByEcheanceAsc(open)
                : (deptIds.isEmpty()
                        ? List.of()
                        : taskRepository.findTop10ByStatutInAndDepartmentIdInOrderByEcheanceAsc(open, deptIds));
        Set<UUID> ids = tasks.stream().map(DepartmentTask::getDepartmentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> names = ids.isEmpty() ? Map.of()
                : departmentRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(Department::getId, Department::getNom));
        List<Map<String, Object>> items = tasks.stream()
                .map(t -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("titre", t.getTitre());
                    item.put("departement", t.getDepartmentId() != null
                            ? names.getOrDefault(t.getDepartmentId(), "—") : "—");
                    item.put("echeance", t.getEcheance() != null
                            ? t.getEcheance().format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
                    item.put("priorite", t.getPriorite() == null ? "" : t.getPriorite().name());
                    return item;
                })
                .toList();
        return Map.of("items", items);
    }

    private long countSoulsByStatut(Set<UUID> soulIds, StatutAme statut) {
        if (soulIds == null) {
            return soulRepository.countByStatut(statut);
        }
        if (soulIds.isEmpty()) {
            return 0;
        }
        return soulRepository.findAllById(soulIds).stream()
                .filter(s -> !s.isDeleted() && s.getStatut() == statut)
                .count();
    }

    private Map<String, Object> recentSoulsTable(Set<UUID> soulIds) {
        List<Soul> souls = soulRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc();
        if (soulIds != null && !soulIds.isEmpty()) {
            souls = souls.stream().filter(s -> soulIds.contains(s.getId())).toList();
        } else if (soulIds != null) {
            souls = List.of();
        }
        Map<UUID, String> familyNames = familyNamesOf(souls);
        List<List<Object>> rows = souls.stream()
                .map(s -> Arrays.<Object>asList(fullName(s), label(s.getStatut()),
                        s.getFamilleId() != null ? familyNames.getOrDefault(s.getFamilleId(), "—") : "—"))
                .toList();
        return Map.of("headers", List.of("Nom", "Statut", "Famille"), "rows", rows);
    }

    private Map<String, Object> upcomingEventsTable() {
        List<Event> events = eventRepository.findTop10ByDeletedFalseAndDateDebutAfterOrderByDateDebutAsc(LocalDateTime.now());
        List<List<Object>> rows = events.stream()
                .map(e -> Arrays.<Object>asList(e.getTitre(),
                        e.getDateDebut() != null ? e.getDateDebut().format(DATE_TIME_FMT) : "—",
                        e.getLieu() == null || e.getLieu().isBlank() ? "—" : e.getLieu()))
                .toList();
        return Map.of("headers", List.of("Événement", "Date", "Lieu"), "rows", rows);
    }

    private Map<String, Object> recentTransfersTable(UUID userId) {
        List<TransferRequest> transfers = userId == null
                ? transferRepository.findTop10ByOrderByCreatedAtDesc()
                : transferRepository.findTop10ByDemandeurIdOrderByCreatedAtDesc(userId);
        List<List<Object>> rows = transfers.stream()
                .map(t -> Arrays.<Object>asList(
                        transferSubject(t),
                        t.getStatut() != null ? t.getStatut().name() : "—",
                        t.getCreatedAt() != null ? t.getCreatedAt().format(DATE_FMT) : "—"))
                .toList();
        return Map.of("headers", List.of("Objet", "Statut", "Date"), "rows", rows);
    }

    private Map<String, Object> departmentsTable(Set<UUID> deptIds) {
        List<Department> departments = deptIds == null
                ? departmentRepository.findByDeletedFalseOrderByNomAsc()
                : departmentRepository.findAllById(deptIds).stream()
                        .filter(d -> !d.isDeleted())
                        .sorted(Comparator.comparing(Department::getNom, String.CASE_INSENSITIVE_ORDER))
                        .toList();
        List<List<Object>> rows = departments.stream()
                .map(d -> Arrays.<Object>asList(d.getNom(), d.getStatut() != null ? d.getStatut().name() : "—"))
                .toList();
        return Map.of("headers", List.of("Département", "Statut"), "rows", rows);
    }

    private Map<String, Object> recentAlertsList(Set<UUID> soulIds) {
        List<Alert> alerts = soulIds == null
                ? alertRepository.findTop10ByStatutOrderByDateDeclenchementDesc(StatutAlerte.ACTIVE)
                : (soulIds.isEmpty()
                        ? List.of()
                        : alertRepository.findTop10ByStatutAndAmeIdInOrderByDateDeclenchementDesc(StatutAlerte.ACTIVE, soulIds));
        Map<UUID, String> soulNames = soulNamesOf(alerts.stream()
                .map(Alert::getAmeId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<Map<String, Object>> items = alerts.stream()
                .map(a -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    String subject = a.getTitre() != null && !a.getTitre().isBlank()
                            ? a.getTitre() : a.getMessage() != null ? a.getMessage() : "Alerte";
                    item.put("label", subject);
                    item.put("value", a.getAmeId() != null ? soulNames.getOrDefault(a.getAmeId(), "") : "");
                    return item;
                })
                .toList();
        return Map.of("items", items);
    }

    /* ----------------------------- Helpers ----------------------------- */

    private Map<String, Object> value(long v) {
        return Map.of("value", v);
    }

    private static Map<String, Object> entry(String name, long value) {
        return Map.of("name", name, "value", value);
    }

    /** Libellé français d'un statut d'âme. */
    private static String statutLabel(StatutAme statut) {
        return switch (statut) {
            case NOUVEAU_CONVERTI -> "Nouveau converti";
            case NOUVEL_ARRIVANT -> "Nouvel arrivant";
            case EN_INTEGRATION -> "En intégration";
            case ACTIF -> "Actif";
            case EN_VEILLE -> "En veille";
            case DECROCHE -> "Décroché";
        };
    }

    /** Libellé français d'un statut d'entité (département). */
    private static String entiteLabel(StatutEntite statut) {
        return switch (statut) {
            case ACTIVE -> "Actif";
            case INACTIVE -> "Inactif";
            case ARCHIVED -> "Archivé";
        };
    }

    /** Humanise une clé technique (ASSIDUITE → Assiduité). */
    private static String humanize(String raw) {
        return Arrays.stream(raw.toLowerCase(Locale.ROOT).split("_"))
                .filter(s -> !s.isBlank())
                .map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    private Map<UUID, String> familyNamesOf(List<Soul> souls) {
        Set<UUID> ids = souls.stream().map(Soul::getFamilleId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return familyRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Family::getId, Family::getNom));
    }

    private Map<UUID, String> soulNamesOf(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return soulRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Soul::getId, PageBuilderService::fullName));
    }

    private static String fullName(Soul s) {
        return (s.getPrenom() == null ? "" : s.getPrenom() + " ") + (s.getNom() == null ? "" : s.getNom());
    }

    private static String label(Enum<?> e) {
        return e == null ? "—" : e.name();
    }

    private String transferSubject(TransferRequest t) {
        if (t.getNouvelleAffectation() != null
                && t.getNouvelleAffectation().get("type") instanceof String type && !type.isBlank()) {
            return "Transfert " + type.toLowerCase(Locale.ROOT).replace('_', ' ');
        }
        if (t.getType() != null) {
            return "Transfert " + t.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        }
        return "Demande de transfert";
    }

    /* ---------------------------- Validation --------------------------- */

    private void validate(CustomPage page) {
        if (page.getKey() == null || page.getKey().isBlank()) {
            throw new IllegalArgumentException("La clé de la page est obligatoire.");
        }
        if (page.getTitle() == null || page.getTitle().isBlank()) {
            throw new IllegalArgumentException("Le titre de la page est obligatoire.");
        }
        if (page.getSlug() == null || page.getSlug().isBlank()) {
            throw new IllegalArgumentException("L'adresse de la page (slug) est obligatoire.");
        }
        if (page.getLayout() != null && !page.getLayout().isBlank() && !LAYOUTS.contains(page.getLayout())) {
            throw new IllegalArgumentException("Disposition inconnue : " + page.getLayout());
        }
        validateBlocks(page.getBlocks());
    }

    @SuppressWarnings("unchecked")
    private void validateBlocks(List<Map<String, Object>> blocks) {
        if (blocks == null) return;
        for (Map<String, Object> block : blocks) {
            String type = String.valueOf(block.getOrDefault("type", "")).toUpperCase();
            if (!BLOCK_TYPES.contains(type)) {
                throw new IllegalArgumentException("Type de bloc inconnu : " + type);
            }
            Map<String, Object> config = block.get("config") instanceof Map<?, ?> m
                    ? (Map<String, Object>) m : Map.of();
            switch (type) {
                case "KPI", "TABLEAU", "LISTE", "GRAPHIQUE", "CALENDRIER", "TIMELINE",
                     "FICHIERS", "TACHES" -> {
                    if (!(config.get("source") instanceof String s) || s.isBlank()) {
                        throw new IllegalArgumentException("Le bloc " + type + " nécessite une source de données.");
                    }
                    if (!sourceExists(String.valueOf(s).toUpperCase(), type)) {
                        throw new IllegalArgumentException("Source inconnue : " + config.get("source"));
                    }
                }
                case "LIENS", "CHECKLIST" -> {
                    Object items = config.get("items");
                    if (!(items instanceof List<?> list) || list.isEmpty()) {
                        throw new IllegalArgumentException("Le bloc " + type + " nécessite au moins un élément.");
                    }
                }
                case "FORMULAIRE" -> {
                    Set<String> cibles = Set.of("PASTEUR", "RESPONSABLE", "CHEF_DE_FAMILLE");
                    Set<String> types = Set.of("SUGGESTION", "RENDEZ_VOUS", "SIGNALEMENT");
                    if (!(config.get("cible") instanceof String cible) || !cibles.contains(cible.toUpperCase())) {
                        throw new IllegalArgumentException("Le bloc FORMULAIRE nécessite une cible valide (PASTEUR, RESPONSABLE ou CHEF_DE_FAMILLE).");
                    }
                    if (!(config.get("type") instanceof String typeF) || !types.contains(typeF.toUpperCase())) {
                        throw new IllegalArgumentException("Le bloc FORMULAIRE nécessite un type valide (SUGGESTION, RENDEZ_VOUS ou SIGNALEMENT).");
                    }
                }
                case "IMAGES" -> {
                    if (!(config.get("url") instanceof String url) || url.isBlank()) {
                        throw new IllegalArgumentException("Le bloc IMAGES nécessite une URL d'image.");
                    }
                }
                default -> { /* TEXTE / RECHERCHE : config libre */ }
            }
        }
    }

    private boolean sourceExists(String source, String type) {
        return sources().stream().anyMatch(s -> s.key().equals(source) && s.type().equals(type));
    }

    private static String slugify(String raw) {
        String slug = raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isBlank() ? "page" : slug;
    }

    private Map<String, Object> pagePayload(CustomPage p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", p.getTitle());
        map.put("description", p.getDescription());
        map.put("slug", p.getSlug());
        map.put("layout", p.getLayout());
        map.put("blocks", p.getBlocks());
        map.put("roles", p.getRoles());
        map.put("enabled", p.isEnabled());
        map.put("published", p.isPublished());
        map.put("version", p.getVersion());
        return map;
    }

    /** Décrit les rôles autorisés d'une page pour l'affichage dans l'éditeur. */
    @Transactional(readOnly = true)
    public Map<String, Object> roleOptions() {
        return Map.of(
                "roles", List.of("ADMIN", "PASTEUR", "RESPONSABLE", "CHEF_DE_FAMILLE", "FAISEUR", "MEMBRE"),
                "layouts", List.of("STACK", "GRID_2", "GRID_3"),
                "blockTypes", BLOCK_TYPES.stream().sorted().toList());
    }
}
