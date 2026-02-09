package com.simvex.simvex_api.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkflowNodeRepository extends JpaRepository<WorkflowNodeEntity, Long> {
    List<WorkflowNodeEntity> findAllByUserId(String userId);
}