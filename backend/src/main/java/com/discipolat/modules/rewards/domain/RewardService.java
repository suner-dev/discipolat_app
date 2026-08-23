package com.discipolat.modules.rewards.domain;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RewardService {

    private final RewardRepository rewardRepo;
    private final UserRewardClaimRepository claimRepo;

    public RewardService(RewardRepository rewardRepo, UserRewardClaimRepository claimRepo) {
        this.rewardRepo = rewardRepo;
        this.claimRepo = claimRepo;
    }

    public List<Reward> listAvailable(Long tenantId) {
        return rewardRepo.findByTenantIdAndIsActiveTrue(tenantId);
    }

    public List<UserRewardClaim> userClaims(Long userId, Long tenantId) {
        return claimRepo.findByUserIdAndTenantIdOrderByClaimedAtDesc(userId, tenantId);
    }

    public Reward create(Reward reward) {
        return rewardRepo.save(reward);
    }

    public UserRewardClaim claim(Long userId, Long rewardId, Long tenantId, int totalPoints) {
        Reward reward = rewardRepo.findById(rewardId).orElseThrow();
        if (totalPoints < reward.getPointsRequired()) {
            throw new IllegalStateException("Insufficient points");
        }
        UserRewardClaim claim = new UserRewardClaim();
        claim.setUserId(userId);
        claim.setRewardId(rewardId);
        claim.setTenantId(tenantId);
        claim.setPointsSpent(reward.getPointsRequired());
        claim.setTotalPoints(totalPoints);
        return claimRepo.save(claim);
    }
}
