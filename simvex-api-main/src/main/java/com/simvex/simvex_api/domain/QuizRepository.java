package com.simvex.simvex_api.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface QuizRepository extends JpaRepository<QuizEntity, Long> {
    List<QuizEntity> findByModelIdOrderByIdAsc(Long modelId);

    // 여러 모델의 퀴즈를 한 번에 가져오기 (모의고사용)
    @Query("SELECT q FROM QuizEntity q WHERE q.model.id IN :modelIds")
    List<QuizEntity> findByModelIds(@Param("modelIds") List<Long> modelIds);
}