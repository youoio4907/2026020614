// src/main/java/com/simvex/simvex_api/dto/ModelDto.java
package com.simvex.simvex_api.dto;

import com.simvex.simvex_api.model.ModelEntity;

public class ModelDto {
    private Long id;
    private String title;
    private String modelUrl;
    
    private String description;
    private String aiSummary;
    private String domainKey;
    private String categoryKey;
    private String slug;

    public ModelDto() {}

    public static ModelDto from(ModelEntity e) {
        ModelDto dto = new ModelDto();
        dto.setId(e.getId());
        dto.setTitle(e.getTitle());
        dto.setModelUrl(e.getModelUrl());
        
        dto.setDescription(e.getDescription());
        dto.setAiSummary(e.getAiSummary());
        dto.setDomainKey(e.getDomainKey());
        dto.setCategoryKey(e.getCategoryKey());
        dto.setSlug(e.getSlug());
        
        return dto;
    }

    // --- Getter & Setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getModelUrl() { return modelUrl; }
    public void setModelUrl(String modelUrl) { this.modelUrl = modelUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getDomainKey() { return domainKey; }
    public void setDomainKey(String domainKey) { this.domainKey = domainKey; }

    public String getCategoryKey() { return categoryKey; }
    public void setCategoryKey(String categoryKey) { this.categoryKey = categoryKey; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}