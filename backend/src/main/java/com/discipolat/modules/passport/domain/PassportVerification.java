package com.discipolat.modules.passport.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Trace de vérification publique d'un passeport (scan QR / page publique).
 * Volontairement non filtrée par tenant : le QR est vérifié publiquement.
 */
@Entity
@Table(name = "passport_verifications")
public class PassportVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "passport_code", nullable = false, length = 40)
    private String passportCode;

    /** VALID, REVOKED, EXPIRED, INVALID, NOT_FOUND. */
    @Column(nullable = false, length = 20)
    private String result;

    @Column(name = "remote_addr", length = 60)
    private String remoteAddr;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "verified_at", nullable = false, updatable = false)
    private LocalDateTime verifiedAt;

    @PrePersist
    protected void onCreate() {
        verifiedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPassportCode() { return passportCode; }
    public void setPassportCode(String passportCode) { this.passportCode = passportCode; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getRemoteAddr() { return remoteAddr; }
    public void setRemoteAddr(String remoteAddr) { this.remoteAddr = remoteAddr; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
}
