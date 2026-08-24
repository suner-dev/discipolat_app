package com.discipolat.modules.directory.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.directory.domain.DirectoryEntry;
import com.discipolat.modules.directory.domain.DirectoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/directory")
public class DirectoryController {

    private final DirectoryService service;

    public DirectoryController(DirectoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PageResponse<DirectoryEntry>> listPublic(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<DirectoryEntry> entries = service.listPublic(PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(entries.getContent(), page, size,
                entries.getTotalElements(), entries.getTotalPages()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<DirectoryEntry>> listAllPublic() {
        return ResponseEntity.ok(service.listAllPublic());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DirectoryEntry> getMyEntry() {
        return ResponseEntity.ok(service.getOrCreate(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DirectoryEntry> updateMyEntry(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        DirectoryEntry entry = service.update(userId,
                (String) body.get("bio"),
                (String) body.get("téléphone"),
                (String) body.get("email"),
                (String) body.get("département"),
                (String) body.get("rôle"),
                body.get("publicProfil") != null ? (Boolean) body.get("publicProfil") : null
        );
        return ResponseEntity.ok(entry);
    }

    @PatchMapping("/me/toggle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> togglePublic() {
        service.togglePublic(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().build();
    }
}
