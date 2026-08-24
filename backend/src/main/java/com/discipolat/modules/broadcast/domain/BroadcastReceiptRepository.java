package com.discipolat.modules.broadcast.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BroadcastReceiptRepository extends JpaRepository<BroadcastReceipt, UUID> {
    List<BroadcastReceipt> findByBroadcastId(UUID broadcastId);
    List<BroadcastReceipt> findByMembreId(UUID membreId);
}
