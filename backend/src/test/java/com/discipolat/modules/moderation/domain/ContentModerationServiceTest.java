package com.discipolat.modules.moderation.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentModerationServiceTest {

    @Mock
    private ModerationItemRepository moderationRepo;

    private ContentModerationService moderationService;
    private UUID tenantId;
    private UUID reviewerId;

    @BeforeEach
    void setUp() {
        moderationService = new ContentModerationService(moderationRepo);
        tenantId = UUID.randomUUID();
        reviewerId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        lenient().when(moderationRepo.save(any(ModerationItem.class))).thenAnswer(inv -> {
            ModerationItem item = inv.getArgument(0);
            if (item.getId() == null) item.setId(UUID.randomUUID());
            return item;
        });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── SAFE CONTENT → AUTO-APPROVE ────────────────────────────

    @Test
    void submitForModeration_safeContent_shouldAutoApprove() {
        ModerationItem item = new ModerationItem();
        item.setContent("Merci Seigneur pour cette bénédiction");
        item.setSource(ModerationItem.Source.PRAYER);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertEquals(ModerationItem.Status.APPROVED, result.getStatus());
        assertEquals(ModerationItem.RiskLevel.LOW, result.getRiskLevel());
        assertTrue(result.getAiConfidence() >= 0.8);
        assertEquals("[]", result.getAiFlags());
    }

    @Test
    void submitForModeration_emptyContent_shouldBeLowRisk() {
        ModerationItem item = new ModerationItem();
        item.setContent("");
        item.setSource(ModerationItem.Source.COMMENT);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertEquals(ModerationItem.RiskLevel.LOW, result.getRiskLevel());
        assertEquals(ModerationItem.Status.APPROVED, result.getStatus());
    }

    @Test
    void submitForModeration_nullContent_shouldBeLowRisk() {
        ModerationItem item = new ModerationItem();
        item.setContent(null);
        item.setSource(ModerationItem.Source.COMMENT);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertEquals(ModerationItem.RiskLevel.LOW, result.getRiskLevel());
    }

    // ── SPAM DETECTION ─────────────────────────────────────────

    @Test
    void submitForModeration_spam_shouldFlagAndReduceScore() {
        ModerationItem item = new ModerationItem();
        item.setContent("Buy now! Click here for free money! Act now limited time!");
        item.setSource(ModerationItem.Source.MESSAGE);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertTrue(result.getAiFlags().contains("spam"));
        assertTrue(result.getAiConfidence() < 0.8);
    }

    // ── HARASSMENT DETECTION ───────────────────────────────────

    @Test
    void submitForModeration_harassment_shouldFlagAndReduceScore() {
        ModerationItem item = new ModerationItem();
        item.setContent("You are so stupid and I hate you, you worthless loser");
        item.setSource(ModerationItem.Source.COMMENT);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertTrue(result.getAiFlags().contains("harassment"));
        assertTrue(result.getAiConfidence() < 0.6);
    }

    // ── INAPPROPRIATE CONTENT ──────────────────────────────────

    @Test
    void submitForModeration_inappropriate_shouldFlag() {
        ModerationItem item = new ModerationItem();
        item.setContent("Check out these alcohol and gambling tips for your next party");
        item.setSource(ModerationItem.Source.TESTIMONY);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertTrue(result.getAiFlags().contains("inappropriate"));
    }

    // ── CRITICAL CONTENT → AUTO-REJECT ─────────────────────────

    @Test
    void submitForModeration_criticalContent_shouldAutoReject() {
        ModerationItem item = new ModerationItem();
        item.setContent("Buy now click here winner congratulations you won stupid idiot kill yourself");
        item.setSource(ModerationItem.Source.MESSAGE);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertEquals(ModerationItem.Status.REJECTED, result.getStatus());
        assertEquals(ModerationItem.RiskLevel.CRITICAL, result.getRiskLevel());
    }

    // ── MEDIUM RISK → PENDING ──────────────────────────────────

    @Test
    void submitForModeration_mediumRisk_shouldStayPending() {
        ModerationItem item = new ModerationItem();
        // Content with just spam — risk level should be MEDIUM (score ~0.6)
        item.setContent("Buy now click here free money winner");
        item.setSource(ModerationItem.Source.MESSAGE);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertEquals(ModerationItem.Status.PENDING, result.getStatus());
        assertEquals(ModerationItem.RiskLevel.MEDIUM, result.getRiskLevel());
    }

    // ── EXCESSIVE CAPS ─────────────────────────────────────────

    @Test
    void submitForModeration_excessiveCaps_shouldFlag() {
        ModerationItem item = new ModerationItem();
        item.setContent("THIS IS A VERY LONG MESSAGE WRITTEN ENTIRELY IN CAPITAL LETTERS THAT SHOULD TRIGGER THE CAPS DETECTION");
        item.setSource(ModerationItem.Source.MESSAGE);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertTrue(result.getAiFlags().contains("excessive_caps"));
    }

    // ── UNUSUALLY LONG CONTENT ─────────────────────────────────

    @Test
    void submitForModeration_veryLongContent_shouldFlag() {
        ModerationItem item = new ModerationItem();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6000; i++) sb.append("word ");
        item.setContent(sb.toString());
        item.setSource(ModerationItem.Source.RAPPORT);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertTrue(result.getAiFlags().contains("unusually_long"));
    }

    // ── MIXED FLAGS ────────────────────────────────────────────

    @Test
    void submitForModeration_multipleIssues_shouldCombineFlags() {
        ModerationItem item = new ModerationItem();
        item.setContent("Buy now stupid idiot free money hate you");
        item.setSource(ModerationItem.Source.MESSAGE);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertTrue(result.getAiFlags().contains("spam"));
        assertTrue(result.getAiFlags().contains("harassment"));
    }

    // ── TENANT ID ──────────────────────────────────────────────

    @Test
    void submitForModeration_shouldSetTenantId() {
        ModerationItem item = new ModerationItem();
        item.setContent("Normal content");
        item.setSource(ModerationItem.Source.COMMENT);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertEquals(tenantId, result.getTenantId());
    }

    // ── REVIEW ─────────────────────────────────────────────────

    @Test
    void reviewItem_shouldSetDecisionAndReviewer() {
        UUID itemId = UUID.randomUUID();
        ModerationItem item = new ModerationItem();
        item.setId(itemId);
        item.setStatus(ModerationItem.Status.PENDING);
        when(moderationRepo.findById(itemId)).thenReturn(Optional.of(item));

        ModerationItem reviewed = moderationService.reviewItem(
                itemId, ModerationItem.Status.APPROVED, "Contenu acceptable", reviewerId);

        assertEquals(ModerationItem.Status.APPROVED, reviewed.getStatus());
        assertEquals("Contenu acceptable", reviewed.getModeratorNotes());
        assertEquals(reviewerId, reviewed.getReviewedBy());
        assertNotNull(reviewed.getReviewedAt());
    }

    @Test
    void reviewItem_rejectWithNotes() {
        UUID itemId = UUID.randomUUID();
        ModerationItem item = new ModerationItem();
        item.setId(itemId);
        item.setStatus(ModerationItem.Status.PENDING);
        when(moderationRepo.findById(itemId)).thenReturn(Optional.of(item));

        ModerationItem reviewed = moderationService.reviewItem(
                itemId, ModerationItem.Status.REJECTED, "Contenu non conforme", reviewerId);

        assertEquals(ModerationItem.Status.REJECTED, reviewed.getStatus());
    }

    // ── LIST ───────────────────────────────────────────────────

    @Test
    void listPending_shouldReturnPendingItems() {
        ModerationItem pending = new ModerationItem();
        pending.setStatus(ModerationItem.Status.PENDING);
        when(moderationRepo.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, ModerationItem.Status.PENDING))
                .thenReturn(List.of(pending));

        List<ModerationItem> result = moderationService.listPending();

        assertEquals(1, result.size());
        assertEquals(ModerationItem.Status.PENDING, result.get(0).getStatus());
    }

    @Test
    void listAll_shouldReturnAllItems() {
        when(moderationRepo.findByTenantIdOrderByCreatedAtDesc(tenantId))
                .thenReturn(List.of(new ModerationItem(), new ModerationItem()));

        List<ModerationItem> result = moderationService.listAll();

        assertEquals(2, result.size());
    }

    // ── STATS ──────────────────────────────────────────────────

    @Test
    void getStats_shouldReturnAllCounts() {
        when(moderationRepo.countByTenantIdAndStatus(tenantId, ModerationItem.Status.PENDING)).thenReturn(10L);
        when(moderationRepo.countByTenantIdAndStatus(tenantId, ModerationItem.Status.APPROVED)).thenReturn(50L);
        when(moderationRepo.countByTenantIdAndStatus(tenantId, ModerationItem.Status.REJECTED)).thenReturn(5L);
        when(moderationRepo.countByTenantIdAndStatus(tenantId, ModerationItem.Status.FLAGGED)).thenReturn(3L);
        when(moderationRepo.countByTenantIdAndRiskLevel(tenantId, ModerationItem.RiskLevel.HIGH)).thenReturn(2L);
        when(moderationRepo.countByTenantIdAndRiskLevel(tenantId, ModerationItem.RiskLevel.CRITICAL)).thenReturn(1L);

        Map<String, Object> stats = moderationService.getStats();

        assertEquals(10L, stats.get("pending"));
        assertEquals(50L, stats.get("approved"));
        assertEquals(5L, stats.get("rejected"));
        assertEquals(3L, stats.get("flagged"));
        assertEquals(2L, stats.get("highRisk"));
        assertEquals(1L, stats.get("criticalRisk"));
    }

    // ── CONFIDENCE SCORING ─────────────────────────────────────

    @Test
    void submitForModeration_cleanContent_highConfidence() {
        ModerationItem item = new ModerationItem();
        item.setContent("Bonjour, je voudrais m'inscrire à l'événement de demain.");
        item.setSource(ModerationItem.Source.MESSAGE);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertTrue(result.getAiConfidence() >= 0.9);
    }

    @Test
    void submitForModeration_spamContent_lowerConfidence() {
        ModerationItem item = new ModerationItem();
        item.setContent("Buy now! Click here! Free money winner!");
        item.setSource(ModerationItem.Source.MESSAGE);
        item.setAuthorId(UUID.randomUUID());

        ModerationItem result = moderationService.submitForModeration(item);

        assertTrue(result.getAiConfidence() < 0.7);
    }
}
