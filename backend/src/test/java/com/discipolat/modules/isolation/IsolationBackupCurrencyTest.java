package com.discipolat.modules.isolation;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.exception.ResourceNotFoundException;
import com.discipolat.modules.backup.domain.BackupDescriptor;
import com.discipolat.modules.backup.domain.BackupServiceImpl;
import com.discipolat.modules.backup.domain.BackupStatus;
import com.discipolat.modules.backup.domain.VerificationResult;
import com.discipolat.modules.backup.infrastructure.BackupDescriptorRepository;
import com.discipolat.modules.backup.infrastructure.BackupStorageService;
import com.discipolat.modules.currency.domain.CurrencyConfig;
import com.discipolat.modules.currency.domain.CurrencyService;
import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import com.discipolat.modules.finances.api.FinanceTransactionRequest;
import com.discipolat.modules.finances.domain.FinanceBudgetRepository;
import com.discipolat.modules.finances.domain.FinanceService;
import com.discipolat.modules.finances.domain.FinanceTransaction;
import com.discipolat.modules.finances.domain.FinanceTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A3 (M7/M9) — Test d'ISOLATION transversal : la sauvegarde d'un tenant est
 * invisible et intouchable depuis un autre tenant (404, jamais 403 — on ne
 * confirme pas l'existence), et chaque tenant persiste ses transactions dans
 * SA devise avec l'unité mineure ISO-4217 exacte.
 *
 * <p>Ce test regroupe les deux promesses « multi-tenant mondiale » parce
 * qu'elles partagent le même contrat : une donnée étiquetée tenant ne doit
 * JAMAIS se mélanger, qu'elle soit un fichier d'archive ou une écriture
 * comptable. Pile JDBC mockée + vrai dossier temporaire, comme
 * {@code BackupServiceTest} : on teste le service, pas PostgreSQL.</p>
 */
class IsolationBackupCurrencyTest {

    private static final UUID TENANT_A = UUID.fromString("aaaaaaaa-1111-4000-8000-00000000000a");
    private static final UUID TENANT_B = UUID.fromString("bbbbbbbb-2222-4000-8000-00000000000b");
    private static final UUID ACTOR = UUID.fromString("cccccccc-3333-4000-8000-00000000000c");
    private static final UUID BACKUP_ID = UUID.fromString("dddddddd-4444-4000-8000-00000000000d");
    private static final String ARCHIVE_NAME = "discipolat_isolation_test.sql.gz";

    @Nested
    @DisplayName("Isolation des sauvegardes")
    class Sauvegardes {

        private Path root;
        private BackupStorageService storage;
        private BackupDescriptorRepository repository;
        private BackupServiceImpl service;
        private BackupDescriptor descriptorA;
        private Path archiveA;

        @BeforeEach
        void setUp() throws IOException {
            root = Files.createTempDirectory("isolation-backup-test");
            storage = new BackupStorageService(root.toString());
            repository = mock(BackupDescriptorRepository.class);
            DataSource dataSource = mock(DataSource.class);
            service = new BackupServiceImpl(dataSource, storage, repository, 30);

            // Archive RÉELLE du tenant A sur le disque, empreinte honnête.
            BackupStorageService.WrittenArchive written = storage.writeArchive(
                    TENANT_A, ARCHIVE_NAME,
                    writer -> writer.write("-- sauvegarde du tenant " + TENANT_A + "\n"));
            archiveA = written.path();
            descriptorA = new BackupDescriptor(BACKUP_ID, TENANT_A, ARCHIVE_NAME,
                    written.sizeBytes(), written.sha256(), Instant.now(), ACTOR, BackupStatus.COMPLETED);

            when(repository.findById(BACKUP_ID)).thenReturn(Optional.of(descriptorA));
            when(repository.findAllByTenant(TENANT_B)).thenReturn(List.of());
        }

        @AfterEach
        void tearDown() throws IOException {
            try (var walk = Files.walk(root)) {
                for (Path p : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }

        @Test
        @DisplayName("depuis le tenant B, la sauvegarde de A est invisible : find et openArchive vides")
        void sauvegardeDautruiInvisible() {
            assertThat(service.find(BACKUP_ID, TENANT_B)).isEmpty();
            assertThat(service.openArchive(BACKUP_ID, TENANT_B)).isEmpty();
            assertThat(service.list(TENANT_B)).isEmpty();
            // Le tenant A, lui, voit bien la sienne.
            assertThat(service.find(BACKUP_ID, TENANT_A)).contains(descriptorA);
        }

        @Test
        @DisplayName("verify/delete depuis un autre tenant = 404 (pas 403 : on ne confirme pas l'existence)")
        void operationsDestructivesRefuseesSansFuite() {
            assertThat(catchThrowableOfType(() -> service.verify(BACKUP_ID, TENANT_B),
                    ResourceNotFoundException.class)).isNotNull();
            assertThat(catchThrowableOfType(() -> service.delete(BACKUP_ID, TENANT_B),
                    ResourceNotFoundException.class)).isNotNull();

            // Rien n'a bougé : ni le descripteur, ni le fichier de l'autre tenant.
            verify(repository, never()).deleteById(any());
            assertThat(Files.exists(archiveA)).isTrue();
        }

        @Test
        @DisplayName("rétention d'un tenant ne purge jamais les archives d'un autre")
        void purgeLimiteAuTenantCourant() {
            assertThat(service.purgeExpired(TENANT_B)).isZero();
            assertThat(Files.exists(archiveA)).as("l'archive de A est intacte").isTrue();
        }

        @Test
        @DisplayName("contrôle positif : le propriétaire vérifie bien son archive réelle (relecture disque)")
        void proprietaireVerifieSonArchive() {
            VerificationResult result = service.verify(BACKUP_ID, TENANT_A);

            assertThat(result.valid()).isTrue();
            assertThat(result.tenantId()).isEqualTo(TENANT_A);
            assertThat(result.actualSha256()).isEqualTo(descriptorA.sha256());
            verify(repository).updateStatus(BACKUP_ID, BackupStatus.VERIFIED);
        }
    }

    @Nested
    @DisplayName("Devise par tenant (écriture comptable)")
    class MonnaieParTenant {

        private FinanceTransactionRepository transactionRepository;
        private CurrencyService currencyService;
        private FinanceService service;
        /** La « devise primaire » renvoyée par le dépôt, telle que résolue par le TenantContext réel. */
        private CurrencyConfig primary;

        @BeforeEach
        void setUp() {
            transactionRepository = mock(FinanceTransactionRepository.class);
            currencyService = mock(CurrencyService.class);
            when(transactionRepository.save(any(FinanceTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            // thenAnswer (et pas thenReturn séquentiel) : toMap relit la devise
            // primaire à chaque rendu — c'est le tenant courant qui répond.
            when(currencyService.getPrimaryCurrency()).thenAnswer(invocation -> primary);
            service = new FinanceService(transactionRepository, mock(FinanceBudgetRepository.class),
                    mock(com.discipolat.common.infrastructure.security.SecurityUtils.class),
                    mock(com.discipolat.modules.audit.domain.AuditService.class),
                    mock(com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher.class),
                    currencyService, new Iso4217CurrencyValidator(),
                    mock(com.discipolat.modules.finances.domain.FinanceAccountRepository.class),
                    mock(com.discipolat.modules.finances.domain.FinanceDonationRepository.class),
                    mock(com.discipolat.modules.finances.domain.FinanceTontineRepository.class),
                    mock(com.discipolat.modules.finances.domain.FinanceTontineMemberRepository.class),
                    mock(com.discipolat.modules.finances.domain.FinanceTontinePayoutRepository.class));
        }

        private static CurrencyConfig currency(String code, String symbol) {
            CurrencyConfig config = new CurrencyConfig();
            config.setCurrencyCode(code);
            config.setCurrencySymbol(symbol);
            config.setTimezone("Europe/Paris");
            return config;
        }

        private FinanceTransaction captureSaved() {
            ArgumentCaptor<FinanceTransaction> captor = ArgumentCaptor.forClass(FinanceTransaction.class);
            verify(transactionRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
            return captor.getValue();
        }

        @Test
        @DisplayName("tenant à EUR : 12.34 € persiste devise=EUR et 1234 unités mineures")
        void tenantEuroPersisteEnEuro() {
            primary = currency("EUR", "€");
            service.createTransaction(new FinanceTransactionRequest(
                    FinanceTransaction.TransactionType.RECETTE, "dime",
                    new BigDecimal("12.34"), "Premiers fruits", LocalDate.of(2026, 9, 1)));

            FinanceTransaction saved = captureSaved();
            assertThat(saved.getDevise()).isEqualTo("EUR");
            assertThat(saved.getMontantMinor()).isEqualTo(1234L);
        }

        @Test
        @DisplayName("tenant à XAF : 15 000 FCFA persiste sans virgule (0 unité mineure ISO)")
        void tenantFrancCfaPersisteSansDecimale() {
            primary = currency("XAF", "FCFA");
            service.createTransaction(new FinanceTransactionRequest(
                    FinanceTransaction.TransactionType.RECETTE, "offrande",
                    new BigDecimal("15000"), "Offrande", LocalDate.of(2026, 9, 6)));

            FinanceTransaction saved = captureSaved();
            assertThat(saved.getDevise()).isEqualTo("XAF");
            assertThat(saved.getMontantMinor()).isEqualTo(15000L);
        }

        @Test
        @DisplayName("isolation monétaire : la même saisie change d'unité mineure selon le tenant")
        void unMontantDeuxDevises() {
            primary = currency("USD", "$");
            service.createTransaction(new FinanceTransactionRequest(
                    FinanceTransaction.TransactionType.DEPENSE, "loyer",
                    new BigDecimal("250"), null, LocalDate.of(2026, 9, 1)));
            assertThat(captureSaved().getMontantMinor()).isEqualTo(25000L);

            primary = currency("JPY", "¥");
            service.createTransaction(new FinanceTransactionRequest(
                    FinanceTransaction.TransactionType.DEPENSE, "loyer",
                    new BigDecimal("250"), null, LocalDate.of(2026, 9, 1)));
            assertThat(captureSaved().getMontantMinor())
                    .as("250 ¥ = 250 unités mineures, 250 $ = 25 000 : jamais de confusion")
                    .isEqualTo(250L);
        }

        @Test
        @DisplayName("saisie incompatible avec la devise du tenant = refus AVANT écriture")
        void decimalesIllégalesRefuseesAvantPersistance() {
            primary = currency("XOF", "FCFA");

            DomainException refused = catchThrowableOfType(() -> service.createTransaction(
                    new FinanceTransactionRequest(FinanceTransaction.TransactionType.RECETTE, "dime",
                            new BigDecimal("1500.75"), null, LocalDate.of(2026, 9, 1))), DomainException.class);

            assertThat(refused.toProblemDetail().getTitle()).isEqualTo("CURRENCY_DECIMALS_INVALID");
            verify(transactionRepository, never()).save(any(FinanceTransaction.class));
        }
    }
}
