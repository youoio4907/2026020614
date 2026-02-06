// src/main/java/com/simvex/simvex_api/part/PartRepository.java
package com.simvex.simvex_api.part;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartRepository extends JpaRepository<PartEntity, Long> {

    // 모델 선택 시 부품 목록
    List<PartEntity> findByModelIdOrderByIdAsc(Long modelId);

    // 추가: AI ask 용 단건 조회
    Optional<PartEntity> findByModel_IdAndMeshName(Long modelId, String meshName);
}
