package com.discipolat.modules.streaming.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LiveStreamService {

    private final LiveStreamRepository repository;

    public LiveStreamService(LiveStreamRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<LiveStream> listByTenant(UUID tenantId) {
        return repository.findByTenantIdOrderByScheduledAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<LiveStream> listLive(UUID tenantId) {
        return repository.findByTenantIdAndStatus(tenantId, LiveStream.StreamStatus.LIVE);
    }

    /**
     * V240 : le tenant et le créateur sont dérivés du contexte
     * authentifié, jamais du corps de la requête (l'ancienne signature
     * laissait le client les fournir — IDOR/élévation).
     */
    public LiveStream create(LiveStream stream, UUID actorId) {
        stream.setId(null);
        stream.setTenantId(TenantContext.requireTenantId());
        stream.setCreatedBy(actorId);
        if (stream.getStatus() == null) {
            stream.setStatus(LiveStream.StreamStatus.SCHEDULED);
        }
        if (stream.getViewerCount() == null) {
            stream.setViewerCount(0);
        }
        return repository.save(stream);
    }

    /**
     * Chargement scopé tenant : un id appartenant à un autre tenant est
     * un 404 (EntityNotFoundException), jamais un 403 qui fuiterait
     * l'existence de la ressource.
     */
    @Transactional(readOnly = true)
    public LiveStream get(UUID tenantId, Long id) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("LiveStream", "id", String.valueOf(id)));
    }

    public LiveStream goLive(UUID tenantId, Long id) {
        LiveStream stream = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("LiveStream", "id", String.valueOf(id)));
        stream.setStatus(LiveStream.StreamStatus.LIVE);
        stream.setStartedAt(LocalDateTime.now());
        return repository.save(stream);
    }

    public LiveStream endStream(UUID tenantId, Long id) {
        LiveStream stream = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("LiveStream", "id", String.valueOf(id)));
        stream.setStatus(LiveStream.StreamStatus.ENDED);
        stream.setEndedAt(LocalDateTime.now());
        return repository.save(stream);
    }

    public LiveStream incrementViewers(UUID tenantId, Long id) {
        LiveStream stream = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("LiveStream", "id", String.valueOf(id)));
        int current = stream.getViewerCount() == null ? 0 : stream.getViewerCount();
        stream.setViewerCount(current + 1);
        return repository.save(stream);
    }

    /**
     * V240 : mise à jour des seuls champs éditables. Le statut ne se touche
     * pas ici (transitions dédiées go-live/end) ; id, tenant et créateur
     * sont inertes par construction (chargement scopé + updatable=false).
     */
    public LiveStream update(UUID tenantId, Long id, LiveStream body) {
        LiveStream stream = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("LiveStream", "id", String.valueOf(id)));
        if (body.getTitle() == null || body.getTitle().isBlank()) {
            throw new IllegalArgumentException("Le titre du stream est requis");
        }
        stream.setTitle(body.getTitle());
        stream.setDescription(body.getDescription());
        stream.setStreamUrl(body.getStreamUrl());
        stream.setThumbnailUrl(body.getThumbnailUrl());
        stream.setRecordingUrl(body.getRecordingUrl());
        if (body.getScheduledAt() != null && stream.getStatus() != LiveStream.StreamStatus.LIVE) {
            stream.setScheduledAt(body.getScheduledAt());
        }
        return repository.save(stream);
    }

    /**
     * Suppression scopée tenant. Un stream en direct doit être arrêté
     * d'abord (sinon les clients websocket resteraient sur un flux mort).
     */
    public void delete(UUID tenantId, Long id) {
        LiveStream stream = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("LiveStream", "id", String.valueOf(id)));
        if (stream.getStatus() == LiveStream.StreamStatus.LIVE) {
            throw new IllegalArgumentException("Arrêtez le stream avant de le supprimer");
        }
        repository.delete(stream);
    }
}
