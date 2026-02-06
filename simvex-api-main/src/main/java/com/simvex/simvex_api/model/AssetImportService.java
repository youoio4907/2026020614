// src/main/java/com/simvex/simvex_api/bootstrap/AssetImportService.java
package com.simvex.simvex_api.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simvex.simvex_api.part.PartEntity;
import com.simvex.simvex_api.part.PartRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AssetImportService {

    private final ObjectMapper objectMapper;
    private final ModelRepository modelRepository;
    private final PartRepository partRepository;

    public AssetImportService(
            ObjectMapper objectMapper,
            ModelRepository modelRepository,
            PartRepository partRepository
    ) {
        this.objectMapper = objectMapper;
        this.modelRepository = modelRepository;
        this.partRepository = partRepository;
    }

    @Transactional
    public void importAllFromResources() throws Exception {
        // resources/import/Data_*.json 전부 스캔
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:/import/Data_*.json");

        if (resources.length == 0) {
            System.out.println("[IMPORT] classpath:/import/Data_*.json 파일이 없다. import 스킵한다.");
            return;
        }

        // 기존 모델들 한번에 로딩(정규화 매칭용)
        List<ModelEntity> existingModels = modelRepository.findAll();

        for (Resource r : resources) {
            Map<String, Object> root = objectMapper.readValue(
                    r.getInputStream(),
                    new TypeReference<Map<String, Object>>() {}
            );

            String jsonFileName = Optional.ofNullable(r.getFilename()).orElse("unknown.json");
            String integratedFile = asString(root.get("integrated_file")); // ex) Drone.glb
            List<Map<String, Object>> assets = asListOfMap(root.get("assets"));

            // "Data_V4Engine.json" -> "V4Engine"
            String rawKey = jsonFileName
                    .replace("Data_", "")
                    .replace(".json", "");

            // 모델 찾기(정규화해서 비교)
            ModelEntity model = findBestModelMatch(existingModels, rawKey, integratedFile);

            if (model == null) {
                System.out.println("[IMPORT] 모델 매칭 실패라 스킵: " + rawKey);
                continue;
            } else {
                System.out.println("[IMPORT] 모델 매칭: " + model.getTitle() + " <= " + rawKey);
            }

            // 이 모델의 folderName 은 modelUrl 기준으로 뽑는다 (/assets/3d/Drone/ -> Drone)
            String folderName = extractFolderName(model.getModelUrl(), model.getTitle());
            String fileUrl = "/assets/3d/" + folderName + "/" + integratedFile;

            // 부품 upsert
            for (Map<String, Object> a : assets) {
                String meshName = firstNonBlank(
                        asString(a.get("title")),
                        asString(a.get("id"))
                );
                if (meshName == null || meshName.isBlank()) continue;

                Map<String, Object> content = new LinkedHashMap<>();
                content.put("name", meshName);
                content.put("type", "part");
                content.put("fileUrl", fileUrl);
                content.put("integratedFile", integratedFile);

                // desc/transform 정보 포함
                content.put("description", asString(a.get("desc")));
                content.put("position", a.get("position"));
                content.put("vector", a.get("vector"));
                content.put("explodeVector", a.get("explodeVector"));

                // raw 그대로도 남겨두면 추후 확장 쉬움
                content.put("raw", a);

                // uk_model_mesh(모델+meshName) 기준으로 upsert
                PartEntity part = findPart(model.getId(), meshName).orElseGet(() ->
                        new PartEntity(model, meshName, new LinkedHashMap<>())
                );
                part.setMeshName(meshName);
                part.setContent(content);
                part.setModel(model);

                partRepository.save(part);
            }

            System.out.println("[IMPORT] 완료: " + jsonFileName + " (assets=" + assets.size() + ")");
        }
    }

    private Optional<PartEntity> findPart(Long modelId, String meshName) {
        // PartRepository 에 "findByModelIdOrderByIdAsc"만 있으니까, 여기선 전체 가져와서 찾는다.
        // 데이터 많아지면 PartRepository에 findByModelIdAndMeshName 추가하는 게 맞다.
        return partRepository.findByModelIdOrderByIdAsc(modelId).stream()
                .filter(p -> meshName.equals(p.getMeshName()))
                .findFirst();
    }

    private ModelEntity findBestModelMatch(List<ModelEntity> existing, String rawKey, String integratedFile) {
        String n1 = norm(rawKey);
        String n2 = norm(integratedFile.replace(".glb", ""));

        for (ModelEntity m : existing) {
            String mt = norm(m.getTitle());
            if (mt.equals(n1) || mt.equals(n2)) return m;
        }

        // modelUrl 에도 폴더명이 들어있으니까 그걸로 한번 더 매칭
        for (ModelEntity m : existing) {
            String folder = extractFolderName(m.getModelUrl(), m.getTitle());
            if (norm(folder).equals(n1) || norm(folder).equals(n2)) return m;
        }

        return null;
    }

    private String extractFolderName(String modelUrl, String fallback) {
        if (modelUrl == null) return fallback;
        // /assets/3d/V4_Engine/ -> V4_Engine
        String s = modelUrl;
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        int idx = s.lastIndexOf('/');
        if (idx >= 0 && idx < s.length() - 1) return s.substring(idx + 1);
        return fallback;
    }

    private String norm(String s) {
        if (s == null) return "";
        // 대소문자/언더스코어/공백/하이픈 제거하고 비교
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMap(Object o) {
        if (o instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object it : list) {
                if (it instanceof Map<?, ?> m) {
                    Map<String, Object> mm = new LinkedHashMap<>();
                    for (var e : m.entrySet()) {
                        mm.put(String.valueOf(e.getKey()), e.getValue());
                    }
                    out.add(mm);
                }
            }
            return out;
        }
        return List.of();
    }

    private String firstNonBlank(String... arr) {
        for (String s : arr) {
            if (s != null && !s.isBlank()) return s;
        }
        return null;
    }
}
