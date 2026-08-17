package com.discipolat.modules.platform.domain;

import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TransferStatus;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.alerts.domain.Alert;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
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
            "KPI", "TABLEAU", "LISTE", "TEXTE", "LIENS", "RECHERCHE", "IMAGES");
    private static final Set<String> LAYOUTS = Set.of("STACK", "GRID_2", "GRID_3");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CustomPageRepository pageRepository;
    private final ConfigRevisionService revisionService;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final WorkspaceScopeService scopeService;
    private final SoulRepository soulRepository;
    private final FamilyRepository familyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final AlertRepository alertRepository;
    private final TransferRequestRepository transferRepository;

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
                              TransferRequestRepository transferRepository) {
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
                        "Départements et leur statut, dans votre périmètre.", false));
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
            default -> null; // TEXTE / LIENS / RECHERCHE / IMAGES : pas de source
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

    /* ------------------- Implémentations des sources ------------------- */

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
                case "KPI", "TABLEAU", "LISTE" -> {
                    if (!(config.get("source") instanceof String s) || s.isBlank()) {
                        throw new IllegalArgumentException("Le bloc " + type + " nécessite une source de données.");
                    }
                    if (!sourceExists(String.valueOf(s).toUpperCase(), type)) {
                        throw new IllegalArgumentException("Source inconnue : " + config.get("source"));
                    }
                }
                case "LIENS" -> {
                    Object items = config.get("items");
                    if (!(items instanceof List<?> list) || list.isEmpty()) {
                        throw new IllegalArgumentException("Le bloc LIENS nécessite au moins un lien.");
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
        String t = "TABLEAU".equals(type) || "LISTE".equals(type) ? type : "KPI";
        return sources().stream().anyMatch(s -> s.key().equals(source) && s.type().equals(t));
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
