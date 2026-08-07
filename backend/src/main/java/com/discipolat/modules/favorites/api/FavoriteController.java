package com.discipolat.modules.favorites.api;

import com.discipolat.common.exception.BadRequestException;
import com.discipolat.modules.favorites.domain.FavoriteEntityType;
import com.discipolat.modules.favorites.domain.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favorites")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggle(
            @RequestBody Map<String, String> body) {
        FavoriteEntityType type = parseType(body.get("entityType"));
        UUID entityId = parseUuid(body.get("entityId"));
        boolean added = favoriteService.toggle(type, entityId);
        return ResponseEntity.ok(Map.of("favorite", added));
    }

    @GetMapping("/is-favorite")
    public ResponseEntity<Map<String, Object>> isFavorite(
            @RequestParam String entityType,
            @RequestParam UUID entityId) {
        boolean fav = favoriteService.isFavorite(parseType(entityType), entityId);
        return ResponseEntity.ok(Map.of("favorite", fav));
    }

    @GetMapping("/souls")
    public ResponseEntity<List<Map<String, Object>>> soulFavorites() {
        return ResponseEntity.ok(favoriteService.listSoulFavorites());
    }

    private FavoriteEntityType parseType(String raw) {
        try {
            return FavoriteEntityType.valueOf((raw == null ? "SOUL" : raw).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Type d'entité invalide (attendu : SOUL, FAMILY, DEPARTMENT)");
        }
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (Exception e) {
            throw new BadRequestException("Identifiant d'entité invalide");
        }
    }
}
