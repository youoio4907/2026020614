package com.simvex.simvex_api.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface WorkflowConnectionRepository extends JpaRepository<WorkflowConnectionEntity, Long> {
    List<WorkflowConnectionEntity> findAllByUserId(String userId);
    
    @Transactional
    void deleteByFromNodeIdOrToNodeId(Long fromNodeId, Long toNodeId);
}