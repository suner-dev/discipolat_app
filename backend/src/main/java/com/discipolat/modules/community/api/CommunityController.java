package com.discipolat.modules.community.api;

import com.discipolat.modules.community.domain.Community;
import com.discipolat.modules.community.domain.CommunityPost;
import com.discipolat.modules.community.domain.CommunityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/communities", "/api/v1/communities"})
@PreAuthorize("isAuthenticated()")
public class CommunityController {

    private final CommunityService service;

    public CommunityController(CommunityService service) {
        this.service = service;
    }

    // ==================== COMMUNITY ENDPOINTS ====================

    @GetMapping
    public ResponseEntity<List<Community>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Community> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Community>> listByCategory(@PathVariable String category) {
        return ResponseEntity.ok(service.listByCategory(category));
    }

    @PostMapping
    public ResponseEntity<Community> create(@RequestBody Community community) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(community));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Community> update(@PathVariable UUID id, @RequestBody Community community) {
        return ResponseEntity.ok(service.update(id, community));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== POST ENDPOINTS ====================

    @GetMapping("/{communityId}/posts")
    public ResponseEntity<List<CommunityPost>> listPosts(@PathVariable UUID communityId) {
        return ResponseEntity.ok(service.listPostsByCommunity(communityId));
    }

    @PostMapping("/{communityId}/posts")
    public ResponseEntity<CommunityPost> createPost(@PathVariable UUID communityId, @RequestBody CommunityPost post) {
        post.setCommunityId(communityId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPost(post));
    }

    @PutMapping("/{communityId}/posts/{postId}")
    public ResponseEntity<CommunityPost> updatePost(@PathVariable UUID communityId, @PathVariable UUID postId, @RequestBody CommunityPost post) {
        return ResponseEntity.ok(service.updatePost(postId, post));
    }

    @DeleteMapping("/{communityId}/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID communityId, @PathVariable UUID postId) {
        service.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{communityId}/posts/{postId}/like")
    public ResponseEntity<CommunityPost> likePost(@PathVariable UUID communityId, @PathVariable UUID postId) {
        return ResponseEntity.ok(service.likePost(postId));
    }

    @PostMapping("/{communityId}/posts/{postId}/pin")
    public ResponseEntity<CommunityPost> pinPost(@PathVariable UUID communityId, @PathVariable UUID postId) {
        return ResponseEntity.ok(service.pinPost(postId));
    }

    @PostMapping("/{communityId}/posts/{postId}/unpin")
    public ResponseEntity<CommunityPost> unpinPost(@PathVariable UUID communityId, @PathVariable UUID postId) {
        return ResponseEntity.ok(service.unpinPost(postId));
    }
}
