package com.discipolat.modules.streaming.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StreamChatMessageService {

    private final StreamChatMessageRepository repository;
    private final LiveStreamRepository liveStreamRepository;

    public StreamChatMessageService(StreamChatMessageRepository repository,
                                    LiveStreamRepository liveStreamRepository) {
        this.repository = repository;
        this.liveStreamRepository = liveStreamRepository;
    }

    @Transactional(readOnly = true)
    public List<StreamChatMessage> listByStream(Long streamId) {
        UUID tenantId = TenantContext.requireTenantId();
        return repository.findByStreamIdAndTenantIdOrderByCreatedAtAsc(streamId, tenantId);
    }

    public StreamChatMessage send(Long streamId, UUID senderId, String senderName, String content, String emoji) {
        UUID tenantId = TenantContext.requireTenantId();
        // Anti-IDOR : interdit d'écrire dans le chat d'un stream qui
        // n'appartient pas au tenant courant (404, sans fuiter l'existence).
        liveStreamRepository.findByIdAndTenantId(streamId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("LiveStream", "id", String.valueOf(streamId)));

        StreamChatMessage msg = new StreamChatMessage();
        msg.setStreamId(streamId);
        msg.setTenantId(tenantId);
        msg.setSenderId(senderId);
        msg.setSenderName(senderName);
        msg.setContent(content);
        msg.setMessageType(emoji != null ? "REACTION" : "TEXT");
        msg.setEmoji(emoji);
        return repository.save(msg);
    }

    @Transactional(readOnly = true)
    public long countByStream(Long streamId) {
        UUID tenantId = TenantContext.requireTenantId();
        return repository.countByStreamIdAndTenantId(streamId, tenantId);
    }
}
