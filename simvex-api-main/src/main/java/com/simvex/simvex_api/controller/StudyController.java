// src/main/java/com/simvex/simvex_api/controller/StudyController.java
package com.simvex.simvex_api.controller;

import com.simvex.simvex_api.dto.PartDto;
import com.simvex.simvex_api.dto.StudyBundleDto;
import com.simvex.simvex_api.dto.StudyCatalogDto;
import com.simvex.simvex_api.model.ModelEntity;
import com.simvex.simvex_api.model.ModelRepository;
import com.simvex.simvex_api.part.PartRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/study")
@CrossOrigin(origins = "*")
public class StudyController {

    private final ModelRepository modelRepository;
    private final PartRepository partRepository;

    public StudyController(ModelRepository modelRepository, PartRepository partRepository) {
        this.modelRepository = modelRepository;
        this.partRepository = partRepository;
    }

    /**
     * GET /api/study/catalog?domain=engineering-dict
     * 특정 도메인의 전체 카탈로그 (카테고리별로 그룹화된 모델 목록)
     */
    @GetMapping("/catalog")
    public ResponseEntity<StudyCatalogDto> catalog(@RequestParam("domain") String domainKey) {
        if (domainKey == null || domainKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<ModelEntity> models = modelRepository.findByDomainKeyOrderByIdAsc(domainKey);

        // 카테고리별로 그룹화
        Map<String, List<ModelEntity>> byCategory = models.stream()
                .collect(Collectors.groupingBy(
                        m -> safe(m.getCategoryKey(), "mechanics"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        StudyCatalogDto dto = new StudyCatalogDto();
        dto.domainKey = domainKey;
        dto.categories = new ArrayList<>();

        for (var e : byCategory.entrySet()) {
            StudyCatalogDto.CategoryDto c = new StudyCatalogDto.CategoryDto();
            c.categoryKey = e.getKey();
            c.title = e.getKey(); // MVP: key를 title로 사용 (추후 개선 가능)
            c.models = e.getValue().stream().map(this::toItem).toList();
            dto.categories.add(c);
        }

        return ResponseEntity.ok(dto);
    }

    /**
     * 🆕 신규 추가: GET /api/study/{domainKey}/{categoryKey}/models
     * 특정 카테고리의 모델 목록만 반환 (Productlistpage.js용)
     */
    @GetMapping("/{domainKey}/{categoryKey}/models")
    public ResponseEntity<List<StudyCatalogDto.ModelItemDto>> listModelsByCategory(
            @PathVariable String domainKey,
            @PathVariable String categoryKey
    ) {
        if (domainKey == null || domainKey.isBlank() ||
                categoryKey == null || categoryKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<ModelEntity> models = modelRepository
                .findByDomainKeyAndCategoryKeyOrderByIdAsc(domainKey, categoryKey);

        List<StudyCatalogDto.ModelItemDto> result = models.stream()
                .map(this::toItem)
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/study/{domainKey}/{categoryKey}/{modelSlug}/bundle
     * 특정 모델의 상세 정보 + 부품 목록 (Learnpage.js용)
     */
    @GetMapping("/{domainKey}/{categoryKey}/{modelSlug}/bundle")
    public ResponseEntity<StudyBundleDto> bundle(
            @PathVariable String domainKey,
            @PathVariable String categoryKey,
            @PathVariable String modelSlug
    ) {
        ModelEntity model = modelRepository
                .findByDomainKeyAndCategoryKeyAndSlug(domainKey, categoryKey, modelSlug)
                .orElse(null);

        if (model == null) {
            return ResponseEntity.notFound().build();
        }

        var parts = partRepository.findByModelIdOrderByIdAsc(model.getId())
                .stream()
                .map(PartDto::from)
                .toList();

        StudyBundleDto dto = new StudyBundleDto();
        dto.model = toStudyModel(model);
        dto.parts = parts;

        return ResponseEntity.ok(dto);
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼 메서드
    // ─────────────────────────────────────────────────────────────

    private StudyCatalogDto.ModelItemDto toItem(ModelEntity m) {
        StudyCatalogDto.ModelItemDto x = new StudyCatalogDto.ModelItemDto();
        x.id = m.getId();
        x.title = m.getTitle();
        x.slug = m.getSlug();
        x.modelUrl = m.getModelUrl();
        x.domainKey = m.getDomainKey();
        x.categoryKey = m.getCategoryKey();
        return x;
    }

    private StudyBundleDto.StudyModelDto toStudyModel(ModelEntity m) {
        StudyBundleDto.StudyModelDto x = new StudyBundleDto.StudyModelDto();
        x.id = m.getId();
        x.title = m.getTitle();
        x.slug = m.getSlug();
        x.modelUrl = m.getModelUrl();
        x.domainKey = m.getDomainKey();
        x.categoryKey = m.getCategoryKey();
        return x;
    }

    private String safe(String s, String fallback) {
        if (s == null || s.isBlank()) return fallback;
        return s;
    }
}