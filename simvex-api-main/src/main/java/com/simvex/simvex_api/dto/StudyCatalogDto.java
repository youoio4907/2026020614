// src/main/java/com/simvex/simvex_api/dto/StudyCatalogDto.java
package com.simvex.simvex_api.dto;

import java.util.List;

public class StudyCatalogDto {
    public String domainKey;
    public List<CategoryDto> categories;

    public static class CategoryDto {
        public String categoryKey;
        public String title;
        public List<ModelItemDto> models;
    }

    public static class ModelItemDto {
        public Long id;
        public String title;
        public String slug;
        public String modelUrl;
        public String domainKey;
        public String categoryKey;
    }
}
