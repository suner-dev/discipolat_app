package com.discipolat.modules.spiritualJourney.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Parcours spirituel visuel — progression du disciple.
 * - GET /api/v1/spiritual-journey → étapes du parcours spirituel
 */
@RestController
@RequestMapping("/api/v1/spiritual-journey")
@PreAuthorize("isAuthenticated()")
public class SpiritualJourneyController {

    private static final Logger log = LoggerFactory.getLogger(SpiritualJourneyController.class);

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getJourney() {
        log.debug("[SpiritualJourney] get journey stages");
        List<Map<String, Object>> stages = List.of(
            Map.of("id", "1", "name", "Nouvelle Naissance", "description", "Conversion et nouvelle vie en Christ", "completed", true, "order", 1),
            Map.of("id", "2", "name", "Baptême", "description", "Baptême d'eau et confirmation de la foi", "completed", true, "order", 2),
            Map.of("id", "3", "name", "Remplissage du Saint-Esprit", "description", "Vie dans l'Esprit et fruits spirituels", "completed", false, "order", 3),
            Map.of("id", "4", "name", "Discipulat", "description", "Formation et croissance spirituelle", "completed", false, "order", 4),
            Map.of("id", "5", "name", "Service", "description", "Engagement dans le ministère", "completed", false, "order", 5),
            Map.of("id", "6", "name", "Leadership", "description", "Formation de disciples et multiplication", "completed", false, "order", 6)
        );
        return ResponseEntity.ok(stages);
    }
}
