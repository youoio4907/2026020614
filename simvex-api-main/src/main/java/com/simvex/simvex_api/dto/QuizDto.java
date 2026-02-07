package com.simvex.simvex_api.dto;

import com.simvex.simvex_api.domain.QuizEntity;
import java.util.List;

public class QuizDto {
    private Long id;
    private String question;
    private int answer;
    private List<String> options;
    private String explanation;
    private String modelTitle; // [추가] 모델 이름 필드

    public QuizDto() {}

    public QuizDto(Long id, String question, int answer, List<String> options, String explanation, String modelTitle) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.options = options;
        this.explanation = explanation;
        this.modelTitle = modelTitle;
    }

    public static QuizDto from(QuizEntity e) {
        // 엔티티에서 모델 이름 추출
        String mTitle = (e.getModel() != null) ? e.getModel().getTitle() : null;
        
        return new QuizDto(
            e.getId(),
            e.getQuestion(),
            e.getAnswer(),
            e.getOptions(),
            e.getExplanation(),
            mTitle // [추가]
        );
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public int getAnswer() { return answer; }
    public void setAnswer(int answer) { this.answer = answer; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getModelTitle() { return modelTitle; }
    public void setModelTitle(String modelTitle) { this.modelTitle = modelTitle; }
}