// src/main/java/com/simvex/simvex_api/model/ModelEntity.java
package com.simvex.simvex_api.model;

import com.simvex.simvex_api.domain.MemoEntity;
import com.simvex.simvex_api.domain.QuizEntity;
import com.simvex.simvex_api.part.PartEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "models",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_models_domain_category_slug", columnNames = {"domain_key", "category_key", "slug"})
        }
)
public class ModelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "model_url", nullable = false)
    private String modelUrl;

    // ✅ B 필드 (기존)
    @Column(name = "domain_key")
    private String domainKey;

    @Column(name = "category_key")
    private String categoryKey;

    @Column(name = "slug")
    private String slug;

    // ----------------------------------------------------
    // [신규] 1. 모델 설명 및 AI 요약
    // ----------------------------------------------------
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    // ----------------------------------------------------
    // [신규] 2. 연관 관계 (부품, 퀴즈, 메모)
    // ----------------------------------------------------
    @OneToMany(mappedBy = "model", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartEntity> parts = new ArrayList<>();

    @OneToMany(mappedBy = "model", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizEntity> quizzes = new ArrayList<>();

    @OneToMany(mappedBy = "model", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemoEntity> memos = new ArrayList<>();

    protected ModelEntity() {}

    public ModelEntity(String title, String modelUrl) {
        this.title = title;
        this.modelUrl = modelUrl;
    }

    // --- Getter ---
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getModelUrl() { return modelUrl; }
    public String getDomainKey() { return domainKey; }
    public String getCategoryKey() { return categoryKey; }
    public String getSlug() { return slug; }
    
    public String getDescription() { return description; }
    public String getAiSummary() { return aiSummary; }

    public List<PartEntity> getParts() { return parts; }
    public List<QuizEntity> getQuizzes() { return quizzes; }
    public List<MemoEntity> getMemos() { return memos; }

    // --- Setter ---
    public void setTitle(String title) { this.title = title; }
    public void setModelUrl(String modelUrl) { this.modelUrl = modelUrl; }
    public void setDomainKey(String domainKey) { this.domainKey = domainKey; }
    public void setCategoryKey(String categoryKey) { this.categoryKey = categoryKey; }
    public void setSlug(String slug) { this.slug = slug; }
    
    public void setDescription(String description) { this.description = description; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    // --- 연관관계 편의 메소드 ---
    public void addPart(PartEntity part) {
        if (part == null) return;
        parts.add(part);
        part.setModel(this);
    }

    public void removePart(PartEntity part) {
        if (part == null) return;
        parts.remove(part);
        part.setModel(null);
    }

    // [신규] 퀴즈 추가
    public void addQuiz(QuizEntity quiz) {
        if (quiz == null) return;
        quizzes.add(quiz);
        quiz.setModel(this);
    }

    // [신규] 메모 추가
    public void addMemo(MemoEntity memo) {
        if (memo == null) return;
        memos.add(memo);
        memo.setModel(this);
    }
}