package com.simvex.simvex_api.domain;

import com.simvex.simvex_api.model.ModelEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "model_memos")
public class MemoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;    // 제목

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;  // 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private ModelEntity model;

    protected MemoEntity() {
        // JPA용 기본 생성자
    }

    public MemoEntity(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // --- Getter & Setter ---

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ModelEntity getModel() {
        return model;
    }

    public void setModel(ModelEntity model) {
        this.model = model;
    }
}