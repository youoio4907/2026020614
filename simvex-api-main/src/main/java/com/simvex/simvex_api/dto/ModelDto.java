// src/main/java/com/simvex/simvex_api/dto/ModelDto.java
package com.simvex.simvex_api.dto;

import com.simvex.simvex_api.model.ModelEntity;

public class ModelDto {
    public Long id;
    public String title;
    public String modelUrl;

    public static ModelDto from(ModelEntity e) {
        ModelDto dto = new ModelDto();
        dto.id = e.getId();
        dto.title = e.getTitle();
        dto.modelUrl = e.getModelUrl();
        return dto;
    }
}
