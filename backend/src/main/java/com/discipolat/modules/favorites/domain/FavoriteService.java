package com.discipolat.modules.favorites.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Favoris : chaque utilisateur peut marquer des âmes (et familles/départements)
 * comme favorites pour un accès rapide. Les favoris sont personnels.
 */
@Service
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final SoulRepository soulRepository;
    private final SecurityUtils securityUtils;
    private final com.discipolat.modules.souls.domain.SoulService soulService;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           SoulRepository soulRepository,
                           SecurityUtils securityUtils,
                           com.discipolat.modules.souls.domain.SoulService soulService) {
        this.favoriteRepository = favoriteRepository;
        this.soulRepository = soulRepository;
        this.securityUtils = securityUtils;
        this.soulService = soulService;
    }

    /** Bascule l'état favori. Retourne le nouvel état (true = ajouté). */
    public boolean toggle(FavoriteEntityType entityType, UUID entityId) {
        UUID userId = securityUtils.getCurrentUserId();
        assertEntityAccessible(entityType, entityId);
        var existing = favoriteRepository.findByUserIdAndEntityTypeAndEntityId(userId, entityType, entityId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        }
        favoriteRepository.save(Favorite.builder()
                .userId(userId).entityType(entityType).entityId(entityId).build());
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(FavoriteEntityType entityType, UUID entityId) {
        return favoriteRepository.existsByUserIdAndEntityTypeAndEntityId(
                securityUtils.getCurrentUserId(), entityType, entityId);
    }

    /** Une âme favorite doit appartenir à l'espace métier courant. */
    private void assertEntityAccessible(FavoriteEntityType entityType, UUID entityId) {
        if (entityType == FavoriteEntityType.SOUL) {
            soulService.assertAccessible(entityId);
        }
    }

    /** Âmes favorites de l'utilisateur, avec leurs noms pour l'affichage. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSoulFavorites() {
        UUID userId = securityUtils.getCurrentUserId();
        return favoriteRepository.findByUserIdAndEntityTypeOrderByCreatedAtDesc(userId, FavoriteEntityType.SOUL)
                .stream()
                .map(f -> {
                    String nom = soulRepository.findById(f.getEntityId())
                            .map(Soul::getNomComplet)
                            .orElse("Âme supprimée");
                    return Map.<String, Object>of(
                            "entityId", f.getEntityId(),
                            "nom", nom,
                            "createdAt", f.getCreatedAt().toString());
                })
                .toList();
    }
}
