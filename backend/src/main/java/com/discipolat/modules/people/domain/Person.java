package com.discipolat.modules.people.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "person", indexes = {
    @Index(name = "idx_person_tenant", columnList = "tenant_id"),
    @Index(name = "idx_person_status", columnList = "status"),
    @Index(name = "idx_person_deleted", columnList = "deleted_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_person_tenant_email", columnNames = {"tenant_id", "email_normalized"}),
    @UniqueConstraint(name = "uk_person_tenant_phone", columnNames = {"tenant_id", "phone_normalized"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone_normalized", length = 30)
    private String phoneNormalized;

    @Column(name = "email_normalized", length = 255)
    private String emailNormalized;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "visibility_scope", nullable = false, length = 30)
    private String visibilityScope = "CHURCH";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public String getFullName() {
        if (firstName == null && lastName == null) return displayName;
        return (firstName != null ? firstName : "") + (lastName != null ? " " + lastName : "");
    }

    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }
}