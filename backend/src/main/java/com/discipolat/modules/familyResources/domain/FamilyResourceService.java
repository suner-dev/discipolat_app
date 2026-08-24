package com.discipolat.modules.familyResources.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FamilyResourceService {

    private final FamilyResourceRepository repository;

    public FamilyResourceService(FamilyResourceRepository repository) {
        this.repository = repository;
    }

    public List<FamilyResource> listByFamily(UUID familleId) {
        return repository.findByFamilleIdOrderByCreatedAtDesc(familleId);
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
