package com.discipolat.modules.dataMigration;

import com.discipolat.DiscipolatApplication;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.dataMigration.domain.LegacyMigrationService;
import com.discipolat.modules.dataMigration.domain.MigrationAudit;
import com.discipolat.modules.dataMigration.domain.MigrationJob;
import com.discipolat.modules.dataMigration.repository.MigrationAuditRepository;
import com.discipolat.modules.dataMigration.repository.MigrationJobRepository;
import com.discipolat.modules.dataMigration.repository.MigrationSnapshotRepository;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.people.domain.Person;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.spaces.domain.SpaceRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.domain.TenantSettingsRepository;
import com.discipolat.modules.tenants.domain.TenantStatus;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * G4.6 — Contrat du moteur de migration legacy.
 *
 * <p>Vérifie les garanties exigées avant commercialisation :</p>
 * <ol>
 *   <li>toggle {@code legacy_migration_enabled} (§G1.2) : sans activation, toute migration est refusée ;</li>
 *   <li>dry-run : rapport complet, <b>aucune</b> écriture dans les tables cibles ;</li>
 *   <li>migration : souls → person + membership, departments → spaces, tracées dans migration_audit ;</li>
 *   <li>replay idempotent : relancer ne crée aucun doublon (déduplication par source_id) ;</li>
 *   <li>rollback : ne retire que les lignes créées par le job, la source reste intacte ;</li>
 *   <li>isolation : les données d'un autre tenant ne migrent jamais dans le tenant actif.</li>
 * </ol>
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@Transactional
class LegacyMigrationEngineIntegrationTest {

