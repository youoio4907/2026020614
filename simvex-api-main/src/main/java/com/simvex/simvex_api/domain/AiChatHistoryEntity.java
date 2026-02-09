package com.simvex.simvex_api.domain;

import com.simvex.simvex_api.model.ModelEntity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ai_chat_histories")
public class AiChatHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private ModelEntity model;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    // [NEW] 사용자 ID
    @Column(name = "user_id", nullable = false)
    private String userId;

    // [NEW] 대화 맥락 ID
    @Column(name = "ai_summary")
    private String aiSummary;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public AiChatHistoryEntity() {}

    public AiChatHistoryEntity(ModelEntity model, String question, String answer, String userId, String aiSummary) {
        this.model = model;
        this.question = question;
        this.answer = answer;
        this.userId = userId;
        this.aiSummary = aiSummary;
    }

    public Long getId() { return id; }
    public ModelEntity getModel() { return model; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String getUserId() { return userId; }
    public String getAiSummary() { return aiSummary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}