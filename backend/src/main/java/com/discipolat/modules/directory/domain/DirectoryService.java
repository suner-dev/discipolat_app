package com.discipolat.modules.directory.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DirectoryService {

    private final DirectoryRepository repository;

    public DirectoryService(DirectoryRepository repository) {
        this.repository = repository;
    }

    public Page<DirectoryEntry> listPublic(Pageable pageable) {
        return repository.findByTenantIdAndPublicProfilTrueOrderByMembreId(
                TenantContext.getCurrentTenantId(), pageable);
    }

    public List<DirectoryEntry> listAllPublic() {
        return repository.findByTenantIdAndPublicProfilTrue(TenantContext.getCurrentTenantId());
    }

    public DirectoryEntry getOrCreate(UUID membreId) {
        List<DirectoryEntry> existing = repository.findByMembreId(membreId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        DirectoryEntry entry = new DirectoryEntry();
        entry.setTenantId(TenantContext.getCurrentTenantId());
        entry.setMembreId(membreId);
        return repository.save(entry);
    }

    public DirectoryEntry update(UUID membreId, String bio, String téléphone, String email,
                                  String département, String rôle, Boolean publicProfil) {
        DirectoryEntry entry = getOrCreate(membreId);
        if (bio != null) entry.setBio(bio);
        if (téléphone != null) entry.setTéléphone(téléphone);
        if (email != null) entry.setEmail(email);
        if (département != null) entry.setDépartement(département);
        if (rôle != null) entry.setRôle(rôle);
        if (publicProfil != null) entry.setPublicProfil(publicProfil);
        entry.setUpdatedAt(LocalDateTime.now());
        return repository.save(entry);
    }

    public void togglePublic(UUID membreId) {
        DirectoryEntry entry = getOrCreate(membreId);
        entry.setPublicProfil(!entry.isPublicProfil());
        entry.setUpdatedAt(LocalDateTime.now());
        repository.save(entry);
    }
}
