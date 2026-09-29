package com.discipolat.modules.backup.api;

import com.discipolat.common.exception.ResourceNotFoundException;
import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.common.test.StrictPlatformAuthzConfig;
import com.discipolat.modules.backup.domain.BackupArchive;
import com.discipolat.modules.backup.domain.BackupDescriptor;
import com.discipolat.modules.backup.domain.BackupResult;
import com.discipolat.modules.backup.domain.BackupService;
import com.discipolat.modules.backup.domain.BackupStatus;
import com.discipolat.modules.backup.domain.VerificationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests du {@link BackupController} : câblage des endpoints, autorité, et
 * isolation tenant.
 *
 * <p>Chaîne de sécurité RÉELLE : {@code @WebMvcTest} +
 * {@link com.discipolat.common.infrastructure.config.SecurityConfig} +
 * {@code JwtTokenProvider} réel. Le bean {@code authz} de
 * {@link StrictPlatformAuthzConfig} n'accepte QUE l'autorité
 * {@code PLATFORM_SUPER_ADMIN}, ce qui permet de prouver qu'un administrateur
 * d'église est bien refusé.
 *
 * <p>Point central de ce test : {@code POST /api/v1/backups/{id}/verify},
 * annoncé par le cahier de charges (CU-26) et longtemps inexistant.
 */
@WebMvcTest(controllers = BackupController.class)
@Import({SecurityConfig.class, StrictPlatformAuthzConfig.class})
class BackupControllerTest {

    private static final UUID SUPER_ADMIN_ID = UUID.fromString("44444444-4444-4440-8000-000000000004");
    private static final UUID CHURCH_ADMIN_ID = UUID.fromString("11111111-1111-4111-8111-000000000001");
    private static final UUID TENANT_A = UUID.fromString("aaaaaaaa-0000-4000-8000-00000000000a");
    private static final UUID TENANT_B = UUID.fromString("bbbbbbbb-0000-4000-8000-00000000000b");
    private static final UUID BACKUP_ID = UUID.fromString("cccccccc-0000-4000-8000-00000000000c");

    private static final String NOM_ARCHIVE = "discipolat_tenant-1a2b3c4d_20260101_120000_abcd1234.sql.gz";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private BackupService backupService;

    @TempDir
    Path dossierTemporaire;

    @BeforeEach
    void setUp() {
        // Le controleur lit le tenant via TenantContext.requireTenantId(), jamais
        // dans le corps ni la query string. Le contexte est pose par
        // TenantInterceptor a partir du jeton (qui porte TENANT_A) : le poser a la
        // main ici serait masque puis ecrase par l'intercepteur, et le test
        // passerait pour une mauvaise raison.
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private String bearer(String role, UUID userId) {
        // Le jeton porte le TENANT : un vrai login le fait, et l'intercepteur
        // TenantInterceptor positionne le contexte a partir du jeton. Un jeton sans
        // tenant est refuse par la garde (401 « missing tenant context ») — c'est
        // le comportement correct de la securite, pas un bug du controleur.
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                userId, "user@discipolat.com", role, Set.of(role), false, TENANT_A);
    }

    private String superAdmin() {
        return bearer("PLATFORM_SUPER_ADMIN", SUPER_ADMIN_ID);
    }

    private static BackupDescriptor descriptor() {
        return new BackupDescriptor(BACKUP_ID, TENANT_A, NOM_ARCHIVE, 512L, "a".repeat(64),
                Instant.parse("2026-01-01T12:00:00Z"), SUPER_ADMIN_ID, BackupStatus.COMPLETED);
    }

    // ==================================================================
    // Autorité — le point le plus sensible
    // ==================================================================

    @Test
    @DisplayName("Le contrôleur porte @authz.isPlatformSuperAdmin() au niveau classe")
    void annotationDautoritePresente() {
        PreAuthorize annotation = BackupController.class.getAnnotation(PreAuthorize.class);

        assertNotNull(annotation,
                "BackupController doit porter @PreAuthorize : une sauvegarde est une opération de PLATEFORME");
        assertEquals("@authz.isPlatformSuperAdmin()", annotation.value(),
                "L'autorité attendue pour une sauvegarde est celle du super admin plateforme");
    }

    @Test
    @DisplayName("Sans jeton → 401 sur chaque endpoint")
    void sansJetonInterdit() throws Exception {
        mockMvc.perform(get("/api/v1/backups")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/backups")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/backups/" + BACKUP_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/backups/" + BACKUP_ID + "/verify")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/backups/" + BACKUP_ID)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un ADMIN d'église → 403 : il ne peut pas dumper la base")
    void adminDEgliseRefuse() throws Exception {
        String token = bearer("ADMIN", CHURCH_ADMIN_ID);

        mockMvc.perform(get("/api/v1/backups").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/backups").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/backups/" + BACKUP_ID + "/verify").header("Authorization", token))
                .andExpect(status().isForbidden());

        // Preuve que le refus intervient AVANT le service
        verify(backupService, never()).list(any());
        verify(backupService, never()).createWithReport(any(), any());
        verify(backupService, never()).verify(any(), any());
    }

    // ==================================================================
    // Câblage des endpoints
    // ==================================================================

