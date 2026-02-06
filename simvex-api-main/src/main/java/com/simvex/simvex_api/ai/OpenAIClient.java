// src/main/java/com/simvex/simvex_api/ai/OpenAIClient.java
package com.simvex.simvex_api.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAIClient {

    private final WebClient webClient;
    private final String apiKey;

    public OpenAIClient(
            WebClient openAIWebClient,
            @Value("${openai.api-key:}") String apiKey
    ) {
        this.webClient = openAIWebClient;
        this.apiKey = apiKey;
        System.out.println("OPENAI KEY LOADED = " + (apiKey != null && !apiKey.isBlank()));
    }

    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String ask(String prompt) {
        // Responses API 규격: input_text 사용
        var req = new ResponsesRequest(
                "gpt-5-mini",
                List.of(
                        new InputMessage(
                                "user",
                                List.of(new ContentPart("input_text", prompt))
                        )
                )
        );

        try {
            ResponsesResponse res = webClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(ResponsesResponse.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            if (res == null) return "";

            System.out.println(res.output_text + "!!!!!!!!!!!!!!!!");

            // 1) 혹시 최상단 output_text 가 있으면 우선 사용 ??
            if (res.output_text != null && !res.output_text.isBlank()) {
                return res.output_text;
            }

            // 2) 일반적으로는 output[] -> message -> content[] -> output_text.text
            String extracted = extractOutputText(res.output);
            return extracted == null ? "" : extracted;

        } catch (WebClientResponseException e) {
            // 콘솔에 남겨서 원인 파악 쉽게
            System.out.println("OPENAI ERROR STATUS = " + e.getStatusCode());
            System.out.println("OPENAI ERROR BODY = " + e.getResponseBodyAsString());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractOutputText(List<Object> output) {
        if (output == null) return null;

        for (Object item : output) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> outItem = (Map<String, Object>) item;

            if (!"message".equals(outItem.get("type"))) continue;

            Object contentObj = outItem.get("content");
            if (!(contentObj instanceof List)) continue;

            List<Object> contentList = (List<Object>) contentObj;
            for (Object c : contentList) {
                if (!(c instanceof Map)) continue;
                Map<String, Object> part = (Map<String, Object>) c;

                if (!"output_text".equals(part.get("type"))) continue;

                Object text = part.get("text");
                if (text instanceof String s && !s.isBlank()) {
                    return s;
                }
            }
        }
        return null;
    }

    // ===== Request DTOs =====
    public record ResponsesRequest(String model, List<InputMessage> input) {}
    public record InputMessage(String role, List<ContentPart> content) {}
    public record ContentPart(String type, String text) {}

    // ===== Response DTO =====
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponsesResponse {
        public String id;
        public String output_text;     // 있을 수도 없을 수도
        public List<Object> output;    // 보통 여기에서 텍스트 추출
    }
}
