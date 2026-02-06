// src/main/java/com/simvex/simvex_api/ai/AiService.java
package com.simvex.simvex_api.ai;

import com.simvex.simvex_api.model.ModelEntity;
import com.simvex.simvex_api.model.ModelRepository;
import com.simvex.simvex_api.part.PartEntity;
import com.simvex.simvex_api.part.PartRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AiService {

    private final PartRepository partRepository;
    private final ModelRepository modelRepository;
    private final OpenAIClient openAIClient;
    private final MockAiClient mockAiClient;

    public AiService(
            PartRepository partRepository,
            ModelRepository modelRepository,
            OpenAIClient openAIClient,
            MockAiClient mockAiClient
    ) {
        this.partRepository = partRepository;
        this.modelRepository = modelRepository;
        this.openAIClient = openAIClient;
        this.mockAiClient = mockAiClient;
    }

    // 1. 컨텍스트 생성 (AI 요약 정보 추가)
    public AiContextResult buildContext(Long modelId, String meshName) {
        Map<String, Object> meta = new HashMap<>();

        // (1) Model Info 추출
        String modelContext = "";
        if (modelId != null) {
            Optional<ModelEntity> modelOpt = modelRepository.findById(modelId);
            if (modelOpt.isPresent()) {
                ModelEntity m = modelOpt.get();
                // [변경] DB에 있는 ai_summary 가져오기 (없으면 "없음")
                String summary = (m.getAiSummary() != null && !m.getAiSummary().isBlank()) 
                        ? m.getAiSummary() 
                        : "아직 요약된 정보가 없습니다.";

                // [변경] 프롬프트에 보낼 모델 정보에 요약(Summary) 추가
                modelContext = """
                        - Model Title: %s
                        - Model Description: %s
                        - AI Summary (Previous Context): %s
                        """.formatted(
                                m.getTitle(), 
                                m.getDescription() != null ? m.getDescription() : "N/A",
                                summary
                        );
            }
        }

        // (2) Part Info 추출
        String partContext;
        if (modelId == null || meshName == null || meshName.isBlank()) {
            meta.put("partFound", false);
            partContext = "- (부품이 선택되지 않음, 전체 모델에 대한 질문)";
            return new AiContextResult("GLOBAL", partContext, modelContext, meta);
        }

        Optional<PartEntity> partOpt = partRepository.findByModel_IdAndMeshName(modelId, meshName);
        meta.put("partFound", partOpt.isPresent());

        if (partOpt.isPresent()) {
            PartEntity part = partOpt.get();
            partContext = """
                    - Part Name: %s
                    - Part Details: %s
                    """.formatted(meshName, String.valueOf(part.getContent()));
            
            return new AiContextResult("PART", partContext, modelContext, meta);
        }

        partContext = "- 해당 부품(%s) 정보를 찾을 수 없습니다.".formatted(meshName);
        return new AiContextResult("PART", partContext, modelContext, meta);
    }

    // 2. 프롬프트 합성
    public String composePrompt(String question, String partContext, String modelContext, String mode) {
        return """
                [MODE] 
                %s

                [NOTES]
                %s

                [CONTEXT]
                %s

                [QUESTION]
                %s

                마지막에 안녕이라고 말해줘
                """.formatted(mode, modelContext, partContext, question);
    }

    // 3. 답변 생성
    public AiAnswerResult generateAnswer(String prompt) {
        if (!openAIClient.enabled()) {
            return new AiAnswerResult(mockAiClient.ask(prompt), "mock", null, null);
        }
        try {
            String a = openAIClient.ask(prompt);
            if (a == null || a.isBlank()) return new AiAnswerResult("", "openai", "empty_answer", "Empty response");
            return new AiAnswerResult(a, "openai", null, null);
        } catch (WebClientResponseException e) {
            return new AiAnswerResult("Error", "openai", "http_" + e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            return new AiAnswerResult("Error", "openai", "error", e.getMessage());
        }
    }

    // 4. [변경] 요약 업데이트 (텍스트 전용 + 비동기 권장)
    @Async // 이 어노테이션이 있어야 사용자가 답변을 빨리 받습니다.
    public void updateSummary(Long modelId, String question, String answer) {
        if (modelId == null || !openAIClient.enabled()) return;
        
        try {
            modelRepository.findById(modelId).ifPresent(model -> {
                String oldSummary = model.getAiSummary() == null ? "없음" : model.getAiSummary();
                
                // [변경] "순수 텍스트로만" 작성하라는 강력한 지시 추가
                String prompt = """
                        [Role]
                        You are a manager maintaining the 'Current Status Summary' of a 3D model.
                        
                        [Task]
                        Integrate the [New Interaction] into the [Old Summary] to create a **single, updated summary**.
                        
                        [Constraints]
                        1. **Do not simply append** the new information. Merge it naturally.
                        2. Remove redundant or outdated information.
                        3. Write in **Korean**.
                        4. Output **ONLY plain text** (No Markdown, No JSON, No special characters like '#').
                        5. Keep the total length concise and coherent.
                        
                        [Old Summary]
                        %s
                        
                        [New Interaction]
                        User: %s
                        AI: %s
                        
                        [Updated Summary]
                        """.formatted(oldSummary, question, answer);
                
                String newSummary = openAIClient.ask(prompt);
                if (newSummary != null && !newSummary.isBlank()) {
                    // 혹시 모를 마크다운 문자 제거 (간단한 후처리)
                    String cleanSummary = newSummary.replaceAll("[#*`]", "").trim();
                    
                    model.setAiSummary(cleanSummary);
                    modelRepository.save(model);
                }
            });
        } catch (Exception e) {
            System.err.println("Summary update failed: " + e.getMessage());
        }
    }

    public record AiAnswerResult(String answer, String provider, String errorCode, String errorMessage) {}
}