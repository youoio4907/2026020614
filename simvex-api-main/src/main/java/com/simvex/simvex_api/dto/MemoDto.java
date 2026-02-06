package com.simvex.simvex_api.dto;

import com.simvex.simvex_api.domain.MemoEntity;

public class MemoDto {
    public Long id;
    public String title;
    public String content;

    public static MemoDto from(MemoEntity e) {
        MemoDto dto = new MemoDto();
        dto.id = e.getId();
        dto.title = e.getTitle();
        dto.content = e.getContent();
        return dto;
    }
}