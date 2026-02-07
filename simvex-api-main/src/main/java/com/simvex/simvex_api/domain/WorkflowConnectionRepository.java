package com.simvex.simvex_api.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface WorkflowConnectionRepository extends JpaRepository<WorkflowConnectionEntity, Long> {
    // 특정 노드(ID)가 출발지(from)이거나 도착지(to)인 연결선을 모두 삭제
    @Transactional
    void deleteByFromNodeIdOrToNodeId(Long fromNodeId, Long toNodeId);
}