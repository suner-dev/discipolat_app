package com.discipolat.modules.publicapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Formulaire de contact public (portail église).
 * - POST /api/v1/public/contact → soumettre un message de contact
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicContactController {

    private static final Logger log = LoggerFactory.getLogger(PublicContactController.class);

    @PostMapping("/contact")
    public ResponseEntity<Map<String, String>> submitContact(@RequestBody Map<String, String> body) {
        log.info("[Public] Contact submitted from: {} <{}>", body.get("nom"), body.get("email"));
        return ResponseEntity.ok(Map.of("status", "received", "message", "Merci pour votre message."));
    }
}
