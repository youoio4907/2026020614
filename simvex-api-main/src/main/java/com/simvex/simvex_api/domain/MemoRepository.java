package com.simvex.simvex_api.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MemoRepository extends JpaRepository<MemoEntity, Long> {
    // [변경] 모델 ID + 사용자 ID로 조회
    List<MemoEntity> findByModelIdAndUserIdOrderByIdAsc(Long modelId, String userId);
}