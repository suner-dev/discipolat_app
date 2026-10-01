package com.discipolat.modules.tenants.service;

import com.discipolat.common.multitenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path rootLocation;
    /**
     * §G6.5 — un dossier de stockage non accessible ne doit JAMAIS empêcher le
     * démarrage de l'application (l'upload n'est pas requis au boot). On démarre
     * en mode dégradé : les opérations d'écriture échouent avec une erreur
     * explicite au moment de l'appel, tout le reste du service reste disponible.
     */
    private final boolean available;

    public FileStorageService(@Value("${discipolat.file.storage.root:/var/discipolat/files}") String rootPath) {
        this.rootLocation = Paths.get(rootPath).toAbsolutePath().normalize();
        boolean ready = false;
        try {
            Files.createDirectories(this.rootLocation);
            ready = Files.isWritable(this.rootLocation);
            if (!ready) {
                log.error("⚠️ Dossier de stockage non accessible en écriture: {} — upload désactivé", rootPath);
            }
        } catch (IOException | RuntimeException e) {
            log.error("⚠️ Impossible de préparer le dossier de stockage: {} — upload désactivé ({})",
                    rootPath, e.getMessage());
        }
        this.available = ready;
    }

    /** Le stockage de fichiers est-il utilisable sur cette instance ? */
    public boolean isAvailable() {
        return available;
    }

    private void requireAvailable() {
        if (!available) {
            throw new IllegalStateException(
                    "Stockage de fichiers indisponible (dossier racine non inscriptible): " + rootLocation);
        }
    }

    /**
     * Upload a file with tenant isolation.
     * The file is stored under: {root}/{tenantId}/{relativePath}
     */
    public String upload(MultipartFile file, String relativePath) {
        return upload(file, relativePath, TenantContext.getCurrentTenantId());
    }

    /**
     * Upload a file with explicit tenant ID (for webhooks, jobs, etc.)
     */
    public String upload(MultipartFile file, String relativePath, UUID tenantId) {
        try {
            requireAvailable();
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Fichier vide");
            }
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("Fichier trop volumineux (max 10MB)");
            }
            if (relativePath == null || relativePath.isBlank()
                    || relativePath.contains("..")
                    || Paths.get(relativePath).isAbsolute()) {
                throw new SecurityException("Chemin de destination invalide");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("tenantId is required for file upload");
            }

            // Tenant-isolated path: {root}/{tenantId}/{relativePath}
            String tenantPrefix = tenantId.toString();
            Path tenantRoot = this.rootLocation.resolve(tenantPrefix).normalize().toAbsolutePath();

            // Security check: tenant root must be within the main root
            if (!tenantRoot.startsWith(this.rootLocation)) {
                throw new SecurityException("Tentative d'accès hors du dossier de stockage");
            }

            Path destinationFile = tenantRoot.resolve(relativePath).normalize().toAbsolutePath();

            // Security check: destination must be within tenant root
            if (!destinationFile.startsWith(tenantRoot)) {
                throw new SecurityException("Tentative d'accès hors du dossier du tenant");
            }

            // Create parent directories
            Files.createDirectories(destinationFile.getParent());

            // Copy the file
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            // Return the relative URL (tenant-aware)
            return "/api/v1/files/branding/" + tenantPrefix + "/" + relativePath;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload du fichier: " + relativePath, e);
        }
    }

    /**
     * Delete a file with tenant isolation.
     */
    public void delete(String relativePath) {
        delete(relativePath, TenantContext.getCurrentTenantId());
    }

    /**
     * Delete a file with explicit tenant ID.
     */
    public void delete(String relativePath, UUID tenantId) {
        if (!available) {
            return;
        }
        try {
            if (tenantId == null) {
                throw new IllegalArgumentException("tenantId is required for file deletion");
            }
            Path tenantRoot = this.rootLocation.resolve(tenantId.toString()).normalize().toAbsolutePath();
            Path filePath = tenantRoot.resolve(relativePath).normalize().toAbsolutePath();

            if (filePath.startsWith(tenantRoot)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            // Log but don't fail
        }
    }

    /**
     * Check if a file exists with tenant isolation.
     */
    public boolean exists(String relativePath) {
        return exists(relativePath, TenantContext.getCurrentTenantId());
    }

    /**
     * Check if a file exists with explicit tenant ID.
     */
    public boolean exists(String relativePath, UUID tenantId) {
        // PORT Develop1 (§ stockage) : si le dossier de stockage n'est pas pret,
        // une lecture d'existence repond « non » au lieu de lever une erreur.
        if (!available || tenantId == null) {
            return false;
        }
        Path tenantRoot = this.rootLocation.resolve(tenantId.toString()).normalize().toAbsolutePath();
        Path filePath = tenantRoot.resolve(relativePath).normalize().toAbsolutePath();
        return Files.exists(filePath) && filePath.startsWith(tenantRoot);
    }

    /**
     * Get the absolute file path with tenant isolation.
     */
    public Path getFilePath(String relativePath) {
        return getFilePath(relativePath, TenantContext.getCurrentTenantId());
    }

    /**
     * Get the absolute file path with explicit tenant ID.
     */
    public Path getFilePath(String relativePath, UUID tenantId) {
        // PORT Develop1 : la lecture d'un fichier n'a de sens que si le stockage est
        // disponible ; on leve une erreur explicite plutot qu'un chemin fantome.
        requireAvailable();
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        Path tenantRoot = this.rootLocation.resolve(tenantId.toString()).normalize().toAbsolutePath();
        Path filePath = tenantRoot.resolve(relativePath).normalize().toAbsolutePath();
        if (!filePath.startsWith(tenantRoot)) {
            throw new SecurityException("Accès non autorisé");
        }
        return filePath;
    }

    /**
     * Generate a unique filename.
     */
    public String generateUniqueFilename(String originalFilename) {
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Get the tenant-isolated root path for a given tenant.
     */
    public Path getTenantRoot(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        Path tenantRoot = this.rootLocation.resolve(tenantId.toString()).normalize().toAbsolutePath();
        if (!tenantRoot.startsWith(this.rootLocation)) {
            throw new SecurityException("Tentative d'accès hors du dossier de stockage");
        }
        try {
            Files.createDirectories(tenantRoot);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier du tenant: " + tenantId, e);
        }
        return tenantRoot;
    }
}