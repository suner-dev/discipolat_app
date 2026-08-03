package com.discipolat.modules.search.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.search.domain.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Intelligent search across souls and users.
     * Results are filtered by the current user's role:
     * - PASTEUR/ADMIN: searches all souls and users
     * - RESPONSABLE: searches only their department's souls
     * - Chef de famille: searches only their family's souls
     * - FAISEUR: searches only their assigned souls
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<Map<String, Object>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50));
        Page<Map<String, Object>> results = searchService.search(q, pageable);
        return ResponseEntity.ok(PageResponse.of(
                results.getContent(), results.getNumber(), results.getSize(),
                results.getTotalElements(), results.getTotalPages()));
    }

    /**
     * Get the complete profile of a soul with all related data in one view.
     * Includes: personal info, church info, assignments, presence history,
     * full timeline, notes, prayer requests, parallel followups, alerts, exits.
     */
    @GetMapping("/profile/{soulId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> getCompleteProfile(@PathVariable UUID soulId) {
        return ResponseEntity.ok(searchService.getCompleteProfile(soulId));
    }
}
