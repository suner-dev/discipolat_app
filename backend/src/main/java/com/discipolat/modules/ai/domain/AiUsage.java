package com.discipolat.modules.ai.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ai_usage", indexes = {
    @Index(name = "idx_ai_usage_tenant_date", columnList = "tenant_id, usage_date"),
    @Index(name = "idx_ai_usage_user", columnList = "user_id"),
    @Index(name = "idx_ai_usage_type", columnList = "request_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsage {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "request_type", nullable = false, length = 50)
    private String requestType; // CHAT, ANALYZE, NARRATIVE, SERMON, PREDICTION, SUMMARY

    @Column(name = "credits_consumed", nullable = false)
    private Integer creditsConsumed;

    @Column(name = "model_used", length = 50)
    private String modelUsed; // ollama, groq, gemini, mistral, huggingface, local

    @Column(name = "tokens_input")
    private Integer tokensInput;

    @Column(name = "tokens_output")
    private Integer tokensOutput;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "success")
    private Boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}