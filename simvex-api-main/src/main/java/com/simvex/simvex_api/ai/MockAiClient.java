// src/main/java/com/simvex/simvex_api/ai/MockAiClient.java
package com.simvex.simvex_api.ai;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MockAiClient implements AiClient {

    @Override
    public String ask(String prompt) {
        // 지금은 Mock 유지
        return "Mock 답변\n"
                + "- prompt 길이: " + (prompt == null ? 0 : prompt.length()) + "\n"
                + "- 생성시각: " + Instant.now();
    }
}
