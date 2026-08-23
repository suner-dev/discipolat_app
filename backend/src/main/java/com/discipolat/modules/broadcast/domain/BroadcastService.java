package com.discipolat.modules.broadcast.domain;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BroadcastService {

    private final BroadcastMessageRepository repository;

    public BroadcastService(BroadcastMessageRepository repository) {
        this.repository = repository;
    }

    public List<BroadcastMessage> list(Long tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public BroadcastMessage send(BroadcastMessage message) {
        message.setSentAt(LocalDateTime.now());
        // In production: dispatch via push, email, SMS
        return repository.save(message);
    }

    public BroadcastMessage markRead(Long id, int totalRecipients) {
        BroadcastMessage msg = repository.findById(id).orElseThrow();
        msg.setReadCount(msg.getReadCount() + 1);
        msg.setTotalRecipients(totalRecipients);
        return repository.save(msg);
    }
}
