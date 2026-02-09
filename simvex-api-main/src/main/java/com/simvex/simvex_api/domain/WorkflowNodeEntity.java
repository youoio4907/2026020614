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

    // [NEW] 사용자 구분을 위한 ID (UUID 문자열)
    @Column(name = "user_id", nullable = false)
    private String userId;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowAttachmentEntity> attachments = new ArrayList<>();

    public WorkflowNodeEntity() {}

    // 생성자에 userId 추가
    public WorkflowNodeEntity(String title, String content, Double x, Double y, String userId) {
        this.title = title;
        this.content = content;
        this.x = x;
        this.y = y;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Double getX() { return x; }
    public void setX(Double x) { this.x = x; }
    public Double getY() { return y; }
    public void setY(Double y) { this.y = y; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<WorkflowAttachmentEntity> getAttachments() { return attachments; }
}