package com.simvex.simvex_api.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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

    public OpenAIClient(WebClient openAIWebClient, @Value("${openai.api-key:}") String apiKey) {
        this.webClient = openAIWebClient;
        this.apiKey = apiKey;
    }

    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    // [변경] 리턴 타입을 String -> AiResponseWrapper (ID 포함)로 변경
    // [변경] 파라미터에 previousResponseId 추가
    public AiResponseWrapper ask(String prompt, String previousResponseId) {
        
        // previousResponseId가 있으면 포함하여 요청 객체 생성
        var req = new ResponsesRequest(
                "gpt-5-mini", // 또는 사용 가능한 모델명
                List.of(
                        new InputMessage(
                                "user",
                                List.of(new ContentPart("input_text", prompt))
                        )
                ),
                previousResponseId // null이면 포함되지 않음 (JsonInclude 설정 필요)
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

            if (res == null) return new AiResponseWrapper("", null);

            String answerText = "";
            if (res.output_text != null && !res.output_text.isBlank()) {
                answerText = res.output_text;
            } else {
                answerText = extractOutputText(res.output);
            }

            // 응답 텍스트와 함께 새로운 response_id 반환
            return new AiResponseWrapper(answerText != null ? answerText : "", res.id);

        } catch (WebClientResponseException e) {
            System.out.println("OPENAI ERROR: " + e.getResponseBodyAsString());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractOutputText(List<Object> output) {
        // (기존 로직 유지)
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

    // ===== DTOs =====
    @JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 JSON에서 제외
    public record ResponsesRequest(
            String model, 
            List<InputMessage> input,
            String previous_response_id // [추가] 필드
    ) {}

    public record InputMessage(String role, List<ContentPart> content) {}
    public record ContentPart(String type, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponsesResponse {
        public String id; // OpenAI가 반환하는 Response ID
        public String output_text;
        public List<Object> output;
    }

    // [신규] 결과 래퍼 클래스
    public record AiResponseWrapper(String text, String responseId) {}
}