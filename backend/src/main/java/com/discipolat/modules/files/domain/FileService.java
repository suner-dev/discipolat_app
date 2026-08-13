package com.discipolat.modules.files.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FileService {

    private final FileEntityRepository fileRepository;
    private final SecurityUtils securityUtils;
    private final WorkspaceScopeService workspaceScopeService;

    public FileService(FileEntityRepository fileRepository, SecurityUtils securityUtils,
                       WorkspaceScopeService workspaceScopeService) {
        this.fileRepository = fileRepository;
        this.securityUtils = securityUtils;
        this.workspaceScopeService = workspaceScopeService;
    }

    public FileEntity upload(FileEntity file) {
        if (file.getFamilleId() != null) assertFamilyAccessible(file.getFamilleId());
        file.setAuteurId(securityUtils.getCurrentUserId());
        return fileRepository.save(file);
    }

    @Transactional(readOnly = true)
    public FileEntity findById(UUID id) {
        FileEntity file = fileRepository.findById(id)
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("File", id));
        if (file.getFamilleId() != null) assertFamilyAccessible(file.getFamilleId());
        return file;
    }

    /**
     * Périmètre famille effectif pour une liste de fichiers :
     * <ul>
     *   <li>{@code null} : aucun filtre famille (super-utilisateur — tout voir) ;</li>
     *   <li>vide : rien de visible (aucune famille accessible) ;</li>
     *   <li>sinon : ids des familles accessibles de l'espace métier courant.</li>
     * </ul>
     * Un {@code familleId} explicite vérifie d'abord l'accès à cette famille.
     * Les fichiers « globaux » ({@code familleId} nul) restent réservés aux
     * super-utilisateurs dans les listes (bibliothèque Documents).
     */
    private Collection<UUID> effectiveFamilyScope(UUID requestedFamilleId) {
        if (requestedFamilleId != null) {
            assertFamilyAccessible(requestedFamilleId);
            return List.of(requestedFamilleId);
        }
        if (workspaceScopeService.isSuperUser()) {
            return null;
        }
        return workspaceScopeService.accessibleFamilyIds();
    }

    @Transactional(readOnly = true)
    public Page<FileEntity> findByFamilleId(UUID familleId, Pageable pageable) {
        var scope = effectiveFamilyScope(familleId);
        if (scope == null) {
            return fileRepository.findByFamilleIdAndDeletedFalse(null, pageable);
        }
        if (scope.isEmpty()) {
            return Page.empty(pageable);
        }
        return fileRepository.findByFamilleIdInAndDeletedFalse(scope, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FileEntity> findByEvenementId(UUID evenementId, Pageable pageable) {
        return fileRepository.findByEvenementIdAndDeletedFalse(evenementId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FileEntity> findByCategorie(String categorie, Pageable pageable) {
        var scope = effectiveFamilyScope(null);
        if (scope == null) {
            return fileRepository.findByCategorieAndDeletedFalse(categorie, pageable);
        }
        if (scope.isEmpty()) {
            return Page.empty(pageable);
        }
        return fileRepository.findByCategorieAndFamilleIdInAndDeletedFalse(categorie, scope, pageable);
    }

    public FileEntity update(UUID id, FileEntity updated) {
        FileEntity file = findById(id);
        if (updated.getDescription() != null) file.setDescription(updated.getDescription());
        if (updated.getCategorie() != null) file.setCategorie(updated.getCategorie());
        if (updated.getNom() != null) file.setNom(updated.getNom());
        return fileRepository.save(file);
    }

    public void delete(UUID id) {
        FileEntity file = findById(id);
        file.setDeleted(true);
        fileRepository.save(file);
    }

    private void assertFamilyAccessible(UUID familleId) {
        if (familleId == null) return;
        if (workspaceScopeService.isSuperUser()) return;
        if (!workspaceScopeService.canAccessFamily(familleId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé : cette famille ne fait pas partie de votre espace métier");
        }
    }
}
