package com.simvex.simvex_api.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "workflow_connections")
public class WorkflowConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long fromNodeId;
    private Long toNodeId;
    private String fromAnchor;
    private String toAnchor;

    public WorkflowConnectionEntity() {}

    public WorkflowConnectionEntity(Long fromNodeId, Long toNodeId, String fromAnchor, String toAnchor) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.fromAnchor = fromAnchor;
        this.toAnchor = toAnchor;
    }

    // Getters & Setters...
    public Long getId() { return id; }
    public Long getFromNodeId() { return fromNodeId; }
    public void setFromNodeId(Long fromNodeId) { this.fromNodeId = fromNodeId; }
    public Long getToNodeId() { return toNodeId; }
    public void setToNodeId(Long toNodeId) { this.toNodeId = toNodeId; }
    public String getFromAnchor() { return fromAnchor; }
    public void setFromAnchor(String fromAnchor) { this.fromAnchor = fromAnchor; }
    public String getToAnchor() { return toAnchor; }
    public void setToAnchor(String toAnchor) { this.toAnchor = toAnchor; }
}