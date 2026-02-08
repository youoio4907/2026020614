package com.simvex.simvex_api.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_attachments")
public class WorkflowAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFileName; // 사용자가 올린 원래 파일명
    private String storedFileName;   // 서버에 저장된 실제 파일명 (중복방지용 UUID 포함)
    private String filePath;         // 전체 경로

    private Long fileSize;           // 파일 크기 (Byte 단위)
    private String contentType;      // 파일 형식 (예: image/png, application/pdf)
    private LocalDateTime uploadedAt;// 업로드된 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private WorkflowNodeEntity node;

    public WorkflowAttachmentEntity() {}

    public WorkflowAttachmentEntity(String originalFileName, String storedFileName, String filePath, Long fileSize, String contentType, WorkflowNodeEntity node) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadedAt = LocalDateTime.now();
        this.node = node;
    }

    // Getters
    public Long getId() { return id; }
    public String getOriginalFileName() { return originalFileName; }
    public String getStoredFileName() { return storedFileName; }
    public String getFilePath() { return filePath; }
    public Long getFileSize() { return fileSize; }
    public String getContentType() { return contentType; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}