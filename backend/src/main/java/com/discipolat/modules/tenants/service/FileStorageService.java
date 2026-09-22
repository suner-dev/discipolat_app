package com.discipolat.modules.tenants.service;

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

    private final Path rootLocation;

    public FileStorageService(@Value("${discipolat.file.storage.root:/var/discipolat/files}") String rootPath) {
        this.rootLocation = Paths.get(rootPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier de stockage: " + rootPath, e);
        }
    }

    public String upload(MultipartFile file, String relativePath) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Fichier vide");
            }
            // G6.6 — garde-fous génériques : taille max 10MB + relativePath contraint.
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("Fichier trop volumineux (max 10MB)");
            }
            if (relativePath == null || relativePath.isBlank()
                    || relativePath.contains("..")
                    || java.nio.file.Paths.get(relativePath).isAbsolute()) {
                throw new SecurityException("Chemin de destination invalide");
            }

            Path destinationFile = this.rootLocation.resolve(relativePath).normalize().toAbsolutePath();
            
            // Vérification de sécurité : le fichier doit être dans le dossier root
            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new SecurityException("Tentative d'accès hors du dossier de stockage");
            }

            // Créer les dossiers parents
            Files.createDirectories(destinationFile.getParent());

            // Copier le fichier
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            // Retourner l'URL relative (pour stockage en DB)
            return "/api/v1/files/branding/" + relativePath;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload du fichier: " + relativePath, e);
        }
    }

    public void delete(String relativePath) {
        try {
            Path filePath = this.rootLocation.resolve(relativePath).normalize().toAbsolutePath();
            if (filePath.startsWith(this.rootLocation)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            // Log mais ne pas faire échouer
        }
    }

    public boolean exists(String relativePath) {
        Path filePath = this.rootLocation.resolve(relativePath).normalize().toAbsolutePath();
        return Files.exists(filePath) && filePath.startsWith(this.rootLocation);
    }

    public Path getFilePath(String relativePath) {
        Path filePath = this.rootLocation.resolve(relativePath).normalize().toAbsolutePath();
        if (!filePath.startsWith(this.rootLocation)) {
            throw new SecurityException("Accès non autorisé");
        }
        return filePath;
    }

    public String generateUniqueFilename(String originalFilename) {
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }
}