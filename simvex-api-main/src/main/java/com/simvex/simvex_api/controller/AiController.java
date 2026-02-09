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

    @GetMapping("/history/{modelId}")
    public ResponseEntity<List<AiService.AiChatHistoryDto>> getHistory(
            @PathVariable Long modelId,
            @RequestHeader(value="X-User-ID", defaultValue="default-guest") String userId
    ) {
        return ResponseEntity.ok(aiService.getChatHistory(modelId, userId));
    }

    @PostMapping("/ask")
    public AiAskResponseDto ask(
            @RequestBody AiAskRequestDto req,
            @RequestHeader(value="X-User-ID", defaultValue="default-guest") String userId
    ) {
        if (req == null || req.question == null || req.question.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required");
        }

        // userId 전달
        AiContextResult ctx = aiService.buildContext(req.modelId, req.meshName, userId);
        String previousAiSummary = ctx.aiSummary(); 

        String prompt = aiService.composePrompt(
                req.question, 
                ctx.partContext(), 
                ctx.modelContext()
        );

        AiService.AiAnswerResult result = aiService.generateAnswer(prompt, previousAiSummary);

        if (result.errorCode() == null) {
            // userId 전달
            aiService.saveChatInteraction(req.modelId, req.question, result.answer(), result.newResponseId(), userId);
        }

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