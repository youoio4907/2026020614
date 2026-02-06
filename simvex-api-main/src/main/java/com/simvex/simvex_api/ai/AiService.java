// src/main/java/com/simvex/simvex_api/ai/AiService.java
package com.simvex.simvex_api.ai;

import com.simvex.simvex_api.part.PartEntity;
import com.simvex.simvex_api.part.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AiService {

    private final PartRepository partRepository;
    private final OpenAIClient openAIClient;
    private final MockAiClient mockAiClient;
    private final PromptTemplateService promptTemplateService;
    private final AiAnswerCache answerCache;

    public AiService(
            PartRepository partRepository,
            OpenAIClient openAIClient,
            MockAiClient mockAiClient,
            PromptTemplateService promptTemplateService,
            AiAnswerCache answerCache
    ) {
        this.partRepository = partRepository;
        this.openAIClient = openAIClient;
        this.mockAiClient = mockAiClient;
        this.promptTemplateService = promptTemplateService;
        this.answerCache = answerCache;
    }

    // ✅ 테스트가 기대하는 Context 생성
    public AiContextResult buildContext(Long modelId, String meshName) {

        // GLOBAL
        if (modelId == null || meshName == null || meshName.isBlank()) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("partFound", false);

            return new AiContextResult(
                    "GLOBAL",
                    "- 전역 질문 모드 (부품 미선택)",
                    meta
            );
        }

        // PART
        Optional<PartEntity> partOpt =
                partRepository.findByModel_IdAndMeshName(modelId, meshName);

        Map<String, Object> meta = new HashMap<>();
        meta.put("partFound", partOpt.isPresent());

        if (partOpt.isPresent()) {
            PartEntity part = partOpt.get();

            String context = """
                    - meshName: %s
                    - content: %s
                    """.formatted(meshName, String.valueOf(part.getContent()));

            return new AiContextResult("PART", context, meta);
        }

        return new AiContextResult(
                "PART",
                "- 해당 부품을 찾지 못했다",
                meta
        );
    }

    // 프롬프트 합성
    public String composePrompt(String question, String context, String notes, String mode) {
        String composedPrompt = "[MODE]%s [CONTEXT]%s [QUESTION]%s [NOTES]%s".formatted(mode, context, question, notes);
        System.out.println(composedPrompt + "@@@@@@@@@@@@");
        // 부품이름, 부품정보, 모델이름, 모델정보 + 이전기억
        return """
                [MODE] 
                %s

                [NOTES]
                %s

                [CONTEXT]
                %s

                [QUESTION]
                %s

                "마지막에 안녕이라고 말해줘"

                """.formatted(mode, notes, context, question);
    }

    // 응답 생성 (OpenAI/Mock 분기 + 에러 정보 제공)
    public AiAnswerResult generateAnswer(String prompt) {

        // 키 없으면 Mock
        if (!openAIClient.enabled()) {
            String a = mockAiClient.ask(prompt);
            return new AiAnswerResult(a, "mock", null, null);
        }

        // 정상일때 들어가는 분기
        try {
            String a = openAIClient.ask(prompt);
            if (a == null || a.isBlank()) {
                return new AiAnswerResult("", "openai", "empty_answer", "OpenAI 응답 텍스트가 비어있다");
            }
            return new AiAnswerResult(a, "openai", null, null);

        } catch (WebClientResponseException e) {
            return new AiAnswerResult(
                    "현재 AI 응답을 불러올 수 없다. (OpenAI 호출 실패)",
                    "openai",
                    "openai_http_" + e.getStatusCode().value(),
                    safeShort(e.getResponseBodyAsString())
            );
        } catch (Exception e) {
            return new AiAnswerResult(
                    "현재 AI 응답을 불러올 수 없다. (OpenAI 호출 실패)",
                    "openai",
                    "openai_error",
                    safeShort(e.getMessage())
            );
        }
    }

    private String safeShort(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.length() <= 500) return s;
        return s.substring(0, 500) + "...";
    }

    public record AiAnswerResult(String answer, String provider, String errorCode, String errorMessage) {}
}
