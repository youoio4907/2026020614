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

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // [1] 기본 생성자 (JPA 필수)
    public AiChatHistoryEntity() {
    }

    // [2] 데이터 저장용 생성자
    public AiChatHistoryEntity(ModelEntity model, String question, String answer) {
        this.model = model;
        this.question = question;
        this.answer = answer;
    }

    // [3] Getters (Lombok 대신 직접 작성)
    public Long getId() {
        return id;
    }

    public ModelEntity getModel() {
        return model;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}