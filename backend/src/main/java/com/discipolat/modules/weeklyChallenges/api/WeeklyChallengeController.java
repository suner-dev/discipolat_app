package com.discipolat.modules.weeklyChallenges.api;

import com.discipolat.modules.weeklyChallenges.domain.WeeklyChallenge;
import com.discipolat.modules.weeklyChallenges.domain.WeeklyChallengeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/weekly-challenges")
public class WeeklyChallengeController {

    private final WeeklyChallengeService challengeService;

    public WeeklyChallengeController(WeeklyChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(challengeService.listActive());
    }

    @GetMapping("/my")
    public ResponseEntity<?> listMy() {
        return ResponseEntity.ok(challengeService.listMyChallenges());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody WeeklyChallenge challenge) {
        return ResponseEntity.status(HttpStatus.CREATED).body(challengeService.create(challenge));
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<?> updateProgress(@PathVariable UUID id, @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(challengeService.updateProgress(id, body.get("progress")));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate() {
        return ResponseEntity.status(HttpStatus.CREATED).body(challengeService.generateWeeklyChallenges());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(challengeService.getStats());
    }
}
