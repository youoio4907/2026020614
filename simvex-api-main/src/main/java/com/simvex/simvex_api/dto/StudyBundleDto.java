// src/main/java/com/simvex/simvex_api/dto/StudyBundleDto.java
package com.simvex.simvex_api.dto;

import java.util.List;

public class StudyBundleDto {
    public StudyModelDto model;
    public List<PartDto> parts;

    public static class StudyModelDto {
        public Long id;
        public String title;
        public String slug;
        public String modelUrl;
        public String domainKey;
        public String categoryKey;
    }
}
