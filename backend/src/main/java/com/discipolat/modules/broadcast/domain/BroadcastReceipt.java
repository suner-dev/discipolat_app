package com.discipolat.modules.broadcast.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "broadcast_receipts")
public class BroadcastReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broadcast_id", nullable = false)
    private BroadcastMessage broadcast;

    @Column(nullable = false)
    private UUID membreId;

    private boolean lu = false;
    private LocalDateTime luAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public BroadcastMessage getBroadcast() { return broadcast; }
    public void setBroadcast(BroadcastMessage broadcast) { this.broadcast = broadcast; }
    public UUID getMembreId() { return membreId; }
    public void setMembreId(UUID membreId) { this.membreId = membreId; }
    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }
    public LocalDateTime getLuAt() { return luAt; }
    public void setLuAt(LocalDateTime luAt) { this.luAt = luAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
