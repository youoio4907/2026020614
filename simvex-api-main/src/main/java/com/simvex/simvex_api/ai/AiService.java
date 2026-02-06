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

    // 1. 컨텍스트 생성 (Summary를 별도 필드로 분리)
    public AiContextResult buildContext(Long modelId, String meshName) {
        Map<String, Object> meta = new HashMap<>();

        String modelContext = "";
        String aiSummary = "기록된 이전 대화 요약이 없습니다.";

        // (1) Model Info 및 Summary 추출
        if (modelId != null) {
            Optional<ModelEntity> modelOpt = modelRepository.findById(modelId);
            if (modelOpt.isPresent()) {
                ModelEntity m = modelOpt.get();
                
                // [변경] Summary를 modelContext에 섞지 않고 변수에 저장
                if (m.getAiSummary() != null && !m.getAiSummary().isBlank()) {
                    aiSummary = m.getAiSummary();
                }

                // [변경] Model Context는 순수하게 모델 정보(이름, 설명)만 포함
                modelContext = """
                        - Model Title: %s
                        - Model Description: %s
                        """.formatted(
                                m.getTitle(), 
                                m.getDescription() != null ? m.getDescription() : "N/A"
                        );
            }
        }

        // (2) Part Info 추출
        String partContext;
        if (modelId == null || meshName == null || meshName.isBlank()) {
            meta.put("partFound", false);
            partContext = "- (부품이 선택되지 않음, 전체 모델에 대한 질문)";
            return new AiContextResult("GLOBAL", partContext, modelContext, aiSummary, meta);
        }

        Optional<PartEntity> partOpt = partRepository.findByModel_IdAndMeshName(modelId, meshName);
        meta.put("partFound", partOpt.isPresent());

        if (partOpt.isPresent()) {
            PartEntity part = partOpt.get();
            
            Map<String, Object> content = part.getContent();
            String title = (content.get("title") != null) ? content.get("title").toString() : meshName;
            String desc = (content.get("desc") != null) ? content.get("desc").toString() : "설명 없음";

            partContext = """
                    - Part Name: %s
                    - Part Description: %s
                    """.formatted(title, desc);
            
            return new AiContextResult("PART", partContext, modelContext, aiSummary, meta);
        }

        partContext = "- 해당 부품(%s) 정보를 찾을 수 없습니다.".formatted(meshName);
        return new AiContextResult("PART", partContext, modelContext, aiSummary, meta);
    }

    // 2. [변경] 프롬프트 합성 (이전 기억 + 모델 정보 + 부품 정보 + 질문)
    // 파라미터에 aiSummary 추가됨
    public String composePrompt(String question, String partContext, String modelContext, String aiSummary, String mode) {
        return """
                [Role]
                You are a knowledgeable technical assistant explaining a 3D model.

                [Instructions]
                1. Answer the user's [Question] utilizing the provided [Model Info], [Part Info], and [Previous Memory].
                2. **Synthesize** the information into natural, helpful Korean sentences. Do NOT simply list the provided fields.
                3. **Inference**: If specific descriptions are missing or marked as "None/설명 없음", **infer** the component's function based on its **name** (e.g., if name is 'Connecting_Rod', explain what a connecting rod does in this context).
                4. **Avoid Negativity**: Do NOT explicitly state "There is no description" or "I only have this info". Instead, focus on explaining what the part *likely* is or does based on your general knowledge and the part's name.
                5. **Context**: Use [Previous Memory] as background context but do not quote it directly unless asked.
                6. Be professional, concise, and helpful.

                [Previous Memory]
                %s

                [Model Info]
                %s

                [Part Info]
                %s

                [Question]
                %s
                """.formatted(aiSummary, modelContext, partContext, question);
    }

    // 3. 답변 생성 (변경 없음)
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

    // 4. 요약 업데이트 (변경 없음)
    @Async
    public void updateSummary(Long modelId, String question, String answer) {
        if (modelId == null || !openAIClient.enabled()) return;
        
        try {
            modelRepository.findById(modelId).ifPresent(model -> {
                String oldSummary = model.getAiSummary() == null ? "없음" : model.getAiSummary();
                
                String prompt = """
                        [Role]
                        You are a manager maintaining the 'Current Status Summary' of a 3D model.
                        
                        [Task]
                        Integrate the [New Interaction] into the [Old Summary] to create a **single, updated summary**.
                        
                        [Constraints]
                        1. **Do not simply append** the new information. Merge it naturally.
                        2. Remove redundant or outdated information.
                        3. Write in **Korean**.
                        4. Output **ONLY plain text**.
                        5. Keep the total length concise.
                        
                        [Old Summary]
                        %s
                        
                        [New Interaction]
                        User: %s
                        AI: %s
                        
                        [Updated Summary]
                        """.formatted(oldSummary, question, answer);
                
                String newSummary = openAIClient.ask(prompt);
                if (newSummary != null && !newSummary.isBlank()) {
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