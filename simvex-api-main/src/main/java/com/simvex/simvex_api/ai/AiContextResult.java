// src/main/java/com/simvex/simvex_api/ai/AiContextResult.java
package com.simvex.simvex_api.ai;

import java.util.Map;

// class -> record 로 변경
public record AiContextResult(
    String mode,          // "GLOBAL" | "PART"
    String partContext,   // 부품 정보 (기존 context)
    String modelContext,  // [신규] 모델 정보 (Title, Description)
    Map<String, Object> meta
) {}