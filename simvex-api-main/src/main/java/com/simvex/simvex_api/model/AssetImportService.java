package com.simvex.simvex_api.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simvex.simvex_api.domain.MemoEntity;
import com.simvex.simvex_api.domain.QuizEntity;
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
            PartRepository partRepository) {
        this.objectMapper = objectMapper;
        this.modelRepository = modelRepository;
        this.partRepository = partRepository;
    }

    @Transactional
    public void importAllFromResources() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:/import/Data_*.json");

        if (resources.length == 0) {
            System.out.println("[IMPORT] classpath:/import/Data_*.json 파일이 없습니다.");
            return;
        }

        List<ModelEntity> existingModels = modelRepository.findAll();

        for (Resource r : resources) {
            Map<String, Object> root = objectMapper.readValue(
                    r.getInputStream(),
                    new TypeReference<Map<String, Object>>() {
                    });

            String jsonFileName = Optional.ofNullable(r.getFilename()).orElse("unknown.json");
            String integratedFile = asString(root.get("integrated_file"));
            List<Map<String, Object>> assets = asListOfMap(root.get("assets"));

            String rawKey = jsonFileName.replace("Data_", "").replace(".json", "");

            ModelEntity model = findBestModelMatch(existingModels, rawKey, integratedFile);

            if (model == null) {
                System.out.println("[IMPORT] 모델 매칭 실패 (Skip): " + rawKey);
                continue;
            } else {
                System.out.println("[IMPORT] 데이터 동기화 시작: " + model.getTitle());
            }

            // ========================================================
            // [수정 1] 모델 설명 & AI 요약 (값이 없을 때만 초기화)
            // ========================================================
            if (root.containsKey("description")) {
                String currentDesc = model.getDescription();
                String jsonDesc = asString(root.get("description"));
                // DB가 비어있고, JSON에 값이 있을 때만 저장
                if ((currentDesc == null || currentDesc.isBlank()) &&
                        (jsonDesc != null && !jsonDesc.isBlank())) {
                    model.setDescription(jsonDesc);
                }
                // model.setDescription(jsonDesc);
            }

            if (root.containsKey("ai_summary")) {
                String currentSummary = model.getAiSummary();
                String jsonSummary = asString(root.get("ai_summary"));
                // DB가 비어있고, JSON에 값이 있을 때만 저장
                if ((currentSummary == null || currentSummary.isBlank()) &&
                        (jsonSummary != null && !jsonSummary.isBlank())) {
                    model.setAiSummary(jsonSummary);
                }
            }

            // ========================================================
            // [유지] 퀴즈 데이터 (이미 DB에 있으면 건너뜀)
            // ========================================================
            List<Map<String, Object>> quizzes = asListOfMap(root.get("quizzes"));
            if (!quizzes.isEmpty() && model.getQuizzes().isEmpty()) {
                for (Map<String, Object> q : quizzes) {
                    String question = firstNonBlank(asString(q.get("q")), asString(q.get("question")));
                    int answer = 0;
                    try {
                        String ansStr = firstNonBlank(asString(q.get("ans")), asString(q.get("answer")));
                        answer = Integer.parseInt(ansStr);
                    } catch (NumberFormatException e) {
                        answer = 0;
                    }

                    List<String> options = new ArrayList<>();
                    if (q.get("opts") instanceof List<?> list) {
                        for (Object o : list)
                            options.add(asString(o));
                    }
                    String explanation = asString(q.get("explanation"));
                    model.addQuiz(new QuizEntity(question, answer, options, explanation));
                }
            }

            // ========================================================
            // [유지] 메모 데이터 (이미 DB에 있으면 건너뜀)
            // ========================================================
            List<Map<String, Object>> memos = asListOfMap(root.get("memos"));
            if (!memos.isEmpty() && model.getMemos().isEmpty()) {
                for (Map<String, Object> m : memos) {
                    model.addMemo(new MemoEntity(asString(m.get("title")), asString(m.get("content"))));
                }
            }

            modelRepository.save(model);

            // ========================================================
            // [수정 2] 부품(Assets) 처리 (이미 존재하면 덮어쓰지 않음)
            // ========================================================
            String folderName = extractFolderName(model.getModelUrl(), model.getTitle());
            String fileUrl = "/assets/3d/" + folderName + "/" + integratedFile;

            for (Map<String, Object> a : assets) {
                String meshName = firstNonBlank(asString(a.get("title")), asString(a.get("id")));
                if (meshName == null || meshName.isBlank())
                    continue;

                // [핵심 변경] 이미 DB에 해당 부품이 존재하면 업데이트 하지 않고 건너뜀
                Optional<PartEntity> existingPart = findPart(model.getId(), meshName);
                if (existingPart.isPresent()) {
                    continue; // 덮어쓰기 방지 (Skip)
                }

                // 존재하지 않을 때만 신규 생성
                Map<String, Object> content = new LinkedHashMap<>();
                content.put("name", meshName);
                content.put("type", "part");
                content.put("fileUrl", fileUrl);
                content.put("integratedFile", integratedFile);
                content.put("description", asString(a.get("desc")));
                content.put("position", a.get("position"));
                content.put("vector", a.get("vector"));
                content.put("explodeVector", a.get("explodeVector"));

                PartEntity part = new PartEntity(model, meshName, content);
                partRepository.save(part);
            }

            System.out.println("[IMPORT] 완료 (보존 모드): " + jsonFileName);
        }
    }

    private Optional<PartEntity> findPart(Long modelId, String meshName) {
        return partRepository.findByModelIdOrderByIdAsc(modelId).stream()
                .filter(p -> meshName.equals(p.getMeshName()))
                .findFirst();
    }

    private ModelEntity findBestModelMatch(List<ModelEntity> existing, String rawKey, String integratedFile) {
        String n1 = norm(rawKey);
        String n2 = norm(integratedFile.replace(".glb", ""));
        for (ModelEntity m : existing) {
            String mt = norm(m.getTitle());
            if (mt.equals(n1) || mt.equals(n2))
                return m;
        }
        for (ModelEntity m : existing) {
            String folder = extractFolderName(m.getModelUrl(), m.getTitle());
            if (norm(folder).equals(n1) || norm(folder).equals(n2))
                return m;
        }
        return null;
    }

    private String extractFolderName(String modelUrl, String fallback) {
        if (modelUrl == null || modelUrl.isBlank())
            return fallback;
        String s = modelUrl;
        if (s.toLowerCase().endsWith(".glb") || s.toLowerCase().endsWith(".gltf")) {
            int lastSlash = s.lastIndexOf('/');
            if (lastSlash > 0)
                s = s.substring(0, lastSlash);
        }
        if (s.endsWith("/"))
            s = s.substring(0, s.length() - 1);
        int idx = s.lastIndexOf('/');
        if (idx >= 0 && idx < s.length() - 1)
            return s.substring(idx + 1);
        return fallback;
    }

    private String norm(String s) {
        if (s == null)
            return "";
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
                    for (var e : m.entrySet())
                        mm.put(String.valueOf(e.getKey()), e.getValue());
                    out.add(mm);
                }
            }
            return out;
        }
        return List.of();
    }

    private String firstNonBlank(String... arr) {
        for (String s : arr)
            if (s != null && !s.isBlank())
                return s;
        return null;
    }
}