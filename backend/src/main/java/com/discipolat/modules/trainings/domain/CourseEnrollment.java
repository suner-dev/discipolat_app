package com.discipolat.modules.trainings.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_enrollments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEnrollment {

    public enum Statut { INSCRIT, EN_COURS, TERMINE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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
