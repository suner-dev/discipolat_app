package com.discipolat.modules.messages.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.exception.BadRequestException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.messages.api.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced messaging service with group conversations, voice messages,
 * reactions, threads (replies), message search, and read receipts.
 */
@Service
@Transactional
public class EnhancedMessageService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final GroupConversationRepository groupConversationRepository;
    private final GroupConversationMemberRepository groupMemberRepository;
    private final MessageReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public EnhancedMessageService(
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            GroupConversationRepository groupConversationRepository,
            GroupConversationMemberRepository groupMemberRepository,
            MessageReactionRepository reactionRepository,
            UserRepository userRepository,
            SecurityUtils securityUtils) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.groupConversationRepository = groupConversationRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.reactionRepository = reactionRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    // ============================================================
    // GROUP CONVERSATIONS
    // ============================================================

    /** Create a group conversation */
    public GroupConversationResponse createGroup(String name, String description, String groupType, List<UUID> memberIds) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        GroupConversation group = groupConversationRepository.save(
                GroupConversation.builder()
                        .tenantId(securityUtils.getCurrentTenantId())
                        .name(name)
                        .description(description)
                        .groupType(groupType != null ? groupType : "TEAM")
                        .createdBy(currentUserId)
                        .build()
        );

        // Add creator as admin
        groupMemberRepository.save(GroupConversationMember.builder()
                .groupId(group.getId())
                .userId(currentUserId)
                .role("ADMIN")
                .build());

        // Add other members
        if (memberIds != null) {
            for (UUID memberId : memberIds) {
                if (!memberId.equals(currentUserId)) {
                    groupMemberRepository.save(GroupConversationMember.builder()
                            .groupId(group.getId())
                            .userId(memberId)
                            .role("MEMBER")
                            .build());
                }
            }
        }

        return toGroupResponse(group, currentUserId);
    }

    /** Get all group conversations for current user */
    @Transactional(readOnly = true)
    public List<GroupConversationResponse> listMyGroups() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        List<GroupConversation> groups = groupConversationRepository.findByMemberUserId(currentUserId);
        return groups.stream()
                .map(g -> toGroupResponse(g, currentUserId))
                .toList();
    }

    /** Get a specific group conversation */
    @Transactional(readOnly = true)
    public GroupConversationResponse getGroup(UUID groupId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        GroupConversation group = groupConversationRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("GroupConversation", groupId));
        assertGroupMember(groupId, currentUserId);
        return toGroupResponse(group, currentUserId);
    }

    /** Add members to a group */
    public void addMembers(UUID groupId, List<UUID> userIds) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        assertGroupAdmin(groupId, currentUserId);

        for (UUID userId : userIds) {
            if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
                groupMemberRepository.save(GroupConversationMember.builder()
                        .groupId(groupId)
                        .userId(userId)
                        .role("MEMBER")
                        .build());
            }
        }
    }

    /** Remove a member from a group */
    public void removeMember(UUID groupId, UUID userId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        assertGroupAdmin(groupId, currentUserId);
        groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .ifPresent(groupMemberRepository::delete);
    }

    /** Send a message to a group */
    public MessageResponse sendGroupMessage(UUID groupId, String content, String messageType, String mediaUrl, UUID replyToId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        assertGroupMember(groupId, currentUserId);

        String replySenderName = null;
        String replyContent = null;
        if (replyToId != null) {
            ConversationMessage replyMsg = messageRepository.findById(replyToId).orElse(null);
            if (replyMsg != null) {
                replySenderName = fullName(replyMsg.getSenderId());
                replyContent = replyMsg.getContent();
            }
        }

        ConversationMessage msg = messageRepository.save(ConversationMessage.builder()
                .tenantId(securityUtils.getCurrentTenantId())
                .groupId(groupId)
                .senderId(currentUserId)
                .content(content)
                .messageType(messageType != null ? messageType : "TEXT")
                .mediaUrl(mediaUrl)
                .replyToId(replyToId)
                .replyToSenderName(replySenderName)
                .replyToContent(replyContent)
                .build());

        // Update group last message
        GroupConversation group = groupConversationRepository.findById(groupId).orElseThrow();
        group.setLastMessage(content != null ? content : "[" + (messageType != null ? messageType : "message") + "]");
        group.setLastMessageAt(LocalDateTime.now());
        group.setLastMessageSenderId(currentUserId);
        groupConversationRepository.save(group);

        // Increment unread for other members
        groupMemberRepository.incrementUnreadCount(groupId, currentUserId);

        return MessageResponse.from(msg, fullName(currentUserId));
    }

    /** Get messages for a group conversation */
    @Transactional(readOnly = true)
    public List<MessageResponse> getGroupMessages(UUID groupId, Pageable pageable) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        assertGroupMember(groupId, currentUserId);

        // Mark as read
        groupMemberRepository.markAsRead(groupId, currentUserId, LocalDateTime.now());

        List<ConversationMessage> msgs = messageRepository.findByGroupIdOrderByCreatedAtAsc(groupId);
        return msgs.stream()
                .map(m -> {
                    MessageResponse resp = MessageResponse.from(m, fullName(m.getSenderId()));
                    // Attach reactions
                    List<MessageReaction> reactions = reactionRepository.findByMessageIdOrderByCreatedAtAsc(m.getId());
                    Map<String, Long> reactionCounts = reactions.stream()
                            .collect(Collectors.groupingBy(MessageReaction::getEmoji, Collectors.counting()));
                    resp.setReactionCounts(reactionCounts);
                    resp.setUserReaction(reactions.stream()
                            .filter(r -> r.getUserId().equals(currentUserId))
                            .map(MessageReaction::getEmoji)
                            .findFirst().orElse(null));
                    return resp;
                })
                .toList();
    }

    // ============================================================
    // VOICE MESSAGES
    // ============================================================

    /** Send a voice message */
    public MessageResponse sendVoiceMessage(UUID conversationId, String audioUrl, Integer durationSeconds) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));
        assertParticipant(conv, currentUserId);

        ConversationMessage msg = messageRepository.save(ConversationMessage.builder()
                .tenantId(securityUtils.getCurrentTenantId())
                .conversationId(conversationId)
                .senderId(currentUserId)
                .content("🎵 Message vocal")
                .messageType("VOICE")
                .mediaUrl(audioUrl)
                .mediaDuration(durationSeconds)
                .build());

        conv.setLastMessage("🎵 Message vocal");
        conv.setLastMessageAt(LocalDateTime.now());
        conv.setLastMessageSenderId(currentUserId);
        conversationRepository.save(conv);

        return MessageResponse.from(msg, fullName(currentUserId));
    }

    /** Send a voice message to a group */
    public MessageResponse sendGroupVoiceMessage(UUID groupId, String audioUrl, Integer durationSeconds) {
        return sendGroupMessage(groupId, "🎵 Message vocal", "VOICE", audioUrl, null);
    }

    // ============================================================
    // REACTIONS
    // ============================================================

    /** Toggle a reaction on a message */
    public Map<String, Object> toggleReaction(UUID messageId, String emoji) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Optional<MessageReaction> existing = reactionRepository.findByMessageIdAndUserId(messageId, currentUserId);

        boolean added;
        if (existing.isPresent()) {
            if (existing.get().getEmoji().equals(emoji)) {
                // Same emoji — remove it
                reactionRepository.delete(existing.get());
                added = false;
            } else {
                // Different emoji — update
                existing.get().setEmoji(emoji);
                reactionRepository.save(existing.get());
                added = true;
            }
        } else {
            // New reaction
            reactionRepository.save(MessageReaction.builder()
                    .messageId(messageId)
                    .userId(currentUserId)
                    .emoji(emoji)
                    .build());
            added = true;
        }

        // Get updated counts
        List<MessageReaction> reactions = reactionRepository.findByMessageIdOrderByCreatedAtAsc(messageId);
        Map<String, Long> counts = reactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getEmoji, Collectors.counting()));
        String userReaction = reactions.stream()
                .filter(r -> r.getUserId().equals(currentUserId))
                .map(MessageReaction::getEmoji)
                .findFirst().orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("added", added);
        result.put("reactionCounts", counts);
        result.put("userReaction", userReaction);
        return result;
    }

    /** Get reactions for a message */
    @Transactional(readOnly = true)
    public Map<String, Object> getReactions(UUID messageId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        List<MessageReaction> reactions = reactionRepository.findByMessageIdOrderByCreatedAtAsc(messageId);
        Map<String, Long> counts = reactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getEmoji, Collectors.counting()));
        String userReaction = reactions.stream()
                .filter(r -> r.getUserId().equals(currentUserId))
                .map(MessageReaction::getEmoji)
                .findFirst().orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("reactionCounts", counts);
        result.put("userReaction", userReaction);
        result.put("totalReactions", reactions.size());
        return result;
    }

    // ============================================================
    // THREADS (REPLIES)
    // ============================================================

    /** Get replies to a specific message */
    @Transactional(readOnly = true)
    public List<MessageResponse> getReplies(UUID messageId) {
        List<ConversationMessage> replies = messageRepository.findByReplyToIdOrderByCreatedAtAsc(messageId);
        return replies.stream()
                .map(m -> MessageResponse.from(m, fullName(m.getSenderId())))
                .toList();
    }

    /** Send a reply to a specific message */
    public MessageResponse sendReply(UUID conversationId, UUID replyToId, String content) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));
        assertParticipant(conv, currentUserId);

        ConversationMessage replyMsg = messageRepository.findById(replyToId)
                .orElseThrow(() -> new EntityNotFoundException("Message", replyToId));

        ConversationMessage msg = messageRepository.save(ConversationMessage.builder()
                .tenantId(securityUtils.getCurrentTenantId())
                .conversationId(conversationId)
                .senderId(currentUserId)
                .content(content)
                .replyToId(replyToId)
                .replyToSenderName(fullName(replyMsg.getSenderId()))
                .replyToContent(replyMsg.getContent())
                .build());

        conv.setLastMessage(content);
        conv.setLastMessageAt(LocalDateTime.now());
        conv.setLastMessageSenderId(currentUserId);
        conversationRepository.save(conv);

        return MessageResponse.from(msg, fullName(currentUserId));
    }

    // ============================================================
    // SEARCH
    // ============================================================

    /** Search messages by content across all conversations */
    @Transactional(readOnly = true)
    public Page<MessageResponse> searchMessages(String query, Pageable pageable) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        // Get all conversations the user participates in
        List<Conversation> convs = new ArrayList<>();
        convs.addAll(conversationRepository.findByUserAIdOrderByLastMessageAtDesc(currentUserId));
        convs.addAll(conversationRepository.findByUserBIdOrderByLastMessageAtDesc(currentUserId));
        Set<UUID> convIds = convs.stream().map(Conversation::getId).collect(Collectors.toSet());

        // Also get group conversations
        List<GroupConversation> groups = groupConversationRepository.findByMemberUserId(currentUserId);
        Set<UUID> groupIds = groups.stream().map(GroupConversation::getId).collect(Collectors.toSet());

        // Search in all relevant messages
        List<ConversationMessage> allResults = new ArrayList<>();
        for (UUID convId : convIds) {
            allResults.addAll(messageRepository.findByConversationIdOrderByCreatedAtDesc(convId));
        }
        for (UUID gId : groupIds) {
            allResults.addAll(messageRepository.findByGroupIdOrderByCreatedAtAsc(gId));
        }

        // Filter by query
        String lowerQuery = query.toLowerCase();
        List<MessageResponse> filtered = allResults.stream()
                .filter(m -> m.getContent() != null && m.getContent().toLowerCase().contains(lowerQuery))
                .sorted(Comparator.comparing(ConversationMessage::getCreatedAt).reversed())
                .map(m -> MessageResponse.from(m, fullName(m.getSenderId())))
                .toList();

        // Paginate
        int start = (int) Math.min(pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    // ============================================================
    // ENHANCED 1:1 MESSAGES (with voice/reply support)
    // ============================================================

    /** Enhanced send message with type, media, and reply support */
    public MessageResponse sendEnhancedMessage(UUID conversationId, String content, String messageType, String mediaUrl, UUID replyToId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));
        assertParticipant(conv, currentUserId);

        String replySenderName = null;
        String replyContent = null;
        if (replyToId != null) {
            ConversationMessage replyMsg = messageRepository.findById(replyToId).orElse(null);
            if (replyMsg != null) {
                replySenderName = fullName(replyMsg.getSenderId());
                replyContent = replyMsg.getContent();
            }
        }

        ConversationMessage msg = messageRepository.save(ConversationMessage.builder()
                .tenantId(securityUtils.getCurrentTenantId())
                .conversationId(conversationId)
                .senderId(currentUserId)
                .content(content)
                .messageType(messageType != null ? messageType : "TEXT")
                .mediaUrl(mediaUrl)
                .replyToId(replyToId)
                .replyToSenderName(replySenderName)
                .replyToContent(replyContent)
                .build());

        conv.setLastMessage(content != null ? content : "[" + (messageType != null ? messageType : "message") + "]");
        conv.setLastMessageAt(LocalDateTime.now());
        conv.setLastMessageSenderId(currentUserId);
        conversationRepository.save(conv);

        MessageResponse resp = MessageResponse.from(msg, fullName(currentUserId));
        // Attach reactions
        List<MessageReaction> reactions = reactionRepository.findByMessageIdOrderByCreatedAtAsc(msg.getId());
        Map<String, Long> reactionCounts = reactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getEmoji, Collectors.counting()));
        resp.setReactionCounts(reactionCounts);
        resp.setUserReaction(reactions.stream()
                .filter(r -> r.getUserId().equals(currentUserId))
                .map(MessageReaction::getEmoji)
                .findFirst().orElse(null));
        return resp;
    }

    /** Get messages with reactions and reply info */
    @Transactional(readOnly = true)
    public List<MessageResponse> getEnhancedMessages(UUID conversationId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));
        assertParticipant(conv, currentUserId);

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(m -> {
                    MessageResponse resp = MessageResponse.from(m, fullName(m.getSenderId()));
                    List<MessageReaction> reactions = reactionRepository.findByMessageIdOrderByCreatedAtAsc(m.getId());
                    Map<String, Long> reactionCounts = reactions.stream()
                            .collect(Collectors.groupingBy(MessageReaction::getEmoji, Collectors.counting()));
                    resp.setReactionCounts(reactionCounts);
                    resp.setUserReaction(reactions.stream()
                            .filter(r -> r.getUserId().equals(currentUserId))
                            .map(MessageReaction::getEmoji)
                            .findFirst().orElse(null));
                    return resp;
                })
                .toList();
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private void assertParticipant(Conversation conv, UUID userId) {
        if (!conv.getUserAId().equals(userId) && !conv.getUserBId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Non participant");
        }
    }

    private void assertGroupMember(UUID groupId, UUID userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Non membre du groupe");
        }
    }

    private void assertGroupAdmin(UUID groupId, UUID userId) {
        GroupConversationMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Non membre du groupe"));
        if (!"ADMIN".equals(member.getRole())) {
            throw new org.springframework.security.access.AccessDeniedException("Non admin du groupe");
        }
    }

    private String fullName(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Utilisateur");
    }

    private GroupConversationResponse toGroupResponse(GroupConversation group, UUID currentUserId) {
        List<GroupConversationMember> members = groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId());
        int memberCount = members.size();
        long unread = members.stream()
                .filter(m -> m.getUserId().equals(currentUserId))
                .mapToLong(GroupConversationMember::getUnreadCount)
                .sum();
        boolean isMember = members.stream().anyMatch(m -> m.getUserId().equals(currentUserId));

        return GroupConversationResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .groupType(group.getGroupType())
                .createdBy(group.getCreatedBy())
                .avatarUrl(group.getAvatarUrl())
                .memberCount(memberCount)
                .unreadCount((int) unread)
                .isMember(isMember)
                .lastMessage(group.getLastMessage())
                .lastMessageAt(group.getLastMessageAt())
                .createdAt(group.getCreatedAt())
                .build();
    }
}
