package com.discipolat.modules.souls.api;

import com.discipolat.modules.souls.domain.SoulNote;
import com.discipolat.modules.souls.domain.SoulNoteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/souls/{soulId}/notes")
@PreAuthorize("@authz.can('SOULS_EXECUTE', 'TENANT', null)")
public class SoulNoteController {

    private final SoulNoteService soulNoteService;

    public SoulNoteController(SoulNoteService soulNoteService) {
        this.soulNoteService = soulNoteService;
    }

    @PostMapping
    @PreAuthorize("@authz.can('SOULS_EXECUTE_CREATE', 'TENANT', null)")
    public ResponseEntity<SoulNoteResponse> create(
            @PathVariable UUID soulId,
            @Valid @RequestBody CreateSoulNoteRequest request) {
        SoulNote note = SoulNote.builder()
                .ameId(soulId)
                .contenu(request.contenu())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SoulNoteResponse.from(soulNoteService.create(note)));
    }

    @GetMapping
    @PreAuthorize("@authz.can('SOULS_EXECUTE_FINDBYSOULID', 'TENANT', null)")
    public ResponseEntity<List<SoulNoteResponse>> findBySoulId(@PathVariable UUID soulId) {
        return ResponseEntity.ok(soulNoteService.findByAmeId(soulId)
                .stream().map(SoulNoteResponse::from).toList());
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("@authz.can('SOULS_EXECUTE_UPDATE', 'TENANT', null)")
    public ResponseEntity<SoulNoteResponse> update(
            @PathVariable UUID noteId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(SoulNoteResponse.from(
                soulNoteService.update(noteId, body.get("contenu"))));
    }

    @DeleteMapping("/{noteId}")
    @PreAuthorize("@authz.can('SOULS_EXECUTE_DELETE2', 'TENANT', null)")
    public ResponseEntity<Void> delete(@PathVariable UUID noteId) {
        soulNoteService.delete(noteId);
        return ResponseEntity.noContent().build();
    }
}
