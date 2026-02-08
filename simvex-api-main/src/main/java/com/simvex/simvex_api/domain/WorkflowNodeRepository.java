package com.simvex.simvex_api.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowNodeRepository extends JpaRepository<WorkflowNodeEntity, Long> {
}