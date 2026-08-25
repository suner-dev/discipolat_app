package com.discipolat.modules.rewards.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.rewards.domain.Reward;
import com.discipolat.modules.rewards.domain.RewardService;
import com.discipolat.modules.rewards.domain.UserRewardClaim;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rewards")
@PreAuthorize("isAuthenticated()")
public class RewardController {

    private final RewardService service;

    public RewardController(RewardService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Reward>> list() {
        return ResponseEntity.ok(service.listAvailable(TenantContext.getTenantId().getLeastSignificantBits()));
    }

    @PostMapping
    public ResponseEntity<Reward> create(@RequestBody Reward reward) {
        return ResponseEntity.ok(service.create(reward));
    }

    @GetMapping("/my-claims")
    public ResponseEntity<List<UserRewardClaim>> myClaims() {
        Long userId = SecurityUtils.getCurrentUserId().getLeastSignificantBits();
        Long tenantId = TenantContext.getTenantId().getLeastSignificantBits();
        return ResponseEntity.ok(service.userClaims(userId, tenantId));
    }

    @PostMapping("/claim")
    public ResponseEntity<UserRewardClaim> claim(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId().getLeastSignificantBits();
        Long rewardId = ((Number) body.get("rewardId")).longValue();
        Long tenantId = TenantContext.getTenantId().getLeastSignificantBits();
        int totalPoints = (int) body.get("totalPoints");
        return ResponseEntity.ok(service.claim(userId, rewardId, tenantId, totalPoints));
    }
}
