package com.discipolat.modules.trainings.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "course_modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseModule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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
