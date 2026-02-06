// src/main/java/com/simvex/simvex_api/model/ModelEntity.java
package com.simvex.simvex_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "models")
public class ModelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(name = "model_url", nullable = false)
    private String modelUrl;

    protected ModelEntity() {
        // JPA 용
    }

    public ModelEntity(String title, String modelUrl) {
        this.title = title;
        this.modelUrl = modelUrl;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getModelUrl() {
        return modelUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setModelUrl(String modelUrl) {
        this.modelUrl = modelUrl;
    }
}
