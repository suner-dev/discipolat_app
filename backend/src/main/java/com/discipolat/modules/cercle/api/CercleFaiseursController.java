package com.discipolat.modules.cercle.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Cercle de Faiseurs — espace d'entraide entre faiseurs de disciples.
 * - GET  /api/v1/cercle-faiseurs          → lister les posts
 * - POST /api/v1/cercle-faiseurs          → créer un post
 * - POST /api/v1/cercle-faiseurs/{id}/like → liker un post
 */
@RestController
@RequestMapping("/api/v1/cercle-faiseurs")
@PreAuthorize("hasAnyRole('ADMIN','PASTEUR','FAISEUR')")
public class CercleFaiseursController {

    private static final Logger log = LoggerFactory.getLogger(CercleFaiseursController.class);

    private static final List<Map<String, Object>> POSTS = new java.util.ArrayList<>();

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@RequestParam(required = false) String categorie) {
        log.debug("[Cercle] list posts, categorie={}", categorie);
        return ResponseEntity.ok(POSTS);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Map<String, Object> post = new LinkedHashMap<>(body);
        post.put("id", UUID.randomUUID().toString());
        post.put("createdAt", java.time.Instant.now().toString());
        post.put("likes", 0);
        POSTS.add(post);
        log.info("[Cercle] post created: {}", post.get("id"));
        return ResponseEntity.status(201).body(post);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> like(@PathVariable String id) {
        for (Map<String, Object> post : POSTS) {
            if (id.equals(post.get("id"))) {
                post.put("likes", (int) post.getOrDefault("likes", 0) + 1);
                return ResponseEntity.ok(post);
            }
        }
        return ResponseEntity.notFound().build();
    }
}
