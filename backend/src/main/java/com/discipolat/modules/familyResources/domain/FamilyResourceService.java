package com.discipolat.modules.familyResources.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class FamilyResourceService {

    private final FamilyResourceRepository repository;
    private final SoulRepository soulRepository;

    public FamilyResourceService(FamilyResourceRepository repository,
                                 SoulRepository soulRepository) {
        this.repository = repository;
        this.soulRepository = soulRepository;
    }

    public List<FamilyResource> listByFamily(UUID familleId) {
        return repository.findByFamilleIdOrderByCreatedAtDesc(familleId);
    }

    public Page<FamilyResource> listByFamilyPage(UUID familleId, Pageable pageable) {
        return repository.findByFamilleIdOrderByCreatedAtDesc(familleId, pageable);
    }

    /** Liste paginée des ressources de la famille de l'utilisateur donné. */
    public Page<FamilyResource> listForUser(UUID userId, Pageable pageable) {
        UUID familleId = soulRepository.findAllByUserId(userId).stream()
                .map(Soul::getFamilleId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (familleId == null) {
            return Page.empty(pageable);
        }
        return repository.findByFamilleIdOrderByCreatedAtDesc(familleId, pageable);
    }

    public FamilyResource getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FamilyResource", id));
    }

    public FamilyResource create(UUID familleId, String titre, String description, String type, String url, UUID userId) {
        FamilyResource resource = new FamilyResource();
        resource.setTenantId(TenantContext.getCurrentTenantId());
        resource.setFamilleId(familleId);
        resource.setTitre(titre);
        resource.setDescription(description);
        resource.setType(FamilyResource.Type.valueOf(type != null ? type : "DOCUMENT"));
        resource.setUrl(url);
        resource.setUploadéPar(userId);
        return repository.save(resource);
    }

    public void delete(UUID id) {
        repository.delete(getById(id));
    }
}
