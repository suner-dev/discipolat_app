package com.discipolat.modules.files.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class FileService {

    private final FileEntityRepository fileRepository;
    private final SecurityUtils securityUtils;

    public FileService(FileEntityRepository fileRepository, SecurityUtils securityUtils) {
        this.fileRepository = fileRepository;
        this.securityUtils = securityUtils;
    }

    public FileEntity upload(FileEntity file) {
        file.setAuteurId(securityUtils.getCurrentUserId());
        return fileRepository.save(file);
    }

    @Transactional(readOnly = true)
    public FileEntity findById(UUID id) {
        return fileRepository.findById(id)
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("File", id));
    }

    @Transactional(readOnly = true)
    public Page<FileEntity> findByFamilleId(UUID familleId, Pageable pageable) {
        return fileRepository.findByFamilleIdAndDeletedFalse(familleId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FileEntity> findByEvenementId(UUID evenementId, Pageable pageable) {
        return fileRepository.findByEvenementIdAndDeletedFalse(evenementId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FileEntity> findByCategorie(String categorie, Pageable pageable) {
        return fileRepository.findByCategorieAndDeletedFalse(categorie, pageable);
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
}
