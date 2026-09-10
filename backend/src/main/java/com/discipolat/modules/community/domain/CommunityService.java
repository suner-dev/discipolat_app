package com.discipolat.modules.community.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CommunityService {

    private final CommunityRepository communityRepo;
    private final CommunityPostRepository postRepo;

    public CommunityService(CommunityRepository communityRepo, CommunityPostRepository postRepo) {
        this.communityRepo = communityRepo;
        this.postRepo = postRepo;
    }

    // ==================== COMMUNITY CRUD ====================

    public Community findById(UUID id) {
        return communityRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Community not found: " + id));
    }

    public List<Community> listAll() {
        return communityRepo.findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    public List<Community> listByCategory(String category) {
        return communityRepo.findByTenantIdAndCategoryAndIsActiveTrueOrderByCreatedAtDesc(TenantContext.getCurrentTenantId(), category);
    }

    public Community create(Community community) {
        community.setTenantId(TenantContext.getCurrentTenantId());
        community.setMemberCount(1);
        community.setPostCount(0);
        return communityRepo.save(community);
    }

    public Community update(UUID id, Community updated) {
        Community community = findById(id);
        community.setName(updated.getName());
        community.setDescription(updated.getDescription());
        community.setCategory(updated.getCategory());
        community.setVisibility(updated.getVisibility());
        return communityRepo.save(community);
    }

    public void delete(UUID id) {
        Community community = findById(id);
        community.setIsActive(false);
        communityRepo.save(community);
    }

    // ==================== POST CRUD ====================

    public CommunityPost findPostById(UUID id) {
        return postRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CommunityPost not found: " + id));
    }

    public List<CommunityPost> listPostsByCommunity(UUID communityId) {
        return postRepo.findByTenantIdAndCommunityIdAndIsActiveTrueOrderByIsPinnedDescCreatedAtDesc(
                TenantContext.getCurrentTenantId(), communityId);
    }

    public CommunityPost createPost(CommunityPost post) {
        post.setTenantId(TenantContext.getCurrentTenantId());
        Community community = findById(post.getCommunityId());
        community.setPostCount(community.getPostCount() + 1);
        communityRepo.save(community);
        return postRepo.save(post);
    }

    public CommunityPost updatePost(UUID id, CommunityPost updated) {
        CommunityPost post = findPostById(id);
        post.setTitle(updated.getTitle());
        post.setContent(updated.getContent());
        post.setPostType(updated.getPostType());
        return postRepo.save(post);
    }

    public void deletePost(UUID id) {
        CommunityPost post = findPostById(id);
        post.setIsActive(false);
        postRepo.save(post);
        Community community = findById(post.getCommunityId());
        community.setPostCount(Math.max(0, community.getPostCount() - 1));
        communityRepo.save(community);
    }

    public CommunityPost likePost(UUID id) {
        CommunityPost post = findPostById(id);
        post.setLikeCount(post.getLikeCount() + 1);
        return postRepo.save(post);
    }

    public CommunityPost pinPost(UUID id) {
        CommunityPost post = findPostById(id);
        post.setIsPinned(true);
        return postRepo.save(post);
    }

    public CommunityPost unpinPost(UUID id) {
        CommunityPost post = findPostById(id);
        post.setIsPinned(false);
        return postRepo.save(post);
    }
}
