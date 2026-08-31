package com.discipolat.modules.streaming.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StreamChatMessageService {

    private final StreamChatMessageRepository repository;

    public StreamChatMessageService(StreamChatMessageRepository repository) {
        this.repository = repository;
    }

    public List<StreamChatMessage> listByStream(Long streamId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return repository.findByStreamIdAndTenantIdOrderByCreatedAtAsc(streamId, tenantId);
    }

    public StreamChatMessage send(Long streamId, UUID senderId, String senderName, String content, String emoji) {
        StreamChatMessage msg = new StreamChatMessage();
        msg.setStreamId(streamId);
        msg.setTenantId(TenantContext.getCurrentTenantId());
        msg.setSenderId(senderId);
        msg.setSenderName(senderName);
        msg.setContent(content);
        msg.setMessageType(emoji != null ? "REACTION" : "TEXT");
        msg.setEmoji(emoji);
        return repository.save(msg);
    }

    public long countByStream(Long streamId) {
        return repository.countByStreamId(streamId);
    }
}
