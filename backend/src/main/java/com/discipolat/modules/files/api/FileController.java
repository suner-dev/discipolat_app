package com.discipolat.modules.files.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.files.domain.FileEntity;
import com.discipolat.modules.files.domain.FileService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
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
                                               @RequestBody UpdateFileRequest request) {
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
