// src/main/java/com/simvex/simvex_api/model/ModelEntity.java
package com.simvex.simvex_api.model;

import com.simvex.simvex_api.part.PartEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "models",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_models_domain_category_slug", columnNames = {"domain_key", "category_key", "slug"})
        }
)
public class ModelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "model_url", nullable = false)
    private String modelUrl;

    // ✅ B 필드 (추가)
    @Column(name = "domain_key")
    private String domainKey;

    @Column(name = "category_key")
    private String categoryKey;

    @Column(name = "slug")
    private String slug;

    @OneToMany(mappedBy = "model", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartEntity> parts = new ArrayList<>();

    protected ModelEntity() {}

    public ModelEntity(String title, String modelUrl) {
        this.title = title;
        this.modelUrl = modelUrl;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getModelUrl() { return modelUrl; }
    public List<PartEntity> getParts() { return parts; }

    public String getDomainKey() { return domainKey; }
    public String getCategoryKey() { return categoryKey; }
    public String getSlug() { return slug; }

    public void setTitle(String title) { this.title = title; }
    public void setModelUrl(String modelUrl) { this.modelUrl = modelUrl; }

    public void setDomainKey(String domainKey) { this.domainKey = domainKey; }
    public void setCategoryKey(String categoryKey) { this.categoryKey = categoryKey; }
    public void setSlug(String slug) { this.slug = slug; }

    public void addPart(PartEntity part) {
        if (part == null) return;
        parts.add(part);
        part.setModel(this);
    }

    public void removePart(PartEntity part) {
        if (part == null) return;
        parts.remove(part);
        part.setModel(null);
    }
}
