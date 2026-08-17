package com.discipolat.modules.files.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface FileEntityRepository extends JpaRepository<FileEntity, UUID> {
    Page<FileEntity> findByFamilleIdAndDeletedFalse(UUID familleId, Pageable pageable);

    /** Source du Page Builder FICHIERS : derniers documents (non supprimés). */
    List<FileEntity> findTop10ByDeletedFalseOrderByCreatedAtDesc();

    /** Source du Page Builder FICHIERS scopée : derniers documents des familles accessibles. */
    List<FileEntity> findTop10ByFamilleIdInAndDeletedFalseOrderByCreatedAtDesc(Collection<UUID> familleIds);
    Page<FileEntity> findByFamilleIdInAndDeletedFalse(Collection<UUID> familleIds, Pageable pageable);
    Page<FileEntity> findByEvenementIdAndDeletedFalse(UUID evenementId, Pageable pageable);
    Page<FileEntity> findByAuteurIdAndDeletedFalse(UUID auteurId, Pageable pageable);
    Page<FileEntity> findByCategorieAndDeletedFalse(String categorie, Pageable pageable);
    Page<FileEntity> findByCategorieAndFamilleIdInAndDeletedFalse(String categorie, Collection<UUID> familleIds, Pageable pageable);
    long countByFamilleIdAndDeletedFalse(UUID familleId);
}
