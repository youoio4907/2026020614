// src/main/java/com/simvex/simvex_api/part/PartEntity.java
package com.simvex.simvex_api.part;

import com.simvex.simvex_api.model.ModelEntity;
import jakarta.persistence.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(
        name = "model_parts",
        uniqueConstraints = @UniqueConstraint(name = "uk_model_mesh", columnNames = {"model_id", "mesh_name"})
)
public class PartEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mesh_name", nullable = false)
    private String meshName;

    /**
     * DB: jsonb
     * Hibernate 6 + PostgreSQL 에서 jsonb 를 "Map" 으로 안전하게 쓰려면
     * @JdbcTypeCode(SqlTypes.JSON) 를 쓰는 게 가장 간단하다.
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> content = new LinkedHashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private ModelEntity model;

    protected PartEntity() {
        // JPA 용
    }

    public PartEntity(ModelEntity model, String meshName, Map<String, Object> content) {
        this.model = model;
        this.meshName = meshName;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public String getMeshName() {
        return meshName;
    }

    public void setMeshName(String meshName) {
        this.meshName = meshName;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public void setContent(Map<String, Object> content) {
        this.content = content;
    }

    public ModelEntity getModel() {
        return model;
    }

    public void setModel(ModelEntity model) {
        this.model = model;
    }
}
