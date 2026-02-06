package com.simvex.simvex_api.dto;

import com.simvex.simvex_api.domain.QuizEntity;
import java.util.List;

public class QuizDto {
    public Long id;
    public String question;     // 문제
    public List<String> options;// [신규] 4지선다 보기
    public int answer;          // 정답 번호
    public String explanation;  // 해설

    public static QuizDto from(QuizEntity e) {
        QuizDto dto = new QuizDto();
        dto.id = e.getId();
        dto.question = e.getQuestion();
        dto.options = e.getOptions(); // Entity의 List<String>을 그대로 전달
        dto.answer = e.getAnswer();
        dto.explanation = e.getExplanation();
        return dto;
    }
}