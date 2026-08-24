package com.discipolat.modules.broadcast.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class BroadcastService {

    private final BroadcastMessageRepository broadcastRepository;
    private final BroadcastReceiptRepository receiptRepository;

    public BroadcastService(BroadcastMessageRepository broadcastRepository, BroadcastReceiptRepository receiptRepository) {
        this.broadcastRepository = broadcastRepository;
        this.receiptRepository = receiptRepository;
    }

    public Page<BroadcastMessage> list(Pageable pageable, String statut) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (statut != null) {
            return broadcastRepository.findByTenantIdAndStatut(tenantId, BroadcastMessage.Statut.valueOf(statut), pageable);
        }
        return broadcastRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    public BroadcastMessage getById(UUID id) {
        return broadcastRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("BroadcastMessage", id));
    }

    public BroadcastMessage create(String titre, String contenu, String cible, String cibleIds, UUID userId) {
        BroadcastMessage message = new BroadcastMessage();
        message.setTenantId(TenantContext.getCurrentTenantId());
        message.setTitre(titre);
        message.setContenu(contenu);
        message.setCible(BroadcastMessage.Cible.valueOf(cible != null ? cible : "TOUS"));
        message.setCibleIds(cibleIds);
        message.setEnvoyéPar(userId);
        return broadcastRepository.save(message);
    }

    public BroadcastMessage schedule(UUID id, LocalDateTime programméAt) {
        BroadcastMessage message = getById(id);
        message.setStatut(BroadcastMessage.Statut.PROGRAMMÉ);
        message.setProgramméAt(programméAt);
        return broadcastRepository.save(message);
    }

    public BroadcastMessage send(UUID id) {
        BroadcastMessage message = getById(id);
        message.setStatut(BroadcastMessage.Statut.ENVOYÉ);
        message.setEnvoyéAt(LocalDateTime.now());
        // In production, this would trigger actual delivery via notification service
        return broadcastRepository.save(message);
    }

    public void markAsRead(UUID broadcastId, UUID membreId) {
        BroadcastMessage message = getById(broadcastId);
        BroadcastReceipt receipt = new BroadcastReceipt();
        receipt.setBroadcast(message);
        receipt.setMembreId(membreId);
        receipt.setLu(true);
        receipt.setLuAt(LocalDateTime.now());
        receiptRepository.save(receipt);
        message.setTotalLu(message.getTotalLu() + 1);
        broadcastRepository.save(message);
    }

    public Map<String, Object> getReceiptStats(UUID broadcastId) {
        BroadcastMessage message = getById(broadcastId);
        List<BroadcastReceipt> receipts = receiptRepository.findByBroadcastId(broadcastId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEnvoyé", message.getTotalEnvoyé());
        stats.put("totalLu", message.getTotalLu());
        stats.put("tauxOuverture", message.getTotalEnvoyé() > 0 ?
                (double) message.getTotalLu() / message.getTotalEnvoyé() * 100 : 0);
        stats.put("détails", receipts);
        return stats;
    }
}
