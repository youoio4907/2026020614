package com.simvex.simvex_api.controller;

import com.simvex.simvex_api.ai.AiContextResult;
import com.simvex.simvex_api.ai.AiService;
import com.simvex.simvex_api.dto.AiAskRequestDto;
import com.simvex.simvex_api.dto.AiAskResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    // [신규] 대화 기록 조회 API
    @GetMapping("/history/{modelId}")
    public ResponseEntity<List<AiService.AiChatHistoryDto>> getHistory(@PathVariable Long modelId) {
        return ResponseEntity.ok(aiService.getChatHistory(modelId));
    }

    @PostMapping("/ask")
    public AiAskResponseDto ask(@RequestBody AiAskRequestDto req) {
        if (req == null || req.question == null || req.question.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required");
        }

        // 1. Context 빌드 (response_id 조회 포함)
        AiContextResult ctx = aiService.buildContext(req.modelId, req.meshName);
        String previousResponseId = ctx.aiSummary(); // aiService에서 aiSummary 자리에 ID를 넣어줌

        // 2. 프롬프트 생성 (이전 기록 요약 제외)
        String prompt = aiService.composePrompt(
                req.question, 
                ctx.partContext(), 
                ctx.modelContext()
        );

        // 3. 답변 생성 (ID 전달)
        AiService.AiAnswerResult result = aiService.generateAnswer(prompt, previousResponseId);

        // 4. 저장 및 갱신 (성공 시)
        if (result.errorCode() == null) {
            aiService.saveChatInteraction(req.modelId, req.question, result.answer(), result.newResponseId());
        }

        // 5. 응답 구성
        Map<String, Object> meta = new HashMap<>();
        if (ctx.meta() != null) meta.putAll(ctx.meta());
        meta.put("provider", result.provider());
        if (result.errorCode() != null) {
            meta.put("aiErrorCode", result.errorCode());
            meta.put("aiErrorMessage", result.errorMessage());
        }

        return new AiAskResponseDto(result.answer(), ctx.partContext(), ctx.mode(), meta);
    }
}