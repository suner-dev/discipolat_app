package com.discipolat.modules.trainings.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "module_id", nullable = false)
    private UUID moduleId;

    @Column(name = "question", nullable = false)
    private String question;

    /** Liste JSON des propositions de réponse. */
    @Column(name = "propositions", nullable = false)
    private String propositions;

    @Column(name = "reponse_index", nullable = false)
    private int reponseIndex;

    @Column(name = "ordre", nullable = false)
    private int ordre;
}
