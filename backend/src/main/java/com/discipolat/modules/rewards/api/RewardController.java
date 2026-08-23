package com.discipolat.modules.rewards.api;

import com.discipolat.modules.rewards.domain.Reward;
import com.discipolat.modules.rewards.domain.RewardService;
import com.discipolat.modules.rewards.domain.UserRewardClaim;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rewards")
public class RewardController {

    private final RewardService service;

    public RewardController(RewardService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Reward>> list(@RequestParam Long tenantId) {
        return ResponseEntity.ok(service.listAvailable(tenantId));
    }

    @PostMapping
    public ResponseEntity<Reward> create(@RequestBody Reward reward) {
        return ResponseEntity.ok(service.create(reward));
    }

    @GetMapping("/my-claims")
    public ResponseEntity<List<UserRewardClaim>> myClaims(
            @RequestParam Long userId,
            @RequestParam Long tenantId) {
        return ResponseEntity.ok(service.userClaims(userId, tenantId));
    }

    @PostMapping("/claim")
    public ResponseEntity<UserRewardClaim> claim(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        Long rewardId = ((Number) body.get("rewardId")).longValue();
        Long tenantId = ((Number) body.get("tenantId")).longValue();
        int totalPoints = (int) body.get("totalPoints");
        return ResponseEntity.ok(service.claim(userId, rewardId, tenantId, totalPoints));
    }
}
