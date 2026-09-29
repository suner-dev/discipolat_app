package com.discipolat.modules.backup.domain;

import com.discipolat.common.exception.ResourceNotFoundException;
import com.discipolat.modules.backup.infrastructure.BackupDescriptorRepository;
import com.discipolat.modules.backup.infrastructure.BackupStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du {@link BackupServiceImpl} SANS PostgreSQL : la pile JDBC est
 * entièrement mockée et le stockage est un vrai dossier temporaire.
 *
 * <p>Ce qui est réellement vérifié ici, ce sont les promesses du module :
 * création d'une archive sur le disque, empreinte calculée sur les octets
 * écrits, et surtout une {@code verify} qui RELIT le fichier — les tests
 * corrompent volontairement l'archive pour prouver qu'elle est détectée.
 */
class BackupServiceTest {

    private static final UUID TENANT_A = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000002");
    private static final UUID ACTOR = UUID.fromString("cccccccc-0000-4000-8000-000000000003");

    private static final String NOM_ARCHIVE = "discipolat_tenant-1a2b3c4d_20260101_120000_abcd1234.sql.gz";

    private Path root;
    private BackupStorageService storage;
    private BackupDescriptorRepository repository;
    private DataSource dataSource;
    private BackupServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        this.root = Files.createTempDirectory("backup-service-test");
        this.storage = new BackupStorageService(this.root.toString());
        this.repository = mock(BackupDescriptorRepository.class);
        this.dataSource = mock(DataSource.class);
        this.service = new BackupServiceImpl(this.dataSource, this.storage, this.repository, 30);
    }

    @AfterEach
    void tearDown() throws Exception {
        deleteRecursivement(this.root);
    }

    // ==================================================================
    // Création
    // ==================================================================

    @Test
    @DisplayName("create écrit une archive et renvoie un descripteur COMPLETED avec son SHA-256")
    void createProduitUnDescripteurComplet() throws Exception {
        tenantAvecUneTableMultiTenante();

        BackupDescriptor descriptor = service.create(TENANT_A, ACTOR);

        assertNotNull(descriptor.id());
        assertEquals(TENANT_A, descriptor.tenantId());
        assertEquals(ACTOR, descriptor.createdBy());
        assertEquals(BackupStatus.COMPLETED, descriptor.status());
        assertNotNull(descriptor.createdAt());

        // L'archive existe réellement, dans le dossier du tenant
        Path archive = storage.resolveArchive(TENANT_A, descriptor.fileName());
        assertTrue(Files.isRegularFile(archive), "L'archive doit exister sur le disque");
        assertEquals(Files.size(archive), descriptor.sizeBytes());
        assertTrue(descriptor.sizeBytes() > 0, "Une archive vide ne doit pas être annoncée comme valide");

        // L'empreinte enregistrée est celle des octets RÉELLEMENT écrits :
        // c'est exactement ce que verify() relira.
        assertEquals(independentSha256(archive), descriptor.sha256());
        assertTrue(descriptor.sha256().matches("[0-9a-f]{64}"),
                "SHA-256 hexadécimal attendu, obtenu: " + descriptor.sha256());
    }

    @Test
    @DisplayName("create archive le contenu SQL du tenant et EXCLUT les tables sans tenant_id")
    void createArchiveLeContenuDuTenant() throws Exception {
        tenantAvecUneTableMultiTenanteEtUneTableGlobale();

        BackupDescriptor descriptor = service.create(TENANT_A, ACTOR);
        String dump = unzip(storage.resolveArchive(TENANT_A, descriptor.fileName()));

        assertTrue(dump.contains("\"souls\""), "La table du tenant doit figurer dans le dump");
        assertTrue(dump.contains("Ame de test"), "Les lignes exportées doivent figurer dans le dump");
        assertFalse(dump.contains("app_config"),
                "Une table sans tenant_id ne doit pas être incluse dans une sauvegarde de tenant");
        assertFalse(dump.contains("clef-de-configuration"),
                "Une donnée globale ne doit pas fuiter dans une sauvegarde de tenant");
    }

    @Test
    @DisplayName("create lie le tenant au SELECT au lieu de le concatener dans le SQL")
    void createFiltreSurLeTenant() throws SQLException {
        CatalogueJdbc catalogue = tableVideDUneColonne();

        service.create(TENANT_A, ACTOR);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(catalogue.connection()).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("\"tenant_id\" = ?"),
                "Le filtre tenant doit être un paramètre lié, obtenu: " + sql.getValue());
        verify(catalogue.statement()).setObject(eq(1), eq(TENANT_A));
    }

    @Test
    @DisplayName("create d'un tenant VIDE réussit : archive valide, zéro table, zéro ligne")
    void createTenantVideReussit() throws Exception {
        aucunTableDansLeCatalogue();

        BackupResult result = service.createWithReport(TENANT_A, ACTOR);

        assertEquals(BackupStatus.COMPLETED, result.descriptor().status());
        assertEquals(0, result.tablesExported());
        assertEquals(0L, result.rowsExported());
        assertTrue(result.durationMs() >= 0);

        Path archive = storage.resolveArchive(TENANT_A, result.descriptor().fileName());
        assertTrue(Files.isRegularFile(archive));
        assertTrue(result.descriptor().sizeBytes() > 0,
                "L'archive d'un tenant vide doit quand même être un fichier gzip valide");
        assertEquals(independentSha256(archive), result.descriptor().sha256());
    }

    @Test
    @DisplayName("create refuse un tenant nul")
    void createRefuseTenantNul() {
        assertThrows(IllegalArgumentException.class, () -> service.create(null, ACTOR));
        assertThrows(IllegalArgumentException.class, () -> service.createWithReport(null, ACTOR));
    }

    @Test
    @DisplayName("create marque la sauvegarde en FAILED et la retire du disque si l'écriture échoue")
    void createEchoueProprement() throws SQLException {
        // Pas de catalogue disponible : l'export lève, la sauvegarde doit être
        // marquée FAILED et ne pas laisser d'archive orpheline.
        when(dataSource.getConnection()).thenThrow(new IllegalStateException("pool fermé"));

        assertThrows(IllegalStateException.class, () -> service.create(TENANT_A, ACTOR));

        verify(repository, atLeastOnce())
                .save(argThat(descriptor -> descriptor.status() == BackupStatus.FAILED));
    }

    // ==================================================================
    // Vérification — le cœur de la promesse
    // ==================================================================

    @Nested
    @DisplayName("Vérification d'intégrité")
    class Verification {

        @Test
        @DisplayName("verify réussit sur une archive intacte et pose le statut VERIFIED")
        void verifyReussitSurArchiveIntacte() throws Exception {
            tenantAvecUneTableMultiTenante();
            BackupDescriptor created = service.create(TENANT_A, ACTOR);
            givenStored(created);

            VerificationResult result = service.verify(created.id(), TENANT_A);

            assertTrue(result.valid(), "Une archive intacte doit être considérée comme valide");
            assertEquals(created.sha256(), result.actualSha256());
            assertEquals(created.sizeBytes(), result.sizeBytes());
            assertNotNull(result.checkedAt());
            assertNotNull(result.message());
            verify(repository).updateStatus(created.id(), BackupStatus.VERIFIED);
        }

        @Test
        @DisplayName("verify DÉTECTE une archive corrompue et pose le statut FAILED")
        void verifyDetecteArchiveCorrompue() throws Exception {
            tenantAvecUneTableMultiTenante();
            BackupDescriptor created = service.create(TENANT_A, ACTOR);
            givenStored(created);

            // Corruption réelle du fichier, comme un disque vieillissant ou un
            // déploiement tronqué : on AJOUTE des octets dans l'archive.
            Path archive = storage.resolveArchive(TENANT_A, created.fileName());
            Files.write(archive, "CORROMPU".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);

            VerificationResult result = service.verify(created.id(), TENANT_A);

            assertFalse(result.valid(), "Une archive modifiée doit être détectée comme corrompue");
            assertEquals(created.sha256(), result.expectedSha256());
            assertNotEquals(created.sha256(), result.actualSha256(),
                    "L'empreinte recalculée doit différer de l'empreinte enregistrée");
            verify(repository).updateStatus(created.id(), BackupStatus.FAILED);
        }

        @Test
        @DisplayName("verify DÉTECTE une archive vidée de son contenu")
        void verifyDetecteArchiveVidee() throws Exception {
            tenantAvecUneTableMultiTenante();
            BackupDescriptor created = service.create(TENANT_A, ACTOR);
            givenStored(created);

            Files.write(storage.resolveArchive(TENANT_A, created.fileName()), new byte[0]);

            VerificationResult result = service.verify(created.id(), TENANT_A);

            assertFalse(result.valid());
            assertTrue(result.message().toLowerCase().contains("corrompue"),
                    "Message attendu décrivant la corruption, obtenu: " + result.message());
        }

        @Test
        @DisplayName("verify signale une archive absente du disque")
        void verifySignaleArchiveAbsente() throws Exception {
            tenantAvecUneTableMultiTenante();
            BackupDescriptor created = service.create(TENANT_A, ACTOR);
            givenStored(created);

            Files.delete(storage.resolveArchive(TENANT_A, created.fileName()));

            VerificationResult result = service.verify(created.id(), TENANT_A);

            assertFalse(result.valid());
            assertEquals(-1L, result.sizeBytes());
            assertTrue(result.message().toLowerCase().contains("introuvable"),
                    "Message attendu décrivant l'absence, obtenu: " + result.message());
            verify(repository).updateStatus(created.id(), BackupStatus.FAILED);
        }

        @Test
        @DisplayName("verify est un 404 pour une sauvegarde d'un AUTRE tenant et ne modifie aucun statut")
        void verifyRefuseSauvegardeDUnAutreTenant() {
            BackupDescriptor created = descriptor(TENANT_A, NOM_ARCHIVE);
            givenStored(created);

            // Fail-closed : répondre 403 confirmerait à l'appelant que cet
            // identifiant existe.
            assertThrows(ResourceNotFoundException.class, () -> service.verify(created.id(), TENANT_B));
            verify(repository, never()).updateStatus(any(), any());
        }
    }

    // ==================================================================
    // Isolation tenant
    // ==================================================================

    @Nested
    @DisplayName("Isolation tenant")
    class Isolation {

        @Test
        @DisplayName("list ne retourne QUE les sauvegardes du tenant demandé")
        void listNeRetourneQueSonTenant() {
            BackupDescriptor mien = descriptor(TENANT_A, "mien.sql.gz");
            BackupDescriptor celuiDUnAutre = descriptor(TENANT_B, "autre-tenant.sql.gz");
            // Le dépôt est ici la source de fuite possible : le service doit
            // tout de même filtrer, sans faire confiance à une seule couche.
            when(repository.findAllByTenant(TENANT_A)).thenReturn(List.of(mien, celuiDUnAutre));

            List<BackupDescriptor> resultats = service.list(TENANT_A);

            assertEquals(1, resultats.size());
            assertEquals("mien.sql.gz", resultats.get(0).fileName());
            assertEquals(TENANT_A, resultats.get(0).tenantId());
        }

        @Test
        @DisplayName("list renvoie les sauvegardes de la plus récente à la plus ancienne")
        void listTrieParDateDecroissante() {
            BackupDescriptor ancien = new BackupDescriptor(UUID.randomUUID(), TENANT_A, "ancien.sql.gz", 1L, "a",
                    Instant.now().minus(Duration.ofDays(10)), ACTOR, BackupStatus.COMPLETED);
            BackupDescriptor recent = new BackupDescriptor(UUID.randomUUID(), TENANT_A, "recent.sql.gz", 1L, "b",
                    Instant.now(), ACTOR, BackupStatus.COMPLETED);
            when(repository.findAllByTenant(TENANT_A)).thenReturn(List.of(ancien, recent));

            List<BackupDescriptor> resultats = service.list(TENANT_A);

            assertEquals(List.of("recent.sql.gz", "ancien.sql.gz"),
                    resultats.stream().map(BackupDescriptor::fileName).toList());
        }

        @Test
        @DisplayName("find ne retourne RIEN pour une sauvegarde d'un autre tenant")
        void findMasqueLesSauvegardesDUnAutreTenant() {
            BackupDescriptor celuiDUnAutre = descriptor(TENANT_B, "autre-tenant.sql.gz");
            when(repository.findById(celuiDUnAutre.id())).thenReturn(Optional.of(celuiDUnAutre));

            assertTrue(service.find(celuiDUnAutre.id(), TENANT_A).isEmpty());
            assertTrue(service.find(celuiDUnAutre.id(), TENANT_B).isPresent());
        }

        @Test
        @DisplayName("delete est un 404 pour une sauvegarde d'un AUTRE tenant et ne touche à rien")
        void deleteRefuseSauvegardeDUnAutreTenant() {
            BackupDescriptor celuiDUnAutre = descriptor(TENANT_B, "autre-tenant.sql.gz");
            when(repository.findById(celuiDUnAutre.id())).thenReturn(Optional.of(celuiDUnAutre));

            assertThrows(ResourceNotFoundException.class, () -> service.delete(celuiDUnAutre.id(), TENANT_A));
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("openArchive ne résout RIEN pour une sauvegarde d'un autre tenant")
        void openArchiveRefuseSauvegardeDUnAutreTenant() {
            BackupDescriptor celuiDUnAutre = descriptor(TENANT_B, "autre-tenant.sql.gz");
            when(repository.findById(celuiDUnAutre.id())).thenReturn(Optional.of(celuiDUnAutre));

            assertTrue(service.openArchive(celuiDUnAutre.id(), TENANT_A).isEmpty());
        }

        @Test
        @DisplayName("openArchive résout l'archive du tenant propriétaire")
        void openArchiveResoutPourLeTenantProprietaire() throws Exception {
            tenantAvecUneTableMultiTenante();
            BackupDescriptor created = service.create(TENANT_A, ACTOR);
            givenStored(created);

            BackupArchive archive = service.openArchive(created.id(), TENANT_A).orElseThrow();

            assertEquals(created.fileName(), archive.fileName());
            assertTrue(Files.isRegularFile(archive.path()));
            assertTrue(archive.path().startsWith(storage.rootLocation()));
        }

        @Test
        @DisplayName("un tenant nul ne provoque pas de NullPointerException")
        void tenantNulNeProvoquePasDeNpe() {
            assertEquals(List.of(), service.list(null));
            assertTrue(service.find(UUID.randomUUID(), null).isEmpty());
            assertTrue(service.openArchive(UUID.randomUUID(), null).isEmpty());
        }
    }

    // ==================================================================
    // Suppression
    // ==================================================================

    @Test
    @DisplayName("delete supprime l'archive du disque ET le descripteur")
    void deleteSupprimeArchiveEtDescripteur() throws Exception {
        tenantAvecUneTableMultiTenante();
        BackupDescriptor created = service.create(TENANT_A, ACTOR);
        givenStored(created);
        Path archive = storage.resolveArchive(TENANT_A, created.fileName());
        assertTrue(Files.exists(archive));

        service.delete(created.id(), TENANT_A);

        assertFalse(Files.exists(archive), "Le fichier doit être supprimé du disque");
        verify(repository).deleteById(created.id());
    }

    @Test
    @DisplayName("delete est un 404 si la sauvegarde n'existe pas")
    void deleteSurSauvegardeInconnue() {
        UUID inconnu = UUID.randomUUID();
        when(repository.findById(inconnu)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(inconnu, TENANT_A));
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete n'affecte QUE l'archive du tenant concerné")
    void deleteNaffectePasLesAutresTenants() {
        BackupDescriptor mien = descriptor(TENANT_A, "mien.sql.gz");
        when(repository.findById(mien.id())).thenReturn(Optional.of(mien));

        service.delete(mien.id(), TENANT_A);

        verify(repository).deleteById(mien.id());
        assertTrue(Files.isDirectory(storage.tenantRoot(TENANT_B)),
                "Le dossier de l'autre tenant ne doit pas être touché");
    }

    // ==================================================================
    // Rétention
    // ==================================================================

    @Test
    @DisplayName("purgeExpired conserve les sauvegardes récentes")
    void purgeExpireGardeLesSauvegardesRecentes() {
        when(repository.findAllByTenant(TENANT_A))
                .thenReturn(List.of(descriptor(TENANT_A, "recente.sql.gz")));

        assertEquals(0, service.purgeExpired(TENANT_A));
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("purgeExpired supprime les sauvegardes au-delà de la rétention")
    void purgeExpireSupprimeLesVieillesSauvegardes() {
        BackupDescriptor ancien = new BackupDescriptor(UUID.randomUUID(), TENANT_A, "ancienne.sql.gz", 10L, "a",
                Instant.now().minus(Duration.ofDays(400)), ACTOR, BackupStatus.COMPLETED);
        when(repository.findAllByTenant(TENANT_A)).thenReturn(List.of(ancien));

        assertEquals(1, service.purgeExpired(TENANT_A));
        verify(repository).deleteById(ancien.id());
    }

    @Test
    @DisplayName("une rétention à 0 n'écrase pas la sauvegarde venant d'être créée")
    void retentionDesactiveeNeSupprimePasLaSauvegardeFraiche() {
        BackupServiceImpl sansRetention = new BackupServiceImpl(this.dataSource, this.storage, this.repository, 0);
        when(repository.findAllByTenant(TENANT_A))
                .thenReturn(List.of(descriptor(TENANT_A, "fraiche.sql.gz")));

        assertEquals(0, sansRetention.purgeExpired(TENANT_A));
        verify(repository, never()).deleteById(any());
    }

    // ==================================================================
    // Utilitaires
    // ==================================================================

    private void givenStored(BackupDescriptor descriptor) {
        when(repository.findById(descriptor.id())).thenReturn(Optional.of(descriptor));
    }

    private static BackupDescriptor descriptor(UUID tenantId, String fileName) {
        return new BackupDescriptor(UUID.randomUUID(), tenantId, fileName, 12L, "0".repeat(64),
                Instant.now(), ACTOR, BackupStatus.COMPLETED);
    }

    /**
     * Connexion simulée et le {@code PreparedStatement} qu'elle a fourni, pour
     * pouvoir vérifier le SQL émis ET le paramètre lié.
     */
    private record CatalogueJdbc(Connection connection, PreparedStatement statement) {
    }

    /**
     * Recalcule l'emprepte SANS passer par le service, pour prouver que celle
     * enregistrée à la création est bien celle du fichier sur disque.
     */
    private static String independentSha256(Path file) throws Exception {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String unzip(Path archive) throws Exception {
        try (InputStream in = new GZIPInputStream(Files.newInputStream(archive))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void deleteRecursivement(Path dossier) throws Exception {
        if (!Files.exists(dossier)) {
            return;
        }
        try (var paths = Files.walk(dossier)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // nettoyage best-effort d'un dossier temporaire
                }
            });
        }
    }

    // ---------- Catalogues JDBC simulés ----------

    /**
     * Catalogue contenant {@code souls} (multi-tenant, 2 lignes) et
     * {@code app_config} (globale, sans {@code tenant_id}).
     */
    private void tenantAvecUneTableMultiTenanteEtUneTableGlobale() throws SQLException {
        Connection connection = nouvelleConnexion();
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        ResultSet colonnesOui = mock(ResultSet.class);
        ResultSet colonnesNon = mock(ResultSet.class);

        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.getCatalog()).thenReturn(null);
        when(metaData.getTables(nullable(String.class), nullable(String.class), anyString(), any(String[].class)))
                .thenReturn(tables);
        when(metaData.getColumns(nullable(String.class), nullable(String.class), eq("souls"), eq("tenant_id")))
                .thenReturn(colonnesOui);
        when(metaData.getColumns(nullable(String.class), nullable(String.class), eq("app_config"), eq("tenant_id")))
                .thenReturn(colonnesNon);

        when(tables.next()).thenReturn(true, true, false);
        when(tables.getString("TABLE_NAME")).thenReturn("souls", "app_config");
        when(colonnesOui.next()).thenReturn(true);
        when(colonnesNon.next()).thenReturn(false);

        ResultSet lignes = mock(ResultSet.class);
        ResultSetMetaData metaLignes = mock(ResultSetMetaData.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(lignes);
        when(lignes.getMetaData()).thenReturn(metaLignes);
        when(metaLignes.getColumnCount()).thenReturn(3);
        when(metaLignes.getColumnLabel(1)).thenReturn("id");
        when(metaLignes.getColumnLabel(2)).thenReturn("nom");
        when(metaLignes.getColumnLabel(3)).thenReturn("tenant_id");
        when(lignes.next()).thenReturn(true, true, false);
        when(lignes.getObject(1)).thenReturn(UUID.randomUUID(), UUID.randomUUID());
        when(lignes.getObject(2)).thenReturn("Ame de test", "Ame de test 2");
        when(lignes.getObject(3)).thenReturn(TENANT_A, TENANT_A);
    }

    /**
     * Catalogue contenant une seule table multi-tenant.
     */
    private void tenantAvecUneTableMultiTenante() throws SQLException {
        Connection connection = nouvelleConnexion();
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        ResultSet colonnesOui = mock(ResultSet.class);

        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.getCatalog()).thenReturn(null);
        when(metaData.getTables(nullable(String.class), nullable(String.class), anyString(), any(String[].class)))
                .thenReturn(tables);
        when(metaData.getColumns(nullable(String.class), nullable(String.class), eq("souls"), eq("tenant_id")))
                .thenReturn(colonnesOui);

        when(tables.next()).thenReturn(true, false);
        when(tables.getString("TABLE_NAME")).thenReturn("souls");
        when(colonnesOui.next()).thenReturn(true);

        ResultSet lignes = mock(ResultSet.class);
        ResultSetMetaData metaLignes = mock(ResultSetMetaData.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(lignes);
        when(lignes.getMetaData()).thenReturn(metaLignes);
        when(metaLignes.getColumnCount()).thenReturn(2);
        when(metaLignes.getColumnLabel(1)).thenReturn("id");
        when(metaLignes.getColumnLabel(2)).thenReturn("tenant_id");
        when(lignes.next()).thenReturn(true, false);
        when(lignes.getObject(1)).thenReturn(UUID.randomUUID());
        when(lignes.getObject(2)).thenReturn(TENANT_A);
    }

    /**
     * Catalogue avec une table multi-tenant mais zéro ligne.
     */
    private CatalogueJdbc tableVideDUneColonne() throws SQLException {
        Connection connection = nouvelleConnexion();
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        ResultSet colonnesOui = mock(ResultSet.class);

        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.getCatalog()).thenReturn(null);
        when(metaData.getTables(nullable(String.class), nullable(String.class), anyString(), any(String[].class)))
                .thenReturn(tables);
        when(metaData.getColumns(nullable(String.class), nullable(String.class), anyString(), eq("tenant_id")))
                .thenReturn(colonnesOui);
        when(tables.next()).thenReturn(true, false);
        when(tables.getString("TABLE_NAME")).thenReturn("souls");
        when(colonnesOui.next()).thenReturn(true);

        ResultSet lignes = mock(ResultSet.class);
        ResultSetMetaData metaLignes = mock(ResultSetMetaData.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(lignes);
        when(lignes.getMetaData()).thenReturn(metaLignes);
        when(metaLignes.getColumnCount()).thenReturn(1);
        when(metaLignes.getColumnLabel(1)).thenReturn("tenant_id");
        when(lignes.next()).thenReturn(false);
        return new CatalogueJdbc(connection, statement);
    }

    /**
     * Catalogue sans aucune table : tenant fraîchement provisionné.
     */
    private void aucunTableDansLeCatalogue() throws SQLException {
        Connection connection = nouvelleConnexion();
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.getCatalog()).thenReturn(null);
        when(metaData.getTables(nullable(String.class), nullable(String.class), anyString(), any(String[].class)))
                .thenReturn(tables);
        when(tables.next()).thenReturn(false);
    }

    private Connection nouvelleConnexion() throws SQLException {
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        return connection;
    }
}
