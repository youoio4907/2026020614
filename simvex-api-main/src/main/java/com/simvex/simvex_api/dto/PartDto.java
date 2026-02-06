// src/main/java/com/simvex/simvex_api/dto/PartDto.java
package com.simvex.simvex_api.dto;

import com.simvex.simvex_api.part.PartEntity;

import java.util.Map;

public class PartDto {
    public Long id;
    public String meshName;

    // ✅ jsonb(Map) 그대로 내려준다
    public Map<String, Object> content;

    public static PartDto from(PartEntity e) {
        PartDto dto = new PartDto();
        dto.id = e.getId();
        dto.meshName = e.getMeshName();
        dto.content = e.getContent(); // ✅ Map -> Map
        return dto;
    }
}
