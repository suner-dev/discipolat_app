package com.discipolat.modules.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_registration_requests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_registration_email", columnNames = "email")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRegistrationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 30)
    private String phone;

    @Column(name = "organization_name", nullable = false, length = 255)
    private String organizationName;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(nullable = false, length = 30)
    private String plan;

    @Column(length = 2)
    private String country;

    @Column(length = 10)
    private String currency;

    @Column(length = 64)
    private String timezone;

    @Column(length = 10)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TenantRegistrationStatus status;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    // ── Preuve de consentement RGPD (capturée à la souscription, art. 7) ──

    @Column(name = "consent_terms_version", length = 20)
    private String consentTermsVersion;

    @Column(name = "consent_cgu", nullable = false)
    @lombok.Builder.Default
    private boolean consentCgu = false;

    @Column(name = "consent_privacy", nullable = false)
    @lombok.Builder.Default
    private boolean consentPrivacy = false;

    @Column(name = "consent_art9", nullable = false)
    @lombok.Builder.Default
    private boolean consentArt9 = false;

    @Column(name = "consent_ip", length = 64)
    private String consentIp;

    @Column(name = "consent_given_at")
    private Instant consentGivenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (status == null) status = TenantRegistrationStatus.PENDING_APPROVAL;
    }
}
