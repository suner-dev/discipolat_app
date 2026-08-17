package com.discipolat.modules.trainings.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "course_modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class CourseModule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "contenu")
    private String contenu;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "ordre", nullable = false)
    private int ordre;
}
