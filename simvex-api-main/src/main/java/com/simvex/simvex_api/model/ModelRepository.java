// src/main/java/com/simvex/simvex_api/model/ModelRepository.java
package com.simvex.simvex_api.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<ModelEntity, Long> {
    Optional<ModelEntity> findByTitle(String title);
    /**
     * 특정 도메인의 모든 모델 조회 (ID 오름차순)
     */
    List<ModelEntity> findByDomainKeyOrderByIdAsc(String domainKey);

    /**
     * 도메인 + 카테고리 + 슬러그로 단일 모델 조회
     */
    Optional<ModelEntity> findByDomainKeyAndCategoryKeyAndSlug(
            String domainKey,
            String categoryKey,
            String slug
    );

    /**
     * 🆕 신규 추가: 특정 도메인 + 카테고리의 모든 모델 조회
     * Productlistpage.js에서 사용
     */
    List<ModelEntity> findByDomainKeyAndCategoryKeyOrderByIdAsc(
            String domainKey,
            String categoryKey
    );
}