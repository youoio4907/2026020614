// src/main/java/com/simvex/simvex_api/ai/AiContextResult.java
package com.simvex.simvex_api.ai;

import java.util.Map;

public class AiContextResult {
    public final String mode;   // "GLOBAL" | "PART"
    public final String context;
    public final Map<String, Object> meta;

    public AiContextResult(String mode, String context, Map<String, Object> meta) {
        this.mode = mode;
        this.context = context;
        this.meta = meta;
    }
}
