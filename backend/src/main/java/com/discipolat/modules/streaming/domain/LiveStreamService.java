package com.discipolat.modules.streaming.domain;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LiveStreamService {

    private final LiveStreamRepository repository;

    public LiveStreamService(LiveStreamRepository repository) {
        this.repository = repository;
    }

    public List<LiveStream> listByTenant(Long tenantId) {
        return repository.findByTenantIdOrderByScheduledAtDesc(tenantId);
    }

    public List<LiveStream> listLive(Long tenantId) {
        return repository.findByTenantIdAndStatus(tenantId, LiveStream.StreamStatus.LIVE);
    }

    public LiveStream create(LiveStream stream) {
        return repository.save(stream);
    }

    public LiveStream goLive(Long id) {
        LiveStream stream = repository.findById(id).orElseThrow();
        stream.setStatus(LiveStream.StreamStatus.LIVE);
        stream.setStartedAt(LocalDateTime.now());
        return repository.save(stream);
    }

    public LiveStream endStream(Long id) {
        LiveStream stream = repository.findById(id).orElseThrow();
        stream.setStatus(LiveStream.StreamStatus.ENDED);
        stream.setEndedAt(LocalDateTime.now());
        return repository.save(stream);
    }

    public LiveStream incrementViewers(Long id) {
        LiveStream stream = repository.findById(id).orElseThrow();
        stream.setViewerCount(stream.getViewerCount() + 1);
        return repository.save(stream);
    }
}
