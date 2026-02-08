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
    private final AiChatHistoryRepository aiChatHistoryRepository; // [추가]
    private final OpenAIClient openAIClient;
    private final MockAiClient mockAiClient;

    public AiService(
            PartRepository partRepository,
            ModelRepository modelRepository,
            AiChatHistoryRepository aiChatHistoryRepository,
            OpenAIClient openAIClient,
            MockAiClient mockAiClient
    ) {
        this.partRepository = partRepository;
        this.modelRepository = modelRepository;
        this.aiChatHistoryRepository = aiChatHistoryRepository;
        this.openAIClient = openAIClient;
        this.mockAiClient = mockAiClient;
    }

    // 1. 컨텍스트 및 ID 조회
    // aiSummary 필드를 previous_response_id 저장소로 사용
    public AiContextResult buildContext(Long modelId, String meshName) {
        Map<String, Object> meta = new HashMap<>();
        String modelContext = "";
        String previousResponseId = null; // [변경] 기존 요약 텍스트 대신 ID 사용

        // (1) Model Info 및 Response ID 추출
        if (modelId != null) {
            Optional<ModelEntity> modelOpt = modelRepository.findById(modelId);
            if (modelOpt.isPresent()) {
                ModelEntity m = modelOpt.get();
                // aiSummary 컬럼에 저장된 값을 ID로 사용
                if (m.getAiSummary() != null && !m.getAiSummary().isBlank()) {
                    previousResponseId = m.getAiSummary();
                }
                
                modelContext = """
                        - Model Title: %s
                        - Model Description: %s
                        """.formatted(m.getTitle(), m.getDescription() != null ? m.getDescription() : "N/A");
            }
        }

        // (2) Part Info 추출 (기존 로직 유지)
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

        // Response ID를 aiSummary 자리에 담아서 리턴
        return new AiContextResult("PART", partContext, modelContext, previousResponseId, meta);
    }

    // 2. 프롬프트 생성 (이전 기억 제거, 문맥은 ID로 대체)
    public String composePrompt(String question, String partContext, String modelContext) {
        // [변경] Previous Memory 섹션 제거 (API가 처리)
        // [참고] 모델/부품 정보는 사용자가 다른 부품을 클릭할 때마다 바뀌므로 매번 보내주는 것이 정확합니다.
        return """
                [Role]
                You are a knowledgeable technical assistant explaining a 3D model.

                [Context Info]
                %s
                %s

                [Instructions]
                1. Answer the user's question based on the Context Info.
                2. If the context has a conversation history, use it.
                3. Infer function from name if description is missing.
                4. Answer in helpful Korean.

                주어진 정보는 참고만 하고 question에 대해서만 답해줘
                [Question]
                %s
                """.formatted(modelContext, partContext, question);
    }

    // 3. 답변 생성 (ID 전달 및 결과 반환)
    public AiAnswerResult generateAnswer(String prompt, String previousResponseId) {
        if (!openAIClient.enabled()) {
            return new AiAnswerResult(mockAiClient.ask(prompt), null, "mock", null, null);
        }
        try {
            // [변경] ID 전달 및 래퍼 수신
            OpenAIClient.AiResponseWrapper wrapper = openAIClient.ask(prompt, previousResponseId);
            
            if (wrapper.text() == null || wrapper.text().isBlank()) {
                return new AiAnswerResult("", null, "openai", "empty_answer", "Empty response");
            }
            return new AiAnswerResult(wrapper.text(), wrapper.responseId(), "openai", null, null);
        } catch (WebClientResponseException e) {
            return new AiAnswerResult("Error", null, "openai", "http_" + e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            return new AiAnswerResult("Error", null, "openai", "error", e.getMessage());
        }
    }

    // 4. [신규] 대화 저장 및 상태 갱신
    @Transactional
    public void saveChatInteraction(Long modelId, String question, String answer, String newResponseId) {
        if (modelId == null) return;
        
        ModelEntity model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Model ID"));

        // (1) 대화 내용 기록 (DB)
        AiChatHistoryEntity history = new AiChatHistoryEntity(model, question, answer);
        aiChatHistoryRepository.save(history);

        // (2) 다음 대화를 위해 aiSummary에 newResponseId 갱신
        if (newResponseId != null && !newResponseId.isBlank()) {
            model.setAiSummary(newResponseId);
            // modelRepository.save(model); // Transactional 안에서는 Dirty Checking으로 자동 저장됨
        }
    }
    
    // 5. [신규] 대화 기록 조회
    public List<AiChatHistoryDto> getChatHistory(Long modelId) {
        return aiChatHistoryRepository.findByModel_IdOrderByCreatedAtAsc(modelId)
                .stream()
                .map(h -> new AiChatHistoryDto(h.getQuestion(), h.getAnswer(), h.getCreatedAt()))
                .toList();
    }

    // DTOs
    public record AiAnswerResult(String answer, String newResponseId, String provider, String errorCode, String errorMessage) {}
    public record AiChatHistoryDto(String question, String answer, java.time.LocalDateTime timestamp) {}
}