package com.discipolat.modules.testimonials.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TestimonyService {

    private final TestimonyRepository testimonyRepository;
    private final UserRepository userRepository;

    public TestimonyService(TestimonyRepository testimonyRepository, UserRepository userRepository) {
        this.testimonyRepository = testimonyRepository;
        this.userRepository = userRepository;
    }

    public Page<Testimony> list(Pageable pageable, String statut, String categorie) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (statut != null) {
            return testimonyRepository.findByTenantIdAndStatutOrderByCreatedAtDesc(tenantId,
                    Testimony.Statut.valueOf(statut), pageable);
        }
        if (categorie != null) {
            return testimonyRepository.findByTenantIdAndCategorieOrderByCreatedAtDesc(tenantId,
                    Testimony.Categorie.valueOf(categorie), pageable);
        }
        return testimonyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    public Testimony getById(UUID id) {
        return testimonyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Testimony", id));
    }

    public Testimony create(String titre, String contenu, String categorie, UUID userId) {
        Testimony t = new Testimony();
        t.setTenantId(TenantContext.getCurrentTenantId());
        t.setTitre(titre);
        t.setContenu(contenu);
        t.setCategorie(Testimony.Categorie.valueOf(categorie));
        t.setAuteurId(userId);
        return testimonyRepository.save(t);
    }

    public Testimony approve(UUID id) {
        Testimony t = getById(id);
        t.setStatut(Testimony.Statut.APPROUVE);
        t.setUpdatedAt(LocalDateTime.now());
        return testimonyRepository.save(t);
    }

    public Testimony reject(UUID id) {
        Testimony t = getById(id);
        t.setStatut(Testimony.Statut.REFUSE);
        t.setUpdatedAt(LocalDateTime.now());
        return testimonyRepository.save(t);
    }

    public Testimony like(UUID id) {
        Testimony t = getById(id);
        t.setLikes(t.getLikes() + 1);
        t.setUpdatedAt(LocalDateTime.now());
        return testimonyRepository.save(t);
    }

    /**
     * Resolve author names for a list of testimonies.
     * Batches user lookups to avoid N+1 queries.
     */
    public Map<UUID, String> resolveAuteurNames(List<Testimony> testimonies) {
        Set<UUID> auteurIds = testimonies.stream()
                .map(Testimony::getAuteurId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (auteurIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, String> names = new HashMap<>();
        userRepository.findAllById(auteurIds).forEach(user -> {
            String name = buildDisplayName(user);
            names.put(user.getId(), name);
        });
        return names;
    }

    private String buildDisplayName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String full = (first + " " + last).trim();
        if (!full.isEmpty()) return full;
        if (user.getEmail() != null) return user.getEmail();
        return "Anonyme";
    }
}
