package com.discipolat.modules.dataMigration.domain;

import com.discipolat.modules.core.service.OutboxPublisher;
import com.discipolat.modules.people.domain.Membership;
import com.discipolat.modules.people.domain.Person;
import com.discipolat.modules.people.repository.MembershipRepository;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.spaces.domain.Space;
import com.discipolat.modules.spaces.domain.SpaceRepository;
import com.discipolat.modules.spaces.domain.SpaceStatus;
import com.discipolat.modules.spaces.domain.SpaceType;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeService;
import com.discipolat.modules.tenants.domain.OrganizationNodeType;
import com.discipolat.modules.dataMigration.repository.MigrationAuditRepository;
import com.discipolat.modules.dataMigration.repository.MigrationJobRepository;
import com.discipolat.modules.dataMigration.repository.MigrationSnapshotRepository;
import com.discipolat.modules.tenants.domain.TenantSettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * G4.6 — Moteur de migration des données legacy vers les tables Church OS.
 *
 * <p>Piloté par le toggle tenant {@code legacy_migration_enabled} (§G1.2) :
 * sans activation, toute exécution est refusée.</p>
 *
 * <p>Garanties contractuelles :</p>
 * <ul>
 *   <li><b>Non destructeur</b> : aucune donnée source n'est lue ailleurs que pour être
 *   copiée ; rien n'est déplacé ni effacé avant validation humaine.</li>
 *   <li><b>Dry-run</b> : simulation complète avec rapport par table (migrables / fusionnables /
 *   conflits / non mappables) sans aucune écriture cible.</li>
 *   <li><b>Replay idempotent</b> : déduplication par (tenant, source_table, source_id) tracée
 *   dans {@code migration_audit} ; relancer ne crée jamais de doublon.</li>
 *   <li><b>Rollback</b> : un job MIGRATE ≤ 30 jours et non validé comme définitif peut être
 *   annulé ; seules les lignes <em>créées</em> par ce job (snapshot) sont retirées.</li>
 *   <li><b>Scope tenant strict</b> : chaque lecture source est filtrée par {@code tenant_id}.</li>
 * </ul>
 */
@Service
@Transactional
@Slf4j
public class LegacyMigrationService {

    /** Statuts d'audit qui verrouillent le replay idempotent. */
    private static final List<MigrationAudit.RowStatus> DONE_STATUSES =
            List.of(MigrationAudit.RowStatus.MIGRATED, MigrationAudit.RowStatus.MERGED);

    /** Fenêtre de rollback d'un job (jours) au-delà de laquelle il devient définitif. */
    static final int ROLLBACK_WINDOW_DAYS = 30;

    /** Acteur technique pour les exécutions pilotées par l'outbox (aucun humain authentifié). */
    private static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final MigrationJobRepository jobs;
    private final MigrationAuditRepository audits;
    private final MigrationSnapshotRepository snapshots;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final PersonRepository personRepository;
    private final MembershipRepository membershipRepository;
    private final SpaceRepository spaceRepository;
    private final OrganizationNodeService organizationNodeService;
    private final JdbcTemplate jdbc;
    private final OutboxPublisher outboxPublisher;

    public LegacyMigrationService(MigrationJobRepository jobs,
                                  MigrationAuditRepository audits,
                                  MigrationSnapshotRepository snapshots,
                                  TenantSettingsRepository tenantSettingsRepository,
                                  PersonRepository personRepository,
                                  MembershipRepository membershipRepository,
                                  SpaceRepository spaceRepository,
                                  OrganizationNodeService organizationNodeService,
                                  JdbcTemplate jdbc,
                                  OutboxPublisher outboxPublisher) {
        this.jobs = jobs;
        this.audits = audits;
        this.snapshots = snapshots;
        this.tenantSettingsRepository = tenantSettingsRepository;
        this.personRepository = personRepository;
        this.membershipRepository = membershipRepository;
        this.spaceRepository = spaceRepository;
        this.organizationNodeService = organizationNodeService;
        this.jdbc = jdbc;
        this.outboxPublisher = outboxPublisher;
    }