    @Test
    @DisplayName("POST /api/v1/backups → 201 avec le descripteur et le rapport d'export")
    void creationRenvoie201() throws Exception {
        BackupDescriptor created = descriptor();
        when(backupService.createWithReport(eq(TENANT_A), any()))
                .thenReturn(BackupResult.of(created, 7, 1234L, 42L));

        mockMvc.perform(post("/api/v1/backups").header("Authorization", superAdmin()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(BACKUP_ID.toString()))
                .andExpect(jsonPath("$.tenantId").value(TENANT_A.toString()))
                .andExpect(jsonPath("$.fileName").value(NOM_ARCHIVE))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.sha256").value("a".repeat(64)))
                .andExpect(jsonPath("$.tablesExported").value(7))
                .andExpect(jsonPath("$.rowsExported").value(1234))
                .andExpect(jsonPath("$.durationMs").value(42));

        // Le tenant vient du contexte, l'acteur du jeton
        verify(backupService).createWithReport(TENANT_A, SUPER_ADMIN_ID);
    }

    @Test
    @DisplayName("GET /api/v1/backups → 200 avec la liste du tenant courant")
    void listeRenvoie200() throws Exception {
        when(backupService.list(TENANT_A)).thenReturn(List.of(descriptor()));

        mockMvc.perform(get("/api/v1/backups").header("Authorization", superAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(BACKUP_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/v1/backups/{id} → 200 si la sauvegarde existe, 404 sinon")
    void detailRenvoie200Ou404() throws Exception {
        when(backupService.find(eq(BACKUP_ID), eq(TENANT_A))).thenReturn(Optional.of(descriptor()));

        mockMvc.perform(get("/api/v1/backups/" + BACKUP_ID).header("Authorization", superAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BACKUP_ID.toString()));

        when(backupService.find(eq(BACKUP_ID), eq(TENANT_A))).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/backups/" + BACKUP_ID).header("Authorization", superAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/backups/{id}/verify → 200 et délègue au service (endpoint du cahier de charges)")
    void verificationDelegueAuService() throws Exception {
        when(backupService.verify(eq(BACKUP_ID), eq(TENANT_A)))
                .thenReturn(VerificationResult.ok(BACKUP_ID, TENANT_A, "a".repeat(64), 512L, Instant.now()));

        mockMvc.perform(post("/api/v1/backups/" + BACKUP_ID + "/verify").header("Authorization", superAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backupId").value(BACKUP_ID.toString()))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.expectedSha256").value("a".repeat(64)))
                .andExpect(jsonPath("$.actualSha256").value("a".repeat(64)))
                .andExpect(jsonPath("$.sizeBytes").value(512));

        verify(backupService).verify(BACKUP_ID, TENANT_A);
    }

    @Test
    @DisplayName("POST /api/v1/backups/{id}/verify → 200 avec valid=false si l'archive est corrompue")
    void verificationRappelleUneArchiveCorrompue() throws Exception {
        when(backupService.verify(eq(BACKUP_ID), eq(TENANT_A)))
                .thenReturn(VerificationResult.corrupted(BACKUP_ID, TENANT_A, "a".repeat(64), "b".repeat(64),
                        520L, Instant.now()));

        mockMvc.perform(post("/api/v1/backups/" + BACKUP_ID + "/verify").header("Authorization", superAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.expectedSha256").value("a".repeat(64)))
                .andExpect(jsonPath("$.actualSha256").value("b".repeat(64)));
    }

    @Test
    @DisplayName("DELETE /api/v1/backups/{id} → 204")
    void suppressionRenvoie204() throws Exception {
        mockMvc.perform(delete("/api/v1/backups/" + BACKUP_ID).header("Authorization", superAdmin()))
                .andExpect(status().isNoContent());

        verify(backupService).delete(BACKUP_ID, TENANT_A);
    }

    @Test
    @DisplayName("GET /api/v1/backups/{id}/download → 200 en pièce jointe")
    void telechargementRenvoie200() throws Exception {
        Path archive = dossierTemporaire.resolve(NOM_ARCHIVE);
        Files.write(archive, "contenu".getBytes(StandardCharsets.UTF_8));
        when(backupService.openArchive(eq(BACKUP_ID), eq(TENANT_A)))
                .thenReturn(Optional.of(new BackupArchive(
                        BACKUP_ID, NOM_ARCHIVE, archive, Files.size(archive))));

        mockMvc.perform(get("/api/v1/backups/" + BACKUP_ID + "/download").header("Authorization", superAdmin()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(NOM_ARCHIVE)));
    }

    // ==================================================================
    // Isolation tenant
    // ==================================================================

    @Test
    @DisplayName("Un tenantId dans la query string est IGNORÉ : seul le contexte fait foi")
    void tenantIdDeLaQueryStringEstIgnore() throws Exception {
        when(backupService.list(TENANT_A)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/backups")
                        .param("tenantId", TENANT_B.toString())
                        .header("Authorization", superAdmin()))
                .andExpect(status().isOk());

        // Preuve : le service reçoit le tenant du contexte, jamais celui de l'URL
        verify(backupService).list(TENANT_A);
        verify(backupService, never()).list(TENANT_B);
    }

    @Test
    @DisplayName("La sauvegarde d'un autre tenant est un 404, pas un 403")
    void sauvegardeDUnAutreTenantEst404() throws Exception {
        when(backupService.verify(eq(BACKUP_ID), eq(TENANT_A)))
                .thenThrow(new ResourceNotFoundException("Backup", "id", BACKUP_ID));

        mockMvc.perform(post("/api/v1/backups/" + BACKUP_ID + "/verify").header("Authorization", superAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("RESOURCE_NOT_FOUND"));
    }
}
