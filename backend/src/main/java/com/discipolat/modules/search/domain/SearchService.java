package com.discipolat.modules.search.domain;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.modules.discipline.domain.SoulDisciplineEventRepository;
import com.discipolat.modules.evaluations.domain.EvaluationService;
import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.StatutSuiviParallele;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.alerts.domain.Alert;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowup;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowupRepository;
import com.discipolat.modules.prayers.domain.Prayer;
import com.discipolat.modules.prayers.domain.PrayerRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.search.domain.SearchAuditRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartment;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulExitRepository;
import com.discipolat.modules.souls.domain.SoulHistoryRepository;
import com.discipolat.modules.souls.domain.SoulNote;
import com.discipolat.modules.souls.domain.SoulNoteRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final DepartmentRepository departmentRepository;
    private final MakerReportRepository makerReportRepository;
    private final PrayerRepository prayerRepository;
    private final SoulNoteRepository soulNoteRepository;
    private final SoulHistoryRepository soulHistoryRepository;
    private final SoulExitRepository soulExitRepository;
    private final AlertRepository alertRepository;
    private final ParallelFollowupRepository parallelFollowupRepository;
    private final EvaluationService evaluationService;
    private final SoulDisciplineEventRepository disciplineEventRepository;
    private final SoulDepartmentRepository soulDepartmentRepository;
    private final SearchAuditRepository searchAuditRepository;
    private final EntityManager entityManager;
    private final SecurityUtils securityUtils;
    private final boolean isPostgreSQL;

    @Autowired
    public SearchService(SoulRepository soulRepository, UserRepository userRepository,
                         FamilyRepository familyRepository, DepartmentRepository departmentRepository,
                         MakerReportRepository makerReportRepository, PrayerRepository prayerRepository,
                         SoulNoteRepository soulNoteRepository, SoulHistoryRepository soulHistoryRepository,
                         SoulExitRepository soulExitRepository, AlertRepository alertRepository,
                         ParallelFollowupRepository parallelFollowupRepository,
                         EvaluationService evaluationService,
                         SoulDisciplineEventRepository disciplineEventRepository,
                         SoulDepartmentRepository soulDepartmentRepository,
                         SearchAuditRepository searchAuditRepository,
                         EntityManager entityManager,
                         SecurityUtils securityUtils,
                         DataSource dataSource) {
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.departmentRepository = departmentRepository;
        this.makerReportRepository = makerReportRepository;
        this.prayerRepository = prayerRepository;
        this.soulNoteRepository = soulNoteRepository;
        this.soulHistoryRepository = soulHistoryRepository;
        this.soulExitRepository = soulExitRepository;
        this.alertRepository = alertRepository;
        this.parallelFollowupRepository = parallelFollowupRepository;
        this.evaluationService = evaluationService;
        this.disciplineEventRepository = disciplineEventRepository;
        this.soulDepartmentRepository = soulDepartmentRepository;
        this.searchAuditRepository = searchAuditRepository;
        this.entityManager = entityManager;
        this.securityUtils = securityUtils;
        
        // Detect database type for full-text search compatibility
        this.isPostgreSQL = detectPostgreSQL(dataSource);
    }
    
    private boolean detectPostgreSQL(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            return "PostgreSQL".equalsIgnoreCase(databaseProductName);
        } catch (Exception e) {
            // Default to false if detection fails
            return false;
        }
    }

    /**
     * Search for souls across the system with role-based filtering.
     * Uses PostgreSQL full-text search (pg_trgm) when available, falls back to simple search otherwise.
     * - PASTEUR/ADMIN: sees all souls
     * - RESPONSABLE: sees souls in their departments
     * - Chef de famille (FAISEUR with estChefDeFamille): sees souls in their family
     * - FAISEUR: sees only their assigned souls
     */
    public Page<Map<String, Object>> search(String query, Pageable pageable) {
        Instant startTime = Instant.now();
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UUID tenantId = TenantContext.getCurrentTenantId();
        
        // Use PostgreSQL full-text search if available, otherwise fall back to simple search
        List<Map<String, Object>> results;
        long totalCount;
        
        if (isPostgreSQL) {
            // Build the WHERE clause for tenant + role-based access
            String accessClause = buildAccessClause(currentUser);
            
            // Build the full-text search query using tsvector + trigram similarity
            String searchQuery = buildSearchQuery(query, accessClause);
            
            // Execute search with pagination
            results = executeFullTextSearch(searchQuery, pageable, query);
            
            // Get total count for pagination
            totalCount = countSearchResults(searchQuery, query);
        } else {
            // Fallback: simple search using JPA repositories (for H2 tests)
            results = executeSimpleSearch(query, currentUser, pageable);
            totalCount = countSimpleSearch(query, currentUser);
        }
        
        // Audit the search
        long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();
        auditSearch(tenantId, currentUserId, query, results.size(), executionTimeMs);
        
        return new PageImpl<>(results, pageable, totalCount);
    }
    
    /**
     * Autocomplete search using trigram similarity for fast type-ahead suggestions.
     * Uses PostgreSQL trigram similarity when available, falls back to simple prefix search otherwise.
     */
    public List<Map<String, Object>> autocomplete(String query, int limit) {
        if (query == null || query.length() < 2) {
            return List.of();
        }
        
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (isPostgreSQL) {
            return autocompletePostgreSQL(query, limit, currentUser);
        } else {
            return autocompleteSimple(query, limit, currentUser);
        }
    }
    
    private List<Map<String, Object>> autocompletePostgreSQL(String query, int limit, User currentUser) {
        String accessClause = buildAccessClause(currentUser);
        String similarityThreshold = "0.3"; // Minimum similarity for autocomplete
        
        String sql = """
            SELECT s.id, s.nom, s.prenom, s.email, s.telephone, s.statut, s.type_disciple,
                   similarity(s.nom, :query) as sim_nom,
                   similarity(s.prenom, :query) as sim_prenom,
                   similarity(s.email, :query) as sim_email
            FROM souls s
            WHERE (%s)
            AND (similarity(s.nom, :query) > :threshold 
                 OR similarity(s.prenom, :query) > :threshold 
                 OR similarity(s.email, :query) > :threshold)
            ORDER BY GREATEST(similarity(s.nom, :query), similarity(s.prenom, :query), similarity(s.email, :query)) DESC
            LIMIT :limit
            """.formatted(accessClause);
        
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("query", query);
        q.setParameter("tenantId", TenantContext.getCurrentTenantId());
        q.setParameter("threshold", similarityThreshold);
        q.setParameter("limit", limit);
        
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        
        return rows.stream().map(row -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "AME");
            result.put("id", row[0]);
            result.put("nom", row[1]);
            result.put("prenom", row[2]);
            result.put("email", row[3]);
            result.put("telephone", row[4]);
            result.put("statut", row[5]);
            result.put("typeDisciple", row[6]);
            result.put("nomComplet", (row[1] != null ? row[1] : "") + " " + (row[2] != null ? row[2] : ""));
            return result;
        }).toList();
    }
    
    private List<Map<String, Object>> autocompleteSimple(String query, int limit, User currentUser) {
        Set<UUID> accessibleSoulIds = getAccessibleSoulIds(currentUser);
        List<Soul> allSouls;
        
        if (accessibleSoulIds != null) {
            allSouls = soulRepository.findAllById(accessibleSoulIds).stream()
                    .filter(s -> !s.isDeleted())
                    .filter(s -> matchesAutocompleteQuery(s, query))
                    .sorted((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()))
                    .limit(limit)
                    .toList();
        } else {
            allSouls = soulRepository.findAll().stream()
                    .filter(s -> !s.isDeleted())
                    .filter(s -> matchesAutocompleteQuery(s, query))
                    .sorted((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()))
                    .limit(limit)
                    .toList();
        }
        
        return allSouls.stream().map(this::soulToSearchResult).toList();
    }
    
    private boolean matchesAutocompleteQuery(Soul soul, String query) {
        String q = query.toLowerCase().trim();
        return (soul.getNom() != null && soul.getNom().toLowerCase().startsWith(q))
                || (soul.getPrenom() != null && soul.getPrenom().toLowerCase().startsWith(q))
                || (soul.getEmail() != null && soul.getEmail().toLowerCase().startsWith(q))
                || (soul.getTelephone() != null && soul.getTelephone().startsWith(q));
    }
    
    /**
     * Build access control clause based on user's active role.
     * Always includes tenant_id filter for multi-tenant isolation.
     */
    private String buildAccessClause(User currentUser) {
        StringBuilder clause = new StringBuilder();
        
        // Base: only non-deleted souls in current tenant (always required for multi-tenancy)
        clause.append("s.tenant_id = :tenantId AND s.deleted = false");
        
        // For super users (PASTEUR/ADMIN), no additional restrictions
        if (securityUtils.isSuperUser()) {
            return clause.toString();
        }
        
        Set<UUID> accessibleSoulIds = getAccessibleSoulIds(currentUser);
        if (accessibleSoulIds != null && !accessibleSoulIds.isEmpty()) {
            // Use parameterized IN clause for accessible IDs
            clause.append(" AND s.id IN :accessibleIds");
        } else if (accessibleSoulIds != null && accessibleSoulIds.isEmpty()) {
            // No accessible souls - return empty
            clause.append(" AND 1=0");
        }
        // If null (should not happen for non-super users), fallback to empty
        
        return clause.toString();
    }
    
    /**
     * Build the full-text search query using tsvector and trigram similarity.
     */
    private String buildSearchQuery(String query, String accessClause) {
        if (query == null || query.isBlank()) {
            // No query - return all accessible
            return """
                SELECT s.id, s.nom, s.prenom, s.email, s.telephone, s.statut, s.type_disciple,
                       s.faiseur_id, s.famille_id, s.date_integration, s.date_dernier_contact,
                       ts_rank_cd(s.search_vector, plainto_tsquery('french', '')) as rank
                FROM souls s
                WHERE %s
                ORDER BY s.nom ASC, s.prenom ASC
                """.formatted(accessClause);
        }
        
        String sanitizedQuery = query.replace("'", "''"); // Basic sanitization for tsquery
        
        return """
            SELECT s.id, s.nom, s.prenom, s.email, s.telephone, s.statut, s.type_disciple,
                   s.faiseur_id, s.famille_id, s.date_integration, s.date_dernier_contact,
                   ts_rank_cd(s.search_vector, plainto_tsquery('french', :query)) as rank,
                   GREATEST(
                       similarity(s.nom, :query),
                       similarity(s.prenom, :query),
                       similarity(s.email, :query),
                       similarity(s.telephone, :query)
                   ) as trigram_sim
            FROM souls s
            WHERE %s
            AND (s.search_vector @@ plainto_tsquery('french', :query)
                 OR similarity(s.nom, :query) > 0.3
                 OR similarity(s.prenom, :query) > 0.3
                 OR similarity(s.email, :query) > 0.3
                 OR similarity(s.telephone, :query) > 0.3)
            ORDER BY rank DESC, trigram_sim DESC, s.nom ASC
            """.formatted(accessClause);
    }
    
    /**
     * Execute the full-text search query with pagination.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> executeFullTextSearch(String searchQuery, Pageable pageable, String query) {
        Query q = entityManager.createNativeQuery(searchQuery);
        q.setParameter("query", query != null ? query : "");
        q.setParameter("tenantId", TenantContext.getCurrentTenantId());
        
        // Add accessible IDs if needed
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow();
        Set<UUID> accessibleSoulIds = getAccessibleSoulIds(currentUser);
        if (accessibleSoulIds != null && !accessibleSoulIds.isEmpty()) {
            q.setParameter("accessibleIds", accessibleSoulIds);
        }
        
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        
        List<Object[]> rows = q.getResultList();
        
        return rows.stream().map(row -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "AME");
            result.put("id", row[0]);
            result.put("nom", row[1]);
            result.put("prenom", row[2]);
            result.put("email", row[3]);
            result.put("telephone", row[4]);
            result.put("statut", row[5]);
            result.put("typeDisciple", row[6]);
            result.put("faiseurId", row[7]);
            result.put("familleId", row[8]);
            result.put("dateIntegration", row[9]);
            result.put("dateDernierContact", row[10]);
            result.put("rank", row[11]);
            result.put("trigramSimilarity", row[12]);
            
            String nomComplet = (row[1] != null ? row[1] : "") + " " + (row[2] != null ? row[2] : "");
            result.put("nomComplet", nomComplet.trim());
            
            // Get faiseur name
            if (row[7] != null) {
                userRepository.findById((UUID) row[7]).ifPresent(f ->
                        result.put("faiseurNom", f.getFirstName() + " " + f.getLastName()));
            }
            
            // Get family name
            if (row[8] != null) {
                familyRepository.findById((UUID) row[8]).ifPresent(fam ->
                        result.put("familleNom", fam.getNom()));
            }
            
            // Years in church
            if (row[9] != null) {
                java.time.LocalDate dateInt = ((java.sql.Date) row[9]).toLocalDate();
                result.put("anneesDansEglise", java.time.Period.between(dateInt, java.time.LocalDate.now()).getYears());
            }
            
            return result;
        }).toList();
    }
    
    /**
     * Count total results for pagination.
     */
    private long countSearchResults(String searchQuery, String query) {
        // Extract the WHERE clause from the search query
        String countQuery = searchQuery
            .replaceFirst("(?s)SELECT.*?FROM", "SELECT COUNT(*) FROM")
            .replaceFirst("(?s)ORDER BY.*", "");
        
        Query q = entityManager.createNativeQuery(countQuery);
        q.setParameter("query", query != null ? query : "");
        q.setParameter("tenantId", TenantContext.getCurrentTenantId());
        
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow();
        Set<UUID> accessibleSoulIds = getAccessibleSoulIds(currentUser);
        if (accessibleSoulIds != null && !accessibleSoulIds.isEmpty()) {
            q.setParameter("accessibleIds", accessibleSoulIds);
        }
        
        return ((Number) q.getSingleResult()).longValue();
    }
    
    /**
     * Audit the search query for analytics and security.
     */
    private void auditSearch(UUID tenantId, UUID userId, String query, int resultsCount, long executionTimeMs) {
        try {
            com.discipolat.modules.search.domain.SearchAudit audit = new com.discipolat.modules.search.domain.SearchAudit();
            audit.setTenantId(tenantId);
            audit.setUserId(userId);
            audit.setQueryText(query);
            audit.setEntityTypes(new String[]{"SOULS"}); // Could be extended for multi-entity search
            audit.setResultsCount(resultsCount);
            audit.setExecutionTimeMs((int) executionTimeMs);
            searchAuditRepository.save(audit);
        } catch (Exception e) {
            // Log but don't fail the search
            System.err.println("Failed to audit search: " + e.getMessage());
        }
    }

    /**
     * Get the complete profile for a specific soul (all related data in one view).
     * Role-based: user can only access souls they have permission to see.
     */
    public Map<String, Object> getCompleteProfile(UUID soulId) {
        Soul soul = soulRepository.findById(soulId)
                .orElseThrow(() -> new RuntimeException("Soul not found: " + soulId));

        // Check access
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        checkSoulAccess(soul, currentUser);

        Map<String, Object> profile = new LinkedHashMap<>();

        // ===== 1. Personal information =====
        Map<String, Object> personalInfo = new LinkedHashMap<>();
        personalInfo.put("id", soul.getId());
        personalInfo.put("nom", soul.getNom());
        personalInfo.put("prenom", soul.getPrenom());
        personalInfo.put("nomComplet", soul.getNomComplet());
        personalInfo.put("email", soul.getEmail());
        personalInfo.put("telephone", soul.getTelephone());
        personalInfo.put("adresse", soul.getAdresse());
        personalInfo.put("dateNaissance", soul.getDateNaissance());
        if (soul.getDateNaissance() != null) {
            personalInfo.put("age", Period.between(soul.getDateNaissance(), LocalDate.now()).getYears());
        }
        personalInfo.put("profession", soul.getProfession());
        personalInfo.put("situationFamiliale", soul.getSituationFamiliale());
        profile.put("informationsPersonnelles", personalInfo);

        // ===== 2. Church information =====
        Map<String, Object> churchInfo = new LinkedHashMap<>();
        churchInfo.put("dateIntegration", soul.getDateIntegration());
        churchInfo.put("dateConversion", soul.getDateConversion());
        churchInfo.put("typeDisciple", soul.getTypeDisciple().name());
        churchInfo.put("statut", soul.getStatut().name());
        churchInfo.put("etatSpirituel", soul.getEtatSpirituel());
        churchInfo.put("niveauCroissance", soul.getNiveauCroissance());
        churchInfo.put("dateDernierContact", soul.getDateDernierContact());
        churchInfo.put("anneesDansEglise", soul.getDateIntegration() != null
                ? Period.between(soul.getDateIntegration(), LocalDate.now()).getYears() : 0);
        profile.put("informationsEcclesiales", churchInfo);

        // ===== 3. Assignment chain =====
        Map<String, Object> assignments = new LinkedHashMap<>();
        assignments.put("faiseurId", soul.getFaiseurId());
        userRepository.findById(soul.getFaiseurId()).ifPresent(f -> {
            assignments.put("faiseurNom", f.getFirstName() + " " + f.getLastName());
            assignments.put("faiseurEmail", f.getEmail());
        });
        assignments.put("familleId", soul.getFamilleId());
        if (soul.getFamilleId() != null) {
            familyRepository.findById(soul.getFamilleId()).ifPresent(fam -> {
                assignments.put("familleNom", fam.getNom());
                // Get chef de famille
                userRepository.findById(fam.getChefFamilleId()).ifPresent(chef -> {
                    assignments.put("chefFamilleId", chef.getId());
                    assignments.put("chefFamilleNom", chef.getFirstName() + " " + chef.getLastName());
                });
                // Get department(s) via soul_departments
            });
        }
        // Disciples suivis by this soul (if they are a faiseur)
        List<Soul> disciplesSuivis = soulRepository.findAllByFaiseurId(soul.getId());
        if (!disciplesSuivis.isEmpty()) {
            assignments.put("disciplesSuivis", disciplesSuivis.stream()
                    .filter(d -> !d.isDeleted())
                    .map(d -> Map.of("id", d.getId(), "nom", d.getNomComplet(), "statut", d.getStatut().name()))
                    .toList());
            assignments.put("nombreDisciplesSuivis", disciplesSuivis.size());
        }
        profile.put("assignations", assignments);

        // ===== 4. Presence history (from maker reports) =====
        List<Map<String, Object>> presenceHistory = new ArrayList<>();
        Page<MakerReport> reports = makerReportRepository.findByAmeId(soul.getId(), PageRequest.of(0, 52));
        for (MakerReport report : reports.getContent()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", report.getId());
            entry.put("semaine", report.getSemaine());
            entry.put("presencesParCulte", report.getPresencesParCulte());
            entry.put("absenceRaison", report.getAbsenceRaison() != null ? report.getAbsenceRaison().name() : null);
            entry.put("absenceCommentaire", report.getAbsenceCommentaire());
            entry.put("soumis", report.isSoumis());
            entry.put("dateSoumission", report.getDateSoumission());
            presenceHistory.add(entry);
        }

        // Compute stats
        long totalReports = presenceHistory.size();
        long soumis = presenceHistory.stream().filter(r -> (boolean) r.get("soumis")).count();
        int totalPresents = 0;
        int totalPossible = 0;
        long absencesWithReason = 0;
        for (Map<String, Object> r : presenceHistory) {
            @SuppressWarnings("unchecked")
            Map<String, Boolean> presences = (Map<String, Boolean>) r.get("presencesParCulte");
            if (presences != null) {
                for (Boolean p : presences.values()) {
                    totalPossible++;
                    if (p) totalPresents++;
                }
            }
            if (r.get("absenceRaison") != null) absencesWithReason++;
        }

        Map<String, Object> presenceStats = new LinkedHashMap<>();
        presenceStats.put("totalRapports", totalReports);
        presenceStats.put("rapportsSoumis", soumis);
        presenceStats.put("tauxSoumission", totalReports > 0 ? Math.round((double) soumis / totalReports * 1000.0) / 10.0 : 0.0);
        presenceStats.put("totalPresences", totalPresents);
        presenceStats.put("totalAbsences", totalPossible - totalPresents);
        presenceStats.put("tauxPresence", totalPossible > 0 ? Math.round((double) totalPresents / totalPossible * 1000.0) / 10.0 : 0.0);
        presenceStats.put("absencesJustifiees", absencesWithReason);
        presenceStats.put("historique", presenceHistory);
        profile.put("presences", presenceStats);

        // ===== 5. Full history (soul_history) =====
        List<Map<String, Object>> fullHistory = soulHistoryRepository.findByAmeIdOrderByCreatedAtDesc(soul.getId())
                .stream()
                .map(h -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", h.getId());
                    entry.put("typeEvenement", h.getTypeEvenement());
                    entry.put("description", h.getDescription());
                    entry.put("ancienStatut", h.getAncienStatut());
                    entry.put("nouveauStatut", h.getNouveauStatut());
                    entry.put("ancienFaiseurId", h.getAncienFaiseurId());
                    entry.put("nouveauFaiseurId", h.getNouveauFaiseurId());
                    entry.put("utilisateurId", h.getUtilisateurId());
                    entry.put("date", h.getCreatedAt());
                    return entry;
                })
                .toList();
        profile.put("historiqueComplet", fullHistory);

        // ===== 6. Notes =====
        List<Map<String, Object>> notes = soulNoteRepository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(soul.getId())
                .stream()
                .map(n -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", n.getId());
                    entry.put("contenu", n.getContenu());
                    entry.put("auteurId", n.getAuteurId());
                    entry.put("date", n.getCreatedAt());
                    return entry;
                })
                .toList();
        profile.put("notes", notes);

        // ===== 7. Prayer requests =====
        List<Map<String, Object>> prayers = prayerRepository.findByAmeIdAndDeletedFalse(soul.getId())
                .stream()
                .map(p -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", p.getId());
                    entry.put("titre", p.getTitre());
                    entry.put("description", p.getDescription());
                    entry.put("categorie", p.getCategorie());
                    entry.put("priorite", p.getPriorite());
                    entry.put("statut", p.getStatut());
                    entry.put("temoignage", p.getTemoignage());
                    entry.put("dateCreation", p.getCreatedAt());
                    entry.put("dateExaucee", p.getDateExaucee());
                    return entry;
                })
                .toList();
        profile.put("demandesPriere", prayers);

        // ===== 8. Parallel followups =====
        List<Map<String, Object>> suivisParalleles = parallelFollowupRepository.findByAmeId(soul.getId())
                .stream()
                .map(sp -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", sp.getId());
                    entry.put("raison", sp.getRaison().name());
                    entry.put("raisonDetail", sp.getRaisonDetail());
                    entry.put("dateDebut", sp.getDateDebut());
                    entry.put("dateFin", sp.getDateFin());
                    entry.put("statut", sp.getStatut().name());
                    entry.put("initiateurId", sp.getInitiateurId());
                    return entry;
                })
                .toList();
        profile.put("suivisParalleles", suivisParalleles);

        // ===== 9. Alerts =====
        List<Map<String, Object>> alerts = alertRepository.findByAmeIdAndStatut(soul.getId(), StatutAlerte.ACTIVE)
                .stream()
                .map(a -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", a.getId());
                    entry.put("typeAlerte", a.getTypeAlerte());
                    entry.put("message", a.getMessage());
                    entry.put("dateDeclenchement", a.getDateDeclenchement());
                    entry.put("statut", a.getStatut().name());
                    return entry;
                })
                .toList();
        profile.put("alertes", alerts);

        // ===== 10. Exits history =====
        List<Map<String, Object>> exits = soulExitRepository.findByAmeIdOrderByCreatedAtDesc(soul.getId())
                .stream()
                .map(e -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", e.getId());
                    entry.put("motif", e.getMotif());
                    entry.put("motifDetail", e.getMotifDetail());
                    entry.put("dateSortie", e.getDateSortie());
                    entry.put("peutReintegrer", e.isPeutReintegrer());
                    return entry;
                })
                .toList();
        profile.put("sorties", exits);

        // ===== 11. Evaluation scores for the assignment chain =====
        Map<String, Object> evalScores = new LinkedHashMap<>();
        // Faiseur evaluation
        if (soul.getFaiseurId() != null) {
            Map<String, Object> faiseurEval = buildUserEvalScores(soul.getFaiseurId());
            if (!faiseurEval.isEmpty()) evalScores.put("faiseur", faiseurEval);
        }
        // Chef de famille evaluation
        if (soul.getFamilleId() != null) {
            familyRepository.findById(soul.getFamilleId()).ifPresent(fam -> {
                Map<String, Object> chefEval = buildUserEvalScores(fam.getChefFamilleId());
                if (!chefEval.isEmpty()) evalScores.put("chefFamille", chefEval);
                // Responsable evaluation (skipped - departments independent from families)
            });
        }
        if (!evalScores.isEmpty()) profile.put("evaluations", evalScores);

        // ===== 12. Discipline events (Phase 7) =====
        List<Map<String, Object>> disciplineEvents = disciplineEventRepository
                .findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(soul.getId())
                .stream()
                .map(d -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", d.getId());
                    entry.put("categorie", d.getCategorie().name());
                    entry.put("typeEvenement", d.getTypeEvenement());
                    entry.put("gravite", d.getGravite() != null ? d.getGravite().name() : null);
                    entry.put("titre", d.getTitre());
                    entry.put("description", d.getDescription());
                    entry.put("dateEvenement", d.getDateEvenement().toString());
                    entry.put("resolu", d.isResolu());
                    entry.put("dateResolution", d.getDateResolution() != null ? d.getDateResolution().toString() : null);
                    entry.put("auteurId", d.getAuteurId());
                    entry.put("createdAt", d.getCreatedAt().toString());
                    return entry;
                })
                .toList();
        profile.put("evenementsDisciplinaires", disciplineEvents);

        // Discipline stats
        long nonResolus = disciplineEvents.stream().filter(e -> !(boolean) e.get("resolu")).count();
        Map<String, Object> disciplineStats = new LinkedHashMap<>();
        disciplineStats.put("total", (long) disciplineEvents.size());
        disciplineStats.put("nonResolus", nonResolus);
        profile.put("statistiquesDisciplinaires", disciplineStats);

        return profile;
    }

    /** Build evaluation score summary for a user across all categories (delegates to cached EvaluationService) */
    private Map<String, Object> buildUserEvalScores(UUID userId) {
        return evaluationService.getUserEvalScores(userId);
    }

    // ======================== HELPERS ========================

    private boolean matchesQuery(Soul soul, String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase().trim();
        return (soul.getNom() != null && soul.getNom().toLowerCase().contains(q))
                || (soul.getPrenom() != null && soul.getPrenom().toLowerCase().contains(q))
                || (soul.getEmail() != null && soul.getEmail().toLowerCase().contains(q))
                || (soul.getTelephone() != null && soul.getTelephone().contains(q))
                || (soul.getProfession() != null && soul.getProfession().toLowerCase().contains(q))
                || (soul.getAdresse() != null && soul.getAdresse().toLowerCase().contains(q))
                || (soul.getNomComplet() != null && soul.getNomComplet().toLowerCase().contains(q));
    }

    private boolean matchesUserQuery(User user, String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase().trim();
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "")
                + " " + (user.getLastName() != null ? user.getLastName() : "");
        return (user.getEmail() != null && user.getEmail().toLowerCase().contains(q))
                || (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(q))
                || (user.getLastName() != null && user.getLastName().toLowerCase().contains(q))
                || (fullName.toLowerCase().contains(q));
    }

    private Map<String, Object> soulToSearchResult(Soul soul) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "AME");
        result.put("id", soul.getId());
        result.put("nom", soul.getNom());
        result.put("prenom", soul.getPrenom());
        result.put("nomComplet", soul.getNomComplet());
        result.put("email", soul.getEmail());
        result.put("telephone", soul.getTelephone());
        result.put("statut", soul.getStatut().name());
        result.put("typeDisciple", soul.getTypeDisciple().name());
        result.put("etatSpirituel", soul.getEtatSpirituel());

        // Get faiseur name
        userRepository.findById(soul.getFaiseurId()).ifPresent(f ->
                result.put("faiseurNom", f.getFirstName() + " " + f.getLastName()));

        // Get family name
        if (soul.getFamilleId() != null) {
            familyRepository.findById(soul.getFamilleId()).ifPresent(fam ->
                    result.put("familleNom", fam.getNom()));
        }

        result.put("dateIntegration", soul.getDateIntegration());
        result.put("dateDernierContact", soul.getDateDernierContact());

        // Years in church
        if (soul.getDateIntegration() != null) {
            result.put("anneesDansEglise", Period.between(soul.getDateIntegration(), LocalDate.now()).getYears());
        }

        return result;
    }

    /**
     * Returns the set of soul IDs accessible to the ACTIVE role (current workspace),
     * or null if all souls are accessible (super-users).
     * Un utilisateur multi-rôles ne voit que les âmes de l'espace métier courant.
     */
    private Set<UUID> getAccessibleSoulIds(User currentUser) {
        if (securityUtils.isSuperUser()) {
            return null; // All souls
        }

        Set<UUID> accessibleIds = new HashSet<>();

        // Responsable actif : membres de SES départements (et non toutes les familles)
        if (securityUtils.hasActiveRole("RESPONSABLE")) {
            List<UUID> deptIds = departmentRepository.findByResponsableId(currentUser.getId())
                    .stream().map(Department::getId).toList();
            if (!deptIds.isEmpty()) {
                soulDepartmentRepository.findByDepartmentIdIn(deptIds).stream()
                        .filter(SoulDepartment::isActif)
                        .map(SoulDepartment::getSoulId)
                        .forEach(accessibleIds::add);
            }
        }

        // Chef de famille actif : les âmes de la famille qu'il gère
        if (securityUtils.hasActiveRole("CHEF_DE_FAMILLE") && currentUser.getFamilleGereeId() != null) {
            List<Soul> familySouls = soulRepository.findAllByFamilleId(currentUser.getFamilleGereeId());
            familySouls.stream().map(Soul::getId).forEach(accessibleIds::add);
        }

        // FAISEUR actif : ses propres disciples
        if (securityUtils.hasActiveRole("FAISEUR")) {
            List<Soul> mySouls = soulRepository.findAllByFaiseurId(currentUser.getId());
            mySouls.stream().map(Soul::getId).forEach(accessibleIds::add);
        }

        return accessibleIds;
    }

    private void checkSoulAccess(Soul soul, User currentUser) {
        Set<UUID> accessibleIds = getAccessibleSoulIds(currentUser);
        if (accessibleIds != null && !accessibleIds.contains(soul.getId())) {
            throw new RuntimeException("Access denied to soul: " + soul.getId());
        }
    }
    
    // ======================== FALLBACK SIMPLE SEARCH (for H2 tests) ========================
    
    private List<Map<String, Object>> executeSimpleSearch(String query, User currentUser, Pageable pageable) {
        Set<UUID> accessibleSoulIds = getAccessibleSoulIds(currentUser);
        List<Soul> allSouls;
        
        if (accessibleSoulIds != null) {
            // Load only accessible souls and filter by query
            allSouls = soulRepository.findAllById(accessibleSoulIds).stream()
                    .filter(s -> !s.isDeleted())
                    .filter(s -> matchesQuery(s, query))
                    .sorted((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()))
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .toList();
        } else {
            // PASTEUR/ADMIN: search all souls
            allSouls = soulRepository.findAll().stream()
                    .filter(s -> !s.isDeleted())
                    .filter(s -> matchesQuery(s, query))
                    .sorted((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()))
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .toList();
        }
        
        // Also search users (faiseurs, chefs, responsables) if super-user (pasteur/admin actifs)
        List<Map<String, Object>> userResults = new ArrayList<>();
        if (securityUtils.isSuperUser()) {
            userRepository.findAll().stream()
                    .filter(u -> !u.isDeleted())
                    .filter(u -> matchesUserQuery(u, query))
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .forEach(u -> {
                        Map<String, Object> userEntry = new LinkedHashMap<>();
                        userEntry.put("type", "UTILISATEUR");
                        userEntry.put("id", u.getId());
                        userEntry.put("nom", u.getFirstName() + " " + u.getLastName());
                        userEntry.put("email", u.getEmail());
                        userEntry.put("role", u.getActiveRole() != null ? u.getActiveRole().name() : u.getRole().name());
                        userEntry.put("estChefDeFamille", u.isEstChefDeFamille());
                        userEntry.put("familleGereeId", u.getFamilleGereeId());
                        userResults.add(userEntry);
                    });
        }
        
        // Convert soul results
        List<Map<String, Object>> soulResults = allSouls.stream()
                .map(this::soulToSearchResult)
                .toList();
        
        // Combine results - souls first, then users
        List<Map<String, Object>> combined = new ArrayList<>();
        combined.addAll(soulResults);
        combined.addAll(userResults);
        
        return combined;
    }
    
    private long countSimpleSearch(String query, User currentUser) {
        Set<UUID> accessibleSoulIds = getAccessibleSoulIds(currentUser);
        long soulCount;
        
        if (accessibleSoulIds != null) {
            soulCount = soulRepository.findAllById(accessibleSoulIds).stream()
                    .filter(s -> !s.isDeleted())
                    .filter(s -> matchesQuery(s, query))
                    .count();
        } else {
            soulCount = soulRepository.findAll().stream()
                    .filter(s -> !s.isDeleted())
                    .filter(s -> matchesQuery(s, query))
                    .count();
        }
        
        long userCount = 0;
        if (securityUtils.isSuperUser()) {
            userCount = userRepository.findAll().stream()
                    .filter(u -> !u.isDeleted())
                    .filter(u -> matchesUserQuery(u, query))
                    .count();
        }
        
        return soulCount + userCount;
    }
}
