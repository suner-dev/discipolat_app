package com.discipolat.modules.discipleship.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.util.UUID;

@Entity
@Table(name = "discipleship_stage_requirements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class StageRequirement {

    public enum RequirementType {
        ATTEND_MEETING, COMPLETE_STUDY, MEMORIZE_VERSE, PRACTICE_HABIT,
        SERVE, SHARE_TESTIMONY, READ_BOOK, COMPLETE_COURSE, ATTEND_EVENT, CUSTOM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private RequirementType type;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "reference_name", length = 200)
    private String referenceName;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private boolean isRequired = true;

    @Column(name = "\"order\"", nullable = false)
    @Builder.Default
    private int order = 0;
}
