package com.discipolat.modules.network.api;

import com.discipolat.modules.network.domain.NetworkDirectory;
import com.discipolat.modules.network.domain.NetworkEvent;
import com.discipolat.modules.network.domain.NetworkResource;
import com.discipolat.modules.network.domain.NetworkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST du réseau inter-églises (Discipolat Network).
 *
 * Sécurité : tous les endpoints nécessitent une authentification.
 * Les ressources/événements publics sont visibles par toutes les églises.
 * Les opérations d'écriture sont restreintes aux rôles ADMIN/PASTEUR.
 */
@RestController
@RequestMapping("/api/v1/network")
@PreAuthorize("isAuthenticated()")
public class NetworkController {

    private final NetworkService service;

    public NetworkController(NetworkService service) {
        this.service = service;
    }

    // ======================== RESSOURCES ========================

    /** Ressources partagées par toutes les églises. */
    @GetMapping("/resources")
    public ResponseEntity<List<NetworkResource>> listSharedResources() {
        return ResponseEntity.ok(service.listSharedResources());
    }

    /** Mes ressources (celles de mon église). */
    @GetMapping("/resources/mine")
    public ResponseEntity<List<NetworkResource>> listMyResources() {
        return ResponseEntity.ok(service.listMyResources());
    }

    /** Recherche dans les ressources partagées. */
    @GetMapping("/resources/search")
    public ResponseEntity<List<NetworkResource>> searchResources(@RequestParam String q) {
        return ResponseEntity.ok(service.searchResources(q));
    }

    /** Ressources par catégorie. */
    @GetMapping("/resources/category/{category}")
    public ResponseEntity<List<NetworkResource>> listByCategory(@PathVariable String category) {
        return ResponseEntity.ok(service.listByCategory(category));
    }

    /** Crée une ressource partagée. */
    @PostMapping("/resources")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<NetworkResource> createResource(@RequestBody NetworkResource resource) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createResource(resource));
    }

    /** Incrémente les téléchargements. */
    @PostMapping("/resources/{id}/download")
    public ResponseEntity<NetworkResource> download(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(service.incrementDownloads(id));
    }

    /** Désactive une ressource. */
    @DeleteMapping("/resources/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Void> deactivateResource(@PathVariable java.util.UUID id) {
        service.deactivateResource(id);
        return ResponseEntity.noContent().build();
    }

    // ======================== ÉVÉNEMENTS ========================

    /** Événements publics à venir. */
    @GetMapping("/events")
    public ResponseEntity<List<NetworkEvent>> listUpcomingEvents() {
        return ResponseEntity.ok(service.listUpcomingEvents());
    }

    /** Tous les événements publics (y passés). */
    @GetMapping("/events/all")
    public ResponseEntity<List<NetworkEvent>> listAllPublicEvents() {
        return ResponseEntity.ok(service.listAllPublicEvents());
    }

    /** Mes événements. */
    @GetMapping("/events/mine")
    public ResponseEntity<List<NetworkEvent>> listMyEvents() {
        return ResponseEntity.ok(service.listMyEvents());
    }

    /** Événements par type. */
    @GetMapping("/events/type/{type}")
    public ResponseEntity<List<NetworkEvent>> listByEventType(@PathVariable String type) {
        return ResponseEntity.ok(service.listByEventType(type));
    }

    /** Crée un événement inter-églises. */
    @PostMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<NetworkEvent> createEvent(@RequestBody NetworkEvent event) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createEvent(event));
    }

    /** S'inscrire à un événement. */
    @PostMapping("/events/{id}/join")
    public ResponseEntity<NetworkEvent> joinEvent(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(service.joinEvent(id));
    }

    /** Annuler son inscription. */
    @PostMapping("/events/{id}/leave")
    public ResponseEntity<NetworkEvent> leaveEvent(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(service.leaveEvent(id));
    }

    /** Désactive un événement. */
    @DeleteMapping("/events/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Void> deactivateEvent(@PathVariable java.util.UUID id) {
        service.deactivateEvent(id);
        return ResponseEntity.noContent().build();
    }

    // ======================== RÉPERTOIRE ========================

    /** Liste des églises volontairement listées. */
    @GetMapping("/directory")
    public ResponseEntity<List<NetworkDirectory>> listDirectory() {
        return ResponseEntity.ok(service.listDirectory());
    }

    /** Répertoire par pays. */
    @GetMapping("/directory/country/{country}")
    public ResponseEntity<List<NetworkDirectory>> listByCountry(@PathVariable String country) {
        return ResponseEntity.ok(service.listDirectoryByCountry(country));
    }

    /** Recherche dans le répertoire. */
    @GetMapping("/directory/search")
    public ResponseEntity<List<NetworkDirectory>> searchDirectory(@RequestParam String q) {
        return ResponseEntity.ok(service.searchDirectory(q));
    }

    /** Mon entrée dans le répertoire. */
    @GetMapping("/directory/mine")
    public ResponseEntity<NetworkDirectory> getMyDirectoryEntry() {
        return ResponseEntity.ok(service.getOrCreateMyDirectoryEntry());
    }

    /** Met à jour mon entrée dans le répertoire. */
    @PutMapping("/directory/mine")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<NetworkDirectory> updateMyDirectoryEntry(@RequestBody NetworkDirectory updates) {
        return ResponseEntity.ok(service.updateMyDirectoryEntry(updates));
    }

    /** Active/désactive mon listing public. */
    @PostMapping("/directory/mine/listing")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<NetworkDirectory> toggleListing(@RequestBody Map<String, Boolean> body) {
        boolean listed = Boolean.TRUE.equals(body.get("listed"));
        return ResponseEntity.ok(service.toggleListing(listed));
    }

    // ======================== STATISTIQUES ========================

    /** Résumé du réseau (responsables de l'église). */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> getNetworkStats() {
        return ResponseEntity.ok(service.getNetworkStats());
    }
}