    @Autowired private LegacyMigrationService service;
    @Autowired private MigrationJobRepository jobs;
    @Autowired private MigrationAuditRepository audits;
    @Autowired private MigrationSnapshotRepository snapshots;
    @Autowired private PersonRepository personRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private SoulRepository soulRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private TenantSettingsRepository tenantSettingsRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID actorId;
    private UUID otherTenantId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of("migration_snapshot", "migration_audit", "migration_job",
                "space_membership", "membership", "spaces", "person", "souls", "departments",
                "organization_nodes")) {
            if (tableExists(table)) {
                jdbcTemplate.execute("TRUNCATE TABLE " + table);
            }
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        tenantId = activeTenantId();
        otherTenantId = UUID.randomUUID();
        actorId = userRepository.save(User.builder()
                .tenantId(tenantId)
                .firstName("Migration")
                .lastName("Admin")
                .email("admin.migration-" + System.nanoTime() + "@test.local")
                .passwordHash("$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinvalidinvali")
                .phone("+67000" + System.nanoTime())
                .role(UserRole.ADMIN)
                .activeRole(UserRole.ADMIN)
                .statut(UserStatus.ACTIVE)
                .build()).getId();
        setLegacyMigrationEnabled(true);
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("G4.6 — toggle legacy_migration_enabled désactivé : la migration est refusée")
    void migrateRefusedWhenToggleDisabled() {
        seedSoul("Mokoko", "Esther", "esther@test.local", "+67077000001");
        setLegacyMigrationEnabled(false);

        assertThat(service.isEnabled(tenantId)).isFalse();
        assertThatThrownBy(() -> service.migrate(tenantId, actorId, "PEOPLES"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("legacy_migration_enabled");

        assertThat(personRepository.findByTenantIdAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(tenantId))
                .isEmpty();
    }

    @Test
    @DisplayName("G4.6 — dry-run produit un rapport sans écrire dans les tables cibles")
    void dryRunProducesReportWithoutWrites() {
        seedSoul("Nkodo", "Paul", "paul@test.local", "+67077000002");
        seedSoul("Nkodo", "Paul", "paul@test.local", "+67077000002"); // doublon de la source
        seedDepartment("Audiovisuel");

        MigrationJob dryRun = service.dryRun(tenantId, actorId, "PEOPLES");

        assertThat(dryRun.getMode()).isEqualTo(MigrationJob.Mode.DRY_RUN);
        assertThat(dryRun.getStatus()).isEqualTo(MigrationJob.Status.COMPLETED);
        assertThat(dryRun.getRowsSeen()).isEqualTo(2);
        // aucune écriture cible, aucune trace d'audit en mode simulation
        assertThat(personRepository.findByTenantIdAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(tenantId))
                .isEmpty();
        assertThat(audits.findByTenantIdAndJobId(tenantId, dryRun.getId())).isEmpty();
        assertThat(snapshots.findByTenantIdAndJobIdOrderByIdDesc(tenantId, dryRun.getId())).isEmpty();
        assertThat(dryRun.getReportJson()).containsKey("rows");
    }

    @Test
    @DisplayName("G4.6 — migration souls → person + membership, tracée ligne à ligne")
    void migratePeoplesWritesTargetAndAudit() {
        seedSoul("Essomba", "Rachelle", "rachelle@test.local", "+67077000003");
        seedSoul("Manga", "Yves", "yves@test.local", "+67077000004");

        MigrationJob job = service.migrate(tenantId, actorId, "PEOPLES");

        assertThat(job.getRowsMigrated()).isEqualTo(2);
        List<Person> people = personRepository
                .findByTenantIdAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(tenantId);
        assertThat(people).hasSize(2);
        assertThat(people).allSatisfy(p -> assertThat(p.getTenantId()).isEqualTo(tenantId));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM membership WHERE tenant_id = ?",
                Integer.class, tenantId)).isEqualTo(2);

        List<MigrationAudit> traces = audits.findByTenantIdAndJobId(tenantId, job.getId());
        assertThat(traces).hasSize(2)
                .allSatisfy(a -> {
                    assertThat(a.getStatus()).isEqualTo(MigrationAudit.RowStatus.MIGRATED);
                    assertThat(a.getSourceTable()).isEqualTo("souls");
                    assertThat(a.getTargetTable()).isEqualTo("person");
                });
    }

    @Test
    @DisplayName("G4.6 — replay idempotent : relancer la migration ne crée aucun doublon")
    void replayIsIdempotent() {
        seedSoul("Bidjeck", "Armel", "armel@test.local", "+67077000005");

        MigrationJob first = service.migrate(tenantId, actorId, "PEOPLES");
        MigrationJob second = service.migrate(tenantId, actorId, "PEOPLES");

        assertThat(first.getRowsMigrated()).isEqualTo(1);
        assertThat(second.getRowsMigrated()).isZero();
        assertThat(second.getRowsSkipped()).isEqualTo(1);
        assertThat(personRepository.findByTenantIdAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(tenantId))
                .hasSize(1);
    }

    @Test
    @DisplayName("G4.6 — doublon interne à la source : conflit détecté, aucune écriture silencieuse")
    void duplicateInsideSourceIsConflict() {
        seedSoul("Tchoumi", "Rose", "rose@test.local", "+67077000006");
        seedSoul("Tchoumi", "Rose", "rose@test.local", "+67077000006");

        MigrationJob job = service.migrate(tenantId, actorId, "PEOPLES");

        assertThat(job.getRowsMigrated()).isEqualTo(1);
        assertThat(job.getRowsConflicts()).isEqualTo(1);
        assertThat(audits.findByTenantIdAndJobId(tenantId, job.getId()))
                .anySatisfy(a -> assertThat(a.getStatus()).isEqualTo(MigrationAudit.RowStatus.CONFLICT));
    }

    @Test
    @DisplayName("G4.6 — rollback : retire uniquement les lignes créées, la source legacy est préservée")
    void rollbackRevertsOnlyCreatedRows() {
        seedSoul("Nkoa", "Sandrine", "sandrine@test.local", "+67077000007");
        seedSoul("Ngo Bassog", "Michel", "michel@test.local", "+67077000008");

        MigrationJob job = service.migrate(tenantId, actorId, "PEOPLES");
        assertThat(personRepository.findByTenantIdAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(tenantId))
                .hasSize(2);

        MigrationJob rollback = service.rollback(tenantId, job.getId(), actorId);

        assertThat(rollback.getMode()).isEqualTo(MigrationJob.Mode.ROLLBACK);
        assertThat(rollback.getRowsMigrated()).isEqualTo(2);
        assertThat(personRepository.findByTenantIdAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(tenantId))
                .isEmpty();
        // règle absolue §0.3 n°6 / G4.6 : aucune donnée source n'est effacée
        assertThat(soulRepository.count()).isEqualTo(2);
        // le replay redevient possible : la trace qui verrouillait l'idempotence a été retirée
        assertThat(audits.findByTenantIdAndJobId(tenantId, job.getId())).isEmpty();
        assertThat(jobs.findByIdAndTenantId(job.getId(), tenantId).getStatus())
                .isEqualTo(MigrationJob.Status.CANCELLED);
    }

    @Test
    @DisplayName("G4.6 — job clôturé depuis plus de 30 jours : définitif, rollback refusé")
    void rollbackRefusedAfterWindow() {
        seedSoul("Ateba", "Lise", "lise@test.local", "+67077000009");
        MigrationJob job = service.migrate(tenantId, actorId, "PEOPLES");
        job.setCompletedAt(job.getCompletedAt().minusDays(31));
        jobs.save(job);

        assertThatThrownBy(() -> service.rollback(tenantId, job.getId(), actorId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("définitif");
    }

    @Test
    @DisplayName("G4.6 — isolation : les âmes d'un autre tenant ne migrent jamais dans le tenant actif")
    void migrationIsTenantScoped() {
        seedSoulInto(otherTenantId, "Autre Eglise", "Stranger", "stranger@other.test", "+67079000000");
        seedSoul("Eyenga", "Clarisse", "clarisse@test.local", "+67077000010");

        MigrationJob job = service.migrate(tenantId, actorId, "PEOPLES");

        assertThat(job.getRowsSeen()).isEqualTo(1);
        assertThat(personRepository.findByTenantIdAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(tenantId))
                .hasSize(1);
        assertThat(personRepository.findByTenantIdAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(otherTenantId))
                .isEmpty();
    }

    @Test
    @DisplayName("G4.6 — migration departments → spaces (espaces configurables) + mapping publié")
    void migrateSpacesCreatesConfigurableSpaces() {
        seedDepartment("Chorale");
        seedDepartment("Protocole");

        MigrationJob job = service.migrate(tenantId, actorId, "SPACES");

        assertThat(job.getRowsMigrated()).isEqualTo(2);
        assertThat(spaceRepository.findByTenantIdAndDeletedAtIsNull(tenantId)).hasSize(2);
        assertThat(spaceRepository.findByTenantIdAndDeletedAtIsNull(tenantId))
                .allSatisfy(s -> assertThat(s.getTenantId()).isEqualTo(tenantId));

        List<LegacyMigrationService.LegacyMap> maps = service.listMaps();
        assertThat(maps).extracting(LegacyMigrationService.LegacyMap::moduleCode)
                .contains("PEOPLES", "SPACES", "EVENTS");
        assertThat(maps.stream().filter(m -> m.moduleCode().equals("SPACES")).findFirst().orElseThrow()
                .unmappableFields()).isNotEmpty();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private UUID activeTenantId() {
        Optional<Tenant> existing = tenantRepository.findFirstByStatusOrderByCreatedAtAsc(TenantStatus.ACTIVE);
        Tenant tenant = existing.orElseGet(() -> tenantRepository.save(Tenant.builder()
                .name("Église de test migration")
                .slug("eglise-test-migration-" + System.nanoTime())
                .status(TenantStatus.ACTIVE)
                .build()));
        return tenant.getId();
    }

    private void setLegacyMigrationEnabled(boolean enabled) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        TenantSettings settings = tenantSettingsRepository.findByTenantId(tenantId)
                .orElseGet(() -> TenantSettings.builder().tenant(tenant).build());
        settings.setLegacyMigrationEnabled(enabled);
        tenantSettingsRepository.save(settings);
    }

    private void seedSoul(String nom, String prenom, String email, String telephone) {
        seedSoulInto(tenantId, nom, prenom, email, telephone);
    }

    private void seedSoulInto(UUID tenant, String nom, String prenom, String email, String telephone) {
        UUID previous = TenantContext.getTenantId();
        TenantContext.setTenantId(tenant);
        try {
            // saveAndFlush : la migration lit les sources via JdbcTemplate ; les lignes doivent
            // être physiquement présentes dans la transaction avant la lecture SQL du service.
            soulRepository.saveAndFlush(Soul.builder()
                    .tenantId(tenant)
                    .nom(nom)
                    .prenom(prenom)
                    .email(email)
                    .telephone(telephone)
                    .typeDisciple(TypeDisciple.NOUVEAU_CONVERTI)
                    .statut(StatutAme.EN_INTEGRATION)
                    .dateIntegration(LocalDate.now().minusMonths(3))
                    .faiseurId(actorId)
                    .etatSpirituel("NOUVEAU_CONVERTI")
                    .niveauCroissance(1)
                    .build());
        } finally {
            if (previous != null) TenantContext.setTenantId(previous); else TenantContext.clear();
        }
    }

    private void seedDepartment(String nom) {
        UUID previous = TenantContext.getTenantId();
        TenantContext.setTenantId(tenantId);
        try {
            departmentRepository.saveAndFlush(Department.builder()
                    .tenantId(tenantId)
                    .nom(nom)
                    .responsableId(actorId)
                    .build());
        } finally {
            if (previous != null) TenantContext.setTenantId(previous); else TenantContext.clear();
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = ?",
                Integer.class, table);
        return count != null && count > 0;
    }
}
