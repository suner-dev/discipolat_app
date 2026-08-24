package com.discipolat.modules.groupMessages.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class GroupMessageService {

    private final GroupMessageRepository messageRepo;

    public GroupMessageService(GroupMessageRepository messageRepo) {
        this.messageRepo = messageRepo;
    }

    public List<GroupMessage> getGroupMessages(UUID groupId) {
        return messageRepo.findByTenantIdAndGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(
                TenantContext.getCurrentTenantId(), groupId);
    }

    public GroupMessage sendMessage(GroupMessage message) {
        message.setTenantId(TenantContext.getCurrentTenantId());
        message.setCreatedAt(LocalDateTime.now());
        return messageRepo.save(message);
    }

    public GroupMessage addReaction(UUID messageId) {
        GroupMessage msg = messageRepo.findById(messageId).orElseThrow();
        msg.setReactionCount(msg.getReactionCount() + 1);
        return messageRepo.save(msg);
    }

    public void deleteMessage(UUID id) {
        GroupMessage msg = messageRepo.findById(id).orElseThrow();
        msg.setIsDeleted(true);
        messageRepo.save(msg);
    }

    public Map<String, Object> getGroupStats(UUID groupId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMessages", messageRepo.countByTenantIdAndGroupIdAndIsDeletedFalse(tenantId, groupId));
        return stats;
    }


    /** P10 — Recherche plein-texte simple dans les messages d'un groupe. */
    @Transactional(readOnly = true)
    public List<GroupMessage> search(UUID groupId, String q) {
        UUID tenantId = TenantContext.getTenantId();
        return messageRepo.findByTenantIdAndGroupIdAndContentContainingIgnoreCaseAndIsDeletedFalseOrderByCreatedAtDesc(
                tenantId, groupId, q).stream().limit(50).toList();
    }
}