// src/main/java/com/simvex/simvex_api/controller/AiController.java
package com.simvex.simvex_api.controller;

import com.simvex.simvex_api.ai.AiContextResult;
import com.simvex.simvex_api.ai.AiService;
import com.simvex.simvex_api.dto.AiAskRequestDto;
import com.simvex.simvex_api.dto.AiAskResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = {
        "http://localhost:3000",
        "http://localhost:5173"
})
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ask")
    public AiAskResponseDto ask(@RequestBody AiAskRequestDto req) {
        if (req == null || isBlank(req.question)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required");
        }

        Long modelId = req.modelId;
        String meshName = safeTrim(req.meshName);
        String question = safeTrim(req.question);
        String notes = safeTrim(req.notes);

        AiContextResult ctx = aiService.buildContext(modelId, meshName);
        String prompt = aiService.composePrompt(question, ctx.context, notes, ctx.mode);

        AiService.AiAnswerResult result = aiService.generateAnswer(prompt);

        // meta 확장(기존 테스트/프론트와 호환)
        Map<String, Object> meta = new HashMap<>();
        if (ctx.meta != null) meta.putAll(ctx.meta);

        meta.put("provider", result.provider()); // "openai" | "mock"
        if (result.errorCode() != null) {
            meta.put("aiErrorCode", result.errorCode());
            meta.put("aiErrorMessage", result.errorMessage());
        }

        return new AiAskResponseDto(result.answer(), ctx.context, ctx.mode, meta);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
