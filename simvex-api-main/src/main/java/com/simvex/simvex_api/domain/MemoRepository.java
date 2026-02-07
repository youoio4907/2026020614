package com.simvex.simvex_api.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MemoRepository extends JpaRepository<MemoEntity, Long> {
    List<MemoEntity> findByModelIdOrderByIdAsc(Long modelId);
}