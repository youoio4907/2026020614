package com.simvex.simvex_api.domain;

import com.simvex.simvex_api.model.ModelEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "model_quizzes")
public class QuizEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question; // 문제 (JSON key: "q" or "question")

    @Column(nullable = false)
    private int answer;      // 정답 인덱스 (JSON key: "ans" or "answer")

    // [신규] 4지선다 보기 리스트 (JSON key: "opts")
    // DB에는 ["옵션1", "옵션2", "옵션3", "옵션4"] 형태의 JSON 배열로 저장됨
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<String> options = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String explanation; // 해설

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private ModelEntity model;

    protected QuizEntity() {}

    public QuizEntity(String question, int answer, List<String> options, String explanation) {
        this.question = question;
        this.answer = answer;
        this.options = options;
        this.explanation = explanation;
    }

    // --- Getter & Setter ---

    public Long getId() { return id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public int getAnswer() { return answer; }
    public void setAnswer(int answer) { this.answer = answer; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public ModelEntity getModel() { return model; }
    public void setModel(ModelEntity model) { this.model = model; }
}