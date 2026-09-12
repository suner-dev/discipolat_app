package com.discipolat.modules.files.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.files.domain.FileEntity;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.files.domain.FileService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;
    private final FileEntityRepository fileRepository;

    public FileController(FileService fileService, FileEntityRepository fileRepository) {
        this.fileService = fileService;
        this.fileRepository = fileRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<FileResponse> upload(@Valid @RequestBody CreateFileRequest request) {
        FileEntity file = FileEntity.builder()
                .nom(request.nom())
                .typeFichier(request.typeFichier())
                .taille(request.taille() != null ? request.taille() : 0L)
                .chemin(request.chemin())
                .description(request.description())
                .familleId(request.familleId())
                .evenementId(request.evenementId())
                .categorie(request.categorie() != null ? request.categorie() : "DOCUMENT")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FileResponse.from(fileService.upload(file)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<FileResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(FileResponse.from(fileService.findById(id)));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fichier non trouvé"));

        // Verify tenant access
        UUID currentTenantId = TenantContext.requireTenantId();
        if (!file.getTenantId().equals(currentTenantId)) {
            throw new RuntimeException("Accès non autorisé à ce fichier");
        }

        // Resolve file path
        Path filePath = Paths.get(file.getChemin()).toAbsolutePath().normalize();
        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Fichier non trouvé", e);
        }

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("Fichier non trouvé ou illisible");
        }

        String contentType = determineContentType(file.getTypeFichier(), file.getNom());
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + file.getNom() + "\"")
                .body(resource);
    }

    private String determineContentType(String typeFichier, String nom) {
        if (typeFichier != null && !typeFichier.isBlank()) {
            return typeFichier;
        }
        // Fallback: guess from extension
        String extension = "";
        int dotIndex = nom.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = nom.substring(dotIndex + 1).toLowerCase();
        }
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc", "docx" -> "application/msword";
            case "xls", "xlsx" -> "application/vnd.ms-excel";
            case "ppt", "pptx" -> "application/vnd.ms-powerpoint";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<FileResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID familleId,
            @RequestParam(required = false) UUID evenementId,
            @RequestParam(required = false) String categorie) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FileEntity> files;
        if (familleId != null) {
            files = fileService.findByFamilleId(familleId, pageable);
        } else if (evenementId != null) {
            files = fileService.findByEvenementId(evenementId, pageable);
        } else if (categorie != null) {
            files = fileService.findByCategorie(categorie, pageable);
        } else {
            files = fileService.findByFamilleId(null, pageable);
        }
        Page<FileResponse> response = files.map(FileResponse::from);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<FileResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateFileRequest request) {
        FileEntity file = FileEntity.builder()
                .nom(request.nom())
                .description(request.description())
                .categorie(request.categorie())
                .build();
        return ResponseEntity.ok(FileResponse.from(fileService.update(id, file)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
