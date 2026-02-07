package com.simvex.simvex_api.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflow_nodes")
public class WorkflowNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;

    private Double x;
    private Double y;

    // 첨부파일과의 관계 (1:N)
    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowAttachmentEntity> attachments = new ArrayList<>();

    public WorkflowNodeEntity() {}

    public WorkflowNodeEntity(String title, String content, Double x, Double y) {
        this.title = title;
        this.content = content;
        this.x = x;
        this.y = y;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Double getX() { return x; }
    public void setX(Double x) { this.x = x; }
    public Double getY() { return y; }
    public void setY(Double y) { this.y = y; }
    public List<WorkflowAttachmentEntity> getAttachments() { return attachments; }
}