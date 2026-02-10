// src/main/java/com/simvex/simvex_api/ai/AiService.java
package com.simvex.simvex_api.ai;

import com.simvex.simvex_api.domain.AiChatHistoryEntity;
import com.simvex.simvex_api.domain.AiChatHistoryRepository;
import com.simvex.simvex_api.model.ModelEntity;
import com.simvex.simvex_api.model.ModelRepository;
import com.simvex.simvex_api.part.PartEntity;
import com.simvex.simvex_api.part.PartRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AiService {

    private final PartRepository partRepository;
    private final ModelRepository modelRepository;
    private final AiChatHistoryRepository aiChatHistoryRepository;
    private final OpenAIClient openAIClient;
    private final MockAiClient mockAiClient;

    public AiService(
            PartRepository partRepository,
            ModelRepository modelRepository,
            AiChatHistoryRepository aiChatHistoryRepository,
            OpenAIClient openAIClient,
            MockAiClient mockAiClient) {
        this.partRepository = partRepository;
        this.modelRepository = modelRepository;
        this.aiChatHistoryRepository = aiChatHistoryRepository;
        this.openAIClient = openAIClient;
        this.mockAiClient = mockAiClient;
    }

    // 1. 컨텍스트 빌드
    public AiContextResult buildContext(Long modelId, String meshName, String userId) {
        Map<String, Object> meta = new HashMap<>();
        String modelContext = "";
        String previousAiSummary = null;

        if (modelId != null) {
            Optional<ModelEntity> modelOpt = modelRepository.findById(modelId);
            if (modelOpt.isPresent()) {
                ModelEntity m = modelOpt.get();
                modelContext = """
                        - Model Title: %s
                        - Model Description: %s
                        """.formatted(m.getTitle(), m.getDescription() != null ? m.getDescription() : "N/A");

                // [삭제됨] 모델 기본 맥락(m.getAiSummary()) 로드 로직 제거
            }

            // [오직 내 기록만 사용]
            if (userId != null) {
                Optional<AiChatHistoryEntity> lastChat = aiChatHistoryRepository
                        .findTopByModel_IdAndUserIdOrderByCreatedAtDesc(modelId, userId);
                if (lastChat.isPresent() && lastChat.get().getAiSummary() != null) {
                    previousAiSummary = lastChat.get().getAiSummary();
                }
            }
        }

        // 부품 정보 (유지)
        String partContext;
        if (modelId == null || meshName == null || meshName.isBlank()) {
            meta.put("partFound", false);
            partContext = "- (부품이 선택되지 않음, 전체 모델에 대한 질문)";
        } else {
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
            } else {
                partContext = "- 해당 부품(%s) 정보를 찾을 수 없습니다.".formatted(meshName);
            }
        }

        return new AiContextResult("PART", partContext, modelContext, previousAiSummary, meta);
    }

    // 2. 프롬프트 생성 (유지)
    public String composePrompt(String question, String partContext, String modelContext) {
        return """
                [Role]
                You are a knowledgeable technical assistant explaining a 3D model.

                [Context Info]
                %s
                %s

                [Instructions]

                Answer the user's question based on the Context Info.
                If the context has a conversation history, use it.
                Infer function from name if description is missing.

                Answer in Korean. Use line breaks to separate paragraphs or list items clearly.

                Keep your answer concise and to the point. Avoid unnecessary rhetoric or repetition.
                Summarize the explanation in 3~5 sentences if possible.

                주어진 정보는 참고만 하고 반드시 question에 대해서만 답해줘
                [Question]
                %s
                """.formatted(modelContext, partContext, question);
    }

    // 3. 답변 생성 (유지)
    public AiAnswerResult generateAnswer(String prompt, String previousAiSummary) {
        if (!openAIClient.enabled()) {
            return new AiAnswerResult(mockAiClient.ask(prompt), null, "mock", null, null);
        }
        try {
            OpenAIClient.AiResponseWrapper wrapper = openAIClient.ask(prompt, previousAiSummary);
            if (wrapper.text() == null || wrapper.text().isBlank()) {
                return new AiAnswerResult("", null, "openai", "empty_answer", "Empty response");
            }
            return new AiAnswerResult(wrapper.text(), wrapper.responseId(), "openai", null, null);
        } catch (WebClientResponseException e) {
            return new AiAnswerResult("Error", null, "openai", "http_" + e.getStatusCode().value(),
                    e.getResponseBodyAsString());
        } catch (Exception e) {
            return new AiAnswerResult("Error", null, "openai", "error", e.getMessage());
        }
    }

    // 4. 대화 저장 (유지: 히스토리에만 저장)
    @Transactional
    public void saveChatInteraction(Long modelId, String question, String answer, String newAiSummary, String userId) {
        if (modelId == null)
            return;

        ModelEntity model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Model ID"));

        AiChatHistoryEntity history = new AiChatHistoryEntity(model, question, answer, userId, newAiSummary);
        aiChatHistoryRepository.save(history);
    }

    // 5. 조회 (유지)
    public List<AiChatHistoryDto> getChatHistory(Long modelId, String userId) {
        return aiChatHistoryRepository.findByModel_IdAndUserIdOrderByCreatedAtAsc(modelId, userId)
                .stream()
                .map(h -> new AiChatHistoryDto(h.getQuestion(), h.getAnswer(), h.getCreatedAt()))
                .toList();
    }

    public record AiAnswerResult(String answer, String newResponseId, String provider, String errorCode,
            String errorMessage) {
    }

    public record AiChatHistoryDto(String question, String answer, java.time.LocalDateTime timestamp) {
    }
}