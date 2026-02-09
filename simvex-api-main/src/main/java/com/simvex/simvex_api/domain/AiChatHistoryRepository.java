package com.simvex.simvex_api.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AiChatHistoryRepository extends JpaRepository<AiChatHistoryEntity, Long> {

    // 채팅 목록 조회
    List<AiChatHistoryEntity> findByModel_IdAndUserIdOrderByCreatedAtAsc(Long modelId, String userId);

    // [NEW] 가장 최근 대화 1개 조회 (마지막 맥락 찾기용)
    Optional<AiChatHistoryEntity> findTopByModel_IdAndUserIdOrderByCreatedAtDesc(Long modelId, String userId);
}