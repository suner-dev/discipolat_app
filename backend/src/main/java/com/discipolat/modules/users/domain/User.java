package com.discipolat.modules.users.domain;

import com.discipolat.common.domain.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "email", nullable = false)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_role")
    private UserRole activeRole;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();

    @Column(name = "est_chef_de_famille", nullable = false)
    private boolean estChefDeFamille;

    @Column(name = "famille_geree_id")
    private UUID familleGereeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private UserStatus statut;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "situation_familiale")
    private String situationFamiliale;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private Instant accountLockedUntil;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled;

    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    @Column(name = "two_factor_backup_codes")
    private String twoFactorBackupCodes;

    /** Opt-in diffusion WhatsApp (annonces, rappels). */
    @Builder.Default
    @Column(name = "whatsapp_opt_in", nullable = false)
    private boolean whatsappOptIn = false;

    /** Fuseau horaire préféré (ex. Africa/Abidjan). */
    @Column(name = "timezone", length = 64)
    private String timezone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markUpdated() {
        this.updatedAt = Instant.now();
    }

    @Transient
    public boolean isAccountLocked() {
        return accountLockedUntil != null && Instant.now().isBefore(accountLockedUntil);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
