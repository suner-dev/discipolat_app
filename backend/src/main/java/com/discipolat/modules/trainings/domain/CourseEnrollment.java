package com.discipolat.modules.trainings.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "course_enrollments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class CourseEnrollment {

    public enum Statut { INSCRIT, EN_COURS, TERMINE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private Statut statut = Statut.INSCRIT;

    @Column(name = "progression", nullable = false)
    private int progression = 0;

    @Column(name = "score_quiz")
    private Integer scoreQuiz;

    @Column(name = "date_inscription", nullable = false, updatable = false)
    private LocalDateTime dateInscription;

    @Column(name = "date_terminaison")
    private LocalDateTime dateTerminaison;

    @PrePersist
    protected void onCreate() {
        this.dateInscription = LocalDateTime.now();
    }
}