    // ------------------------------------------------------------------
    // Catalogue des maps (source → cible, règles, champs non mappables)
    // ------------------------------------------------------------------

    public record LegacyMap(String moduleCode, String name, String description,
                            List<String> sourceTables, String targetTables,
                            List<String> unmappableFields) {}

    public static final List<LegacyMap> MAPS = List.of(
            new LegacyMap("PEOPLES", "Personnes (âmes legacy)",
                    "souls → person + membership (identité unique, dédoublonnage email/téléphone)",
                    List.of("souls"), "person, membership",
                    List.of("type_disciple", "niveau_croissance", "etat_spirituel", "notes_pasteur",
                            "suivi vers faith_journey (G3.7)")),
            new LegacyMap("SPACES", "Départements & familles",
                    "departments/families → organization_nodes + spaces (espaces configurables)",
                    List.of("departments", "families"), "organization_nodes, spaces",
                    List.of("membres_ids", "suivi vers space_membership (G3.2)")),
            new LegacyMap("EVENTS", "Événements",
                    "legacy_events → event (déjà migré par V158 ; audité ici pour traçabilité ligne à ligne)",
                    List.of("legacy_events", "events"), "event",
                    List.of("colonnes legacy spécifiques non conservées")));

    public List<LegacyMap> listMaps() {
        List<LegacyMap> result = new ArrayList<>();
        for (LegacyMap map : MAPS) {
            List<String> available = map.sourceTables().stream().filter(this::tableExists).toList();
            result.add(new LegacyMap(map.moduleCode(), map.name(), map.description(),
                    available, map.targetTables(), map.unmappableFields()));
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Dry-run & exécution
    // ------------------------------------------------------------------

    public MigrationJob dryRun(UUID tenantId, UUID actorId, String moduleCode) {
        return run(tenantId, actorId, moduleCode, MigrationJob.Mode.DRY_RUN);
    }

    public MigrationJob migrate(UUID tenantId, UUID actorId, String moduleCode) {
        return run(tenantId, actorId, moduleCode, MigrationJob.Mode.MIGRATE);
    }

    /** Consommateur outbox {@code LegacyDataNeedMigration} — exécution asynchrone du module demandé. */
    public void onMigrationRequested(UUID tenantId, String moduleCode) {
        migrate(tenantId, null, moduleCode);
    }

    private MigrationJob run(UUID tenantId, UUID actorId, String moduleCode, MigrationJob.Mode mode) {
        requireModule(moduleCode);
        if (mode == MigrationJob.Mode.MIGRATE) {
            requireEnabled(tenantId);
        }

        MigrationJob job = new MigrationJob();
        job.setTenantId(tenantId);
        job.setModuleCode(moduleCode);
        job.setMode(mode);
        job.setStatus(MigrationJob.Status.RUNNING);
        // OrganizationNodeService exige un acteur non nul ; un appel système (outbox) reçoit un identifiant technique.
        job.setCreatedBy(actorId != null ? actorId : SYSTEM_ACTOR_ID);
        jobs.save(job);

        Map<String, Object> report = new LinkedHashMap<>();
        List<Map<String, Object>> rowReports = new ArrayList<>();

        try {
            switch (moduleCode) {
                case "PEOPLES" -> migratePeoples(tenantId, job, mode, rowReports);
                case "SPACES" -> migrateSpaces(tenantId, job, mode, rowReports);
                case "EVENTS" -> migrateEvents(tenantId, job, mode, rowReports);
                default -> throw new IllegalArgumentException("Module inconnu: " + moduleCode);
            }
        } catch (RuntimeException e) {
            log.error("[G4.6] migration {} ({}) interrompue tenant={}", moduleCode, mode, tenantId, e);
            job.setStatus(MigrationJob.Status.FAILED);
            report.put("error", e.getMessage());
            report.put("rows", rowReports);
            job.setReportJson(report);
            job.setCompletedAt(OffsetDateTime.now());
            jobs.save(job);
            throw e;
        }

        report.put("rows", rowReports);
        report.put("mode", mode.name());
        job.setReportJson(report);
        job.setStatus(MigrationJob.Status.COMPLETED);
        job.setCompletedAt(OffsetDateTime.now());
        jobs.save(job);

        if (mode == MigrationJob.Mode.MIGRATE) {
            outboxPublisher.publish(tenantId, "LegacyMigration", job.getId(), "LegacyMigrationCompleted",
                    Map.of("module", moduleCode,
                            "tenantId", tenantId.toString(),
                            "migrated", job.getRowsMigrated(),
                            "merged", job.getRowsMerged(),
                            "conflicts", job.getRowsConflicts(),
                            "skipped", job.getRowsSkipped()));
        }
        return job;
    }

    // ------------------------------------------------------------------
    // Module PEOPLES : souls → person + membership
    // ------------------------------------------------------------------

    private void migratePeoples(UUID tenantId, MigrationJob job, MigrationJob.Mode mode,
                                List<Map<String, Object>> rowReports) {
        if (!tableExists("souls")) {
            finishWithNote(job, "souls", "Source absente — rien à migrer");
            return;
        }
        List<Map<String, Object>> rows = readLegacy(tenantId, "souls",
                "id, tenant_id, nom, prenom, email, telephone, adresse, date_naissance, statut, date_integration");

        // Doublons internes à la source : deux âmes avec la même identité normalisée → conflit
        Map<String, String> seenEmailSource = new HashMap<>();
        Map<String, String> seenPhoneSource = new HashMap<>();

        for (Map<String, Object> row : rows) {
            String sourceId = str(row.get("id"));
            job.setRowsSeen(job.getRowsSeen() + 1);

            if (audits.existsByTenantIdAndSourceTableAndSourceIdAndStatusIn(tenantId, "souls", sourceId, DONE_STATUSES)) {
                job.setRowsSkipped(job.getRowsSkipped() + 1);
                rowReports.add(rowReport(sourceId, "SKIPPED", "déjà migré"));
                recordAudit(tenantId, job, "souls", sourceId, "person", null,
                        MigrationAudit.RowStatus.SKIPPED, "déjà migré");
                continue;
            }

            String email = normalizeEmail(str(row.get("email")));
            String phone = normalizePhone(str(row.get("telephone")));
            String lastName = str(row.get("nom"));
            String firstName = str(row.get("prenom"));

            UUID existingId = findExistingPersonId(tenantId, email, phone);

            String duplicateSource = email != null ? seenEmailSource.get(email)
                    : (phone != null ? seenPhoneSource.get(phone) : null);
            if (duplicateSource != null) {
                job.setRowsConflicts(job.getRowsConflicts() + 1);
                rowReports.add(rowReport(sourceId, "CONFLICT",
                        "doublon dans la source avec l'âme " + duplicateSource + " — fusion assistée requise (G3.1)"));
                if (mode == MigrationJob.Mode.MIGRATE) {
                    recordAudit(tenantId, job, "souls", sourceId, null, null,
                            MigrationAudit.RowStatus.CONFLICT, "doublon source " + duplicateSource);
                }
                continue;
            }

            if (email != null) seenEmailSource.put(email, sourceId);
            if (phone != null) seenPhoneSource.put(phone, sourceId);

            if (existingId != null) {
                // fusion : la fiche existe déjà → on trace MERGED, aucune écriture cible
                job.setRowsMerged(job.getRowsMerged() + 1);
                rowReports.add(rowReport(sourceId, "MERGED", "fiche personne existante " + existingId));
                if (mode == MigrationJob.Mode.MIGRATE) {
                    recordAudit(tenantId, job, "souls", sourceId, "person", existingId.toString(),
                            MigrationAudit.RowStatus.MERGED, "dédoublonnage à la création");
                }
                continue;
            }

            rowReports.add(rowReport(sourceId, "MIGRATED", firstName + " " + lastName));
            if (mode == MigrationJob.Mode.MIGRATE) {
                Person person = new Person();
                person.setTenantId(tenantId);
                person.setFirstName(firstName != null && !firstName.isBlank() ? firstName : lastName);
                person.setLastName(lastName);
                person.setDisplayName(((firstName == null ? "" : firstName + " ") + (lastName == null ? "" : lastName)).trim());
                person.setEmailNormalized(email);
                person.setPhoneNormalized(phone);
                person.setAddress(str(row.get("adresse")));
                person.setBirthDate(toLocalDate(row.get("date_naissance")));
                person.setStatus("ACTIVE");
                person.setVisibilityScope("CHURCH");
                person.setCreatedAt(OffsetDateTime.now());
                person.setUpdatedAt(OffsetDateTime.now());
                Person saved = personRepository.save(person);

                Membership membership = Membership.builder()
                        .tenantId(tenantId)
                        .personId(saved.getId())
                        .membershipStatus(mapSoulStatut(str(row.get("statut"))))
                        .joinedAt(toLocalDate(row.get("date_integration")) != null
                                ? toLocalDate(row.get("date_integration")) : LocalDate.now())
                        .source("IMPORT")
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build();
                membershipRepository.save(membership);

                job.setRowsMigrated(job.getRowsMigrated() + 1);
                MigrationAudit audit = recordAudit(tenantId, job, "souls", sourceId, "person",
                        saved.getId().toString(), MigrationAudit.RowStatus.MIGRATED, null);
                snapshot(tenantId, job, audit, "person", saved.getId().toString());
            }
        }
    }

    // ------------------------------------------------------------------
    // Module SPACES : departments/families → organization_nodes + spaces
    // ------------------------------------------------------------------

    private void migrateSpaces(UUID tenantId, MigrationJob job, MigrationJob.Mode mode,
                               List<Map<String, Object>> rowReports) {
        OrganizationNode root = organizationNodeService.getRoot(tenantId).orElse(null);
        if (root == null && mode == MigrationJob.Mode.MIGRATE) {
            root = organizationNodeService.createRootChurch(tenantId, "Église", "ROOT", job.getCreatedBy());
        }

        for (String sourceTable : List.of("departments", "families")) {
            if (!tableExists(sourceTable)) continue;
            SpaceType spaceType = "departments".equals(sourceTable) ? SpaceType.DEPARTMENT : SpaceType.FAMILY;
            OrganizationNodeType nodeType = "departments".equals(sourceTable)
                    ? OrganizationNodeType.DEPARTMENT : OrganizationNodeType.GROUP;
            String prefix = "departments".equals(sourceTable) ? "DEP" : "FAM";

            List<Map<String, Object>> rows = readLegacy(tenantId, sourceTable, "id, tenant_id, nom");
            for (Map<String, Object> row : rows) {
                String sourceId = str(row.get("id"));
                job.setRowsSeen(job.getRowsSeen() + 1);

                if (audits.existsByTenantIdAndSourceTableAndSourceIdAndStatusIn(tenantId, sourceTable, sourceId, DONE_STATUSES)) {
                    job.setRowsSkipped(job.getRowsSkipped() + 1);
                    rowReports.add(rowReport(sourceId, "SKIPPED", "déjà migré"));
                    recordAudit(tenantId, job, sourceTable, sourceId, "spaces", null,
                            MigrationAudit.RowStatus.SKIPPED, "déjà migré");
                    continue;
                }

                String name = str(row.get("nom"));
                String code = prefix + "-" + sourceId.substring(0, Math.min(8, sourceId.length()));
                Optional<Space> existingSpace = spaceRepository.findByTenantIdAndCode(tenantId, code);
                if (existingSpace.isPresent()
                        || audits.existsByTenantIdAndSourceTableAndSourceIdAndStatusIn(
                                tenantId, sourceTable, code, DONE_STATUSES)) {
                    job.setRowsMerged(job.getRowsMerged() + 1);
                    rowReports.add(rowReport(sourceId, "MERGED", "espace existant " + code));
                    if (mode == MigrationJob.Mode.MIGRATE) {
                        recordAudit(tenantId, job, sourceTable, sourceId, "spaces", code,
                                MigrationAudit.RowStatus.MERGED, "espace déjà présent");
                    }
                    continue;
                }

                rowReports.add(rowReport(sourceId, "MIGRATED", "espace " + spaceType + " « " + name + " »"));
                if (mode == MigrationJob.Mode.MIGRATE && root != null) {
                    OrganizationNode node = organizationNodeService.createNode(tenantId, nodeType,
                            name, code, root.getId(), null, job.getCreatedBy());

                    Space space = new Space();
                    space.setTenantId(tenantId);
                    space.setOrganizationUnitId(node.getId());
                    space.setSpaceType(spaceType);
                    space.setName(name);
                    space.setCode(code);
                    space.setStatus(SpaceStatus.ACTIVE);
                    space.setConfigurationJson(new LinkedHashMap<>(Map.of(
                            "migrated_from", sourceTable,
                            "migrated_source_id", sourceId)));
                    space.setCreatedAt(Instant.now());
                    space.setUpdatedAt(Instant.now());
                    Space saved = spaceRepository.save(space);

                    job.setRowsMigrated(job.getRowsMigrated() + 1);
                    MigrationAudit audit = recordAudit(tenantId, job, sourceTable, sourceId, "spaces",
                            saved.getId().toString(), MigrationAudit.RowStatus.MIGRATED, null);
                    snapshot(tenantId, job, audit, "spaces", saved.getId().toString());
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Module EVENTS : traçabilité de la migration V158 (legacy_events → event)
    // ------------------------------------------------------------------

    private void migrateEvents(UUID tenantId, MigrationJob job, MigrationJob.Mode mode,
                               List<Map<String, Object>> rowReports) {
        String source = tableExists("legacy_events") ? "legacy_events" : null;
        if (source == null) {
            if (!tableExists("events")) {
                finishWithNote(job, "events", "Aucune source events/legacy_events");
                return;
            }
            source = "events";
        }
        List<Map<String, Object>> rows = readLegacy(tenantId, source, "id, tenant_id");
        for (Map<String, Object> row : rows) {
            String sourceId = str(row.get("id"));
            job.setRowsSeen(job.getRowsSeen() + 1);
            if (audits.existsByTenantIdAndSourceTableAndSourceIdAndStatusIn(tenantId, source, sourceId, DONE_STATUSES)) {
                job.setRowsSkipped(job.getRowsSkipped() + 1);
                rowReports.add(rowReport(sourceId, "SKIPPED", "déjà audité"));
                continue;
            }
            boolean inTarget = targetEventExists(tenantId, sourceId);
            rowReports.add(rowReport(sourceId, inTarget ? "MERGED" : "CONFLICT",
                    inTarget ? "présent dans event (V158)" : "absent de la cible — à rejouer manuellement"));
            if (mode == MigrationJob.Mode.MIGRATE) {
                recordAudit(tenantId, job, source, sourceId, inTarget ? "event" : null, sourceId,
                        inTarget ? MigrationAudit.RowStatus.MERGED : MigrationAudit.RowStatus.CONFLICT,
                        inTarget ? "déjà migré par V158" : "introuvable dans event");
                if (inTarget) job.setRowsMerged(job.getRowsMerged() + 1);
                else job.setRowsConflicts(job.getRowsConflicts() + 1);
            }
        }
    }

    // ------------------------------------------------------------------
    // Jobs, rollback
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MigrationJob> listJobs(UUID tenantId) {
        return jobs.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public MigrationJob getJob(UUID tenantId, UUID jobId) {
        MigrationJob job = jobs.findByIdAndTenantId(jobId, tenantId);
        if (job == null) throw new NoSuchElementException("MigrationJob " + jobId);
        return job;
    }

    @Transactional(readOnly = true)
    public List<MigrationAudit> listAudit(UUID tenantId, UUID jobId) {
        return audits.findByTenantIdAndJobId(tenantId, jobId);
    }

    /**
     * Rollback d'un job MIGRATE : retire uniquement les lignes créées par ce job (snapshot),
     * dans l'ordre inverse. Un job clôturé depuis plus de {@value #ROLLBACK_WINDOW_DAYS} jours
     * est définitif (rollback refusé) — la source legacy, elle, n'a jamais été touchée.
     */
    public MigrationJob rollback(UUID tenantId, UUID jobId, UUID actorId) {
        MigrationJob original = getJob(tenantId, jobId);
        requireEnabled(tenantId);
        if (original.getMode() != MigrationJob.Mode.MIGRATE) {
            throw new IllegalStateException("Seul un job MIGRATE peut être annulé");
        }
        if (original.getCompletedAt() == null
                || original.getCompletedAt().isBefore(OffsetDateTime.now().minusDays(ROLLBACK_WINDOW_DAYS))) {
            throw new IllegalStateException("Job définitif (clôturé depuis plus de "
                    + ROLLBACK_WINDOW_DAYS + " jours) — rollback impossible");
        }

        MigrationJob rollbackJob = new MigrationJob();
        rollbackJob.setTenantId(tenantId);
        rollbackJob.setModuleCode(original.getModuleCode());
        rollbackJob.setMode(MigrationJob.Mode.ROLLBACK);
        rollbackJob.setStatus(MigrationJob.Status.RUNNING);
        rollbackJob.setCreatedBy(actorId);
        jobs.save(rollbackJob);

        List<MigrationSnapshot> snapshotsToRevert = snapshots.findByTenantIdAndJobIdOrderByIdDesc(tenantId, jobId);
        for (MigrationSnapshot snapshot : snapshotsToRevert) {
            UUID targetId = UUID.fromString(snapshot.getTargetId());
            switch (snapshot.getTargetTable()) {
                case "person" -> {
                    membershipRepository.findByTenantIdAndPersonId(tenantId, targetId)
                            .forEach(membershipRepository::delete);
                    personRepository.findByIdAndTenantId(targetId, tenantId)
                            .ifPresent(personRepository::delete);
                }
                case "spaces" -> spaceRepository.findById(targetId).ifPresent(space -> {
                    space.setDeletedAt(Instant.now());
                    space.setStatus(SpaceStatus.ARCHIVED);
                    space.setUpdatedAt(Instant.now());
                    spaceRepository.save(space);
                });
                default -> log.warn("[G4.6] rollback: table cible non gérée {}", snapshot.getTargetTable());
            }
            audits.findByTenantIdAndJobId(tenantId, jobId).stream()
                    .filter(a -> snapshot.getTargetTable().equals(a.getTargetTable())
                            && snapshot.getTargetId().equals(a.getTargetId()))
                    .forEach(audits::delete);
            rollbackJob.setRowsMigrated(rollbackJob.getRowsMigrated() + 1);
        }

        rollbackJob.setStatus(MigrationJob.Status.COMPLETED);
        rollbackJob.setCompletedAt(OffsetDateTime.now());
        rollbackJob.setReportJson(new LinkedHashMap<>(Map.of(
                "rolled_back_job", jobId.toString(),
                "reverted_rows", rollbackJob.getRowsMigrated())));
        jobs.save(rollbackJob);

        original.setStatus(MigrationJob.Status.CANCELLED);
        jobs.save(original);
        return rollbackJob;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void requireModule(String moduleCode) {
        if (moduleCode == null || MAPS.stream().noneMatch(m -> m.moduleCode().equalsIgnoreCase(moduleCode))) {
            throw new IllegalArgumentException("Module de migration inconnu: " + moduleCode);
        }
    }

    /** Le toggle §G1.2 {@code legacy_migration_enabled} conditionne TOUTE écriture de migration. */
    public boolean isEnabled(UUID tenantId) {
        return tenantSettingsRepository.findByTenantId(tenantId)
                .map(s -> Boolean.TRUE.equals(s.getLegacyMigrationEnabled()))
                .orElse(false);
    }

    private void requireEnabled(UUID tenantId) {
        if (!isEnabled(tenantId)) {
            throw new IllegalStateException(
                    "Migration legacy désactivée pour ce tenant (toggle legacy_migration_enabled)");
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = ?",
                Integer.class, table.toLowerCase());
        return count != null && count > 0;
    }

    /** Lecture scope tenant strict (§0.3 n°2-3) : le tenant est celui du job, jamais une donnée frontend. */
    private List<Map<String, Object>> readLegacy(UUID tenantId, String table, String columns) {
        if (tenantId == null) throw new SecurityException("Aucun tenant pour la migration");
        List<Map<String, Object>> rows =
                jdbc.queryForList("SELECT " + columns + " FROM " + table + " WHERE tenant_id = ?", tenantId);
        rows.forEach(row -> row.remove("tenant_id"));
        return rows;
    }

    private boolean targetEventExists(UUID tenantId, String eventId) {
        if (!tableExists("event")) return false;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM event WHERE tenant_id = ? AND id = CAST(? AS UUID)",
                Integer.class, tenantId, eventId);
        return count != null && count > 0;
    }

    private MigrationAudit recordAudit(UUID tenantId, MigrationJob job, String sourceTable, String sourceId,
                                       String targetTable, String targetId,
                                       MigrationAudit.RowStatus status, String message) {
        MigrationAudit audit = new MigrationAudit();
        audit.setTenantId(tenantId);
        audit.setJobId(job.getId());
        audit.setModuleCode(job.getModuleCode());
        audit.setSourceTable(sourceTable);
        audit.setSourceId(sourceId);
        audit.setTargetTable(targetTable);
        audit.setTargetId(targetId);
        audit.setStatus(status);
        audit.setMessage(message);
        return audits.save(audit);
    }

    private void snapshot(UUID tenantId, MigrationJob job, MigrationAudit audit,
                          String targetTable, String targetId) {
        MigrationSnapshot snapshot = new MigrationSnapshot();
        snapshot.setTenantId(tenantId);
        snapshot.setJobId(job.getId());
        snapshot.setAuditId(audit.getId());
        snapshot.setTargetTable(targetTable);
        snapshot.setTargetId(targetId);
        snapshot.setPayloadJson(Map.of("rolled_back", false));
        snapshots.save(snapshot);
    }

    private void finishWithNote(MigrationJob job, String source, String note) {
        Map<String, Object> report = job.getReportJson() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(job.getReportJson());
        report.put("note", source + " : " + note);
        job.setReportJson(report);
    }

    private Map<String, Object> rowReport(String sourceId, String status, String detail) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("sourceId", sourceId);
        entry.put("status", status);
        entry.put("detail", detail);
        return entry;
    }

    static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** Normalisation E.164-lite : digits seuls, préfixe international conservé. */
    static String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.isBlank() || digits.equals("+")) return null;
        return digits.startsWith("+") ? digits : (digits.startsWith("0") ? digits.substring(1) : digits);
    }

    static String mapSoulStatut(String statut) {
        if (statut == null) return "MEMBRE";
        return switch (statut.toUpperCase(Locale.ROOT)) {
            case "NOUVELLE_AME", "EN_INTEGRATION", "NOUVEAU_CONVERTI" -> "NOUVEAU_CONVERTI";
            case "VISITEUR" -> "VISITEUR";
            case "EN_VEILLE", "REFUS", "PERDU" -> "EN_VEILLE";
            case "DECROCHE" -> "DECROCHE";
            case "TRANSFERE" -> "TRANSFERE";
            case "DECES" -> "DECES";
            default -> "MEMBRE";
        };
    }

    /** Identité unique (§G3.1) : recherche scopée tenant par email puis téléphone normalisés. */
    private UUID findExistingPersonId(UUID tenantId, String email, String phone) {
        if (email != null) {
            Optional<Person> byEmail = personRepository
                    .findByTenantIdAndEmailNormalizedAndDeletedAtIsNull(tenantId, email);
            if (byEmail.isPresent()) return byEmail.get().getId();
        }
        if (phone != null) {
            Optional<Person> byPhone = personRepository
                    .findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(tenantId, phone);
            if (byPhone.isPresent()) return byPhone.get().getId();
        }
        return null;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof java.sql.Date sd) return sd.toLocalDate();
        if (value instanceof java.util.Date ud) return new java.sql.Timestamp(ud.getTime()).toLocalDateTime().toLocalDate();
        if (value instanceof LocalDateTime ldt) return ldt.toLocalDate();
        if (value instanceof OffsetDateTime odt) return odt.toLocalDate();
        String text = String.valueOf(value);
        return text.isBlank() ? null : LocalDate.parse(text.substring(0, Math.min(10, text.length())));
    }
}
