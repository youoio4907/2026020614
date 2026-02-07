// src/main/java/com/simvex/simvex_api/dto/MemoDto.java
package com.simvex.simvex_api.dto;

import com.simvex.simvex_api.domain.MemoEntity;

public class MemoDto {
    private Long id;
    private String title;
    private String content;

    // 기본 생성자
    public MemoDto() {}

    // 전체 필드 생성자
    public MemoDto(Long id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    // Entity -> DTO 변환
    public static MemoDto from(MemoEntity e) {
        return new MemoDto(
            e.getId(),
            e.getTitle(),
            e.getContent()
        );
    }

    // --- Getter & Setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}