package com.simvex.simvex_api.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiChatHistoryRepository extends JpaRepository<AiChatHistoryEntity, Long> {
    // 특정 모델의 대화 내역을 시간순으로 조회
    List<AiChatHistoryEntity> findByModel_IdOrderByCreatedAtAsc(Long modelId);
}