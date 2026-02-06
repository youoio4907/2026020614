package com.simvex.simvex_api.dto;

import com.simvex.simvex_api.model.ModelEntity;
import java.util.List;
import java.util.stream.Collectors;

public class ModelDto {
    public Long id;
    public String title;
    public String modelUrl;

    // [신규 추가] 모델 설명 및 AI 요약
    public String description;
    public String aiSummary;

    // [신규 추가] 퀴즈와 메모 리스트
    public List<QuizDto> quizzes;
    public List<MemoDto> memos;

    public static ModelDto from(ModelEntity e) {
        ModelDto dto = new ModelDto();
        dto.id = e.getId();
        dto.title = e.getTitle();
        dto.modelUrl = e.getModelUrl();

        // Entity 데이터를 DTO로 복사
        dto.description = e.getDescription();
        dto.aiSummary = e.getAiSummary();

        // 리스트 변환 (Entity List -> DTO List)
        if (e.getQuizzes() != null) {
            dto.quizzes = e.getQuizzes().stream()
                    .map(QuizDto::from)
                    .collect(Collectors.toList());
        }
        
        if (e.getMemos() != null) {
            dto.memos = e.getMemos().stream()
                    .map(MemoDto::from)
                    .collect(Collectors.toList());
        }

        return dto;
    }
}