// src/main/java/com/simvex/simvex_api/dto/AiAskResponseDto.java
package com.simvex.simvex_api.dto;

import java.util.Map;

public class AiAskResponseDto {

    public String answer;
    public String context;
    public String mode; // "GLOBAL" | "PART"
    public Map<String, Object> meta;

    public AiAskResponseDto() {}

    public AiAskResponseDto(String answer, String context, String mode, Map<String, Object> meta) {
        this.answer = answer;
        this.context = context;
        this.mode = mode;
        this.meta = meta;
    }
}
