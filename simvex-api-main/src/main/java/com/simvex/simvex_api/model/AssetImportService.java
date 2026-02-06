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

            // --------------------------------------------------------
            // [신규] 1. 모델 메타데이터 업데이트 (설명, AI요약)
            // --------------------------------------------------------
            if (root.containsKey("description")) {
                model.setDescription(asString(root.get("description")));
            }
            if (root.containsKey("ai_summary")) {
                model.setAiSummary(asString(root.get("ai_summary")));
            }

            // --------------------------------------------------------
            // [신규] 2. 퀴즈 데이터 저장
            // --------------------------------------------------------
            List<Map<String, Object>> quizzes = asListOfMap(root.get("quizzes"));
            
            if (!quizzes.isEmpty() && model.getQuizzes().isEmpty()) {
                for (Map<String, Object> q : quizzes) {
                    // 1. 문제 (key: "q" 또는 "question")
                    String question = firstNonBlank(
                            asString(q.get("q")), 
                            asString(q.get("question"))
                    );

                    // 2. 정답 (key: "ans" 또는 "answer")
                    int answer = 0;
                    try {
                        String ansStr = firstNonBlank(asString(q.get("ans")), asString(q.get("answer")));
                        answer = Integer.parseInt(ansStr);
                    } catch (NumberFormatException e) {
                        answer = 0; // 파싱 실패 시 기본값 0
                    }

                    // 3. 보기 (key: "opts") -> List<String> 변환
                    List<String> options = new ArrayList<>();
                    if (q.get("opts") instanceof List<?> list) {
                        for (Object o : list) {
                            options.add(asString(o));
                        }
                    }

                    // 4. 해설
                    String explanation = asString(q.get("explanation"));

                    // Entity 생성 및 추가
                    model.addQuiz(new QuizEntity(question, answer, options, explanation));
                }
            }

            // --------------------------------------------------------
            // [신규] 3. 메모 데이터 저장
            // --------------------------------------------------------
            List<Map<String, Object>> memos = asListOfMap(root.get("memos"));
            if (!memos.isEmpty() && model.getMemos().isEmpty()) {
                for (Map<String, Object> m : memos) {
                    String title = asString(m.get("title"));
                    String content = asString(m.get("content"));
                    model.addMemo(new MemoEntity(title, content));
                }
            }
            
            // 변경된 모델 정보(퀴즈, 메모 포함) 저장
            modelRepository.save(model);

            // --------------------------------------------------------
            // [수정] 4. 부품(Assets) 처리 및 경로 버그 수정
            // --------------------------------------------------------
            
            // 이 모델의 folderName 은 modelUrl 기준으로 뽑는다 (/assets/3d/Drone/Drone.glb -> Drone)
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

                // [삭제] raw 데이터는 용량 문제 및 중복으로 저장하지 않음
                // content.put("raw", a); 

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

        for (ModelEntity m : existing) {
            String folder = extractFolderName(m.getModelUrl(), m.getTitle());
            if (norm(folder).equals(n1) || norm(folder).equals(n2)) return m;
        }

        return null;
    }

    // [수정됨] 경로 파싱 로직 개선
    private String extractFolderName(String modelUrl, String fallback) {
        if (modelUrl == null || modelUrl.isBlank()) return fallback;
        
        String s = modelUrl;

        // .glb 같은 확장자가 포함된 경우, 파일명 부분을 제거하고 상위 폴더를 찾는다.
        // 예: /assets/3d/Drone/Drone.glb -> /assets/3d/Drone/
        if (s.toLowerCase().endsWith(".glb") || s.toLowerCase().endsWith(".gltf")) {
            int lastSlash = s.lastIndexOf('/');
            if (lastSlash > 0) {
                s = s.substring(0, lastSlash); 
            }
        }

        // 끝의 슬래시 제거
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);

        // 마지막 경로(폴더명) 추출
        int idx = s.lastIndexOf('/');
        if (idx >= 0 && idx < s.length() - 1) {
            return s.substring(idx + 1);
        }
        return fallback;
    }

    private String norm(String s) {
        if (s == null) return "";
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