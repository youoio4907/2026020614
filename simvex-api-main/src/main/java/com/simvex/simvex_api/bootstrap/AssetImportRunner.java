package com.simvex.simvex_api.bootstrap;

import com.simvex.simvex_api.model.AssetImportService;
import com.simvex.simvex_api.model.ModelEntity;
import com.simvex.simvex_api.model.ModelRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Component
public class AssetImportRunner implements CommandLineRunner {

    private final AssetImportService assetImportService;
    private final ModelRepository modelRepository;

    public AssetImportRunner(AssetImportService assetImportService, ModelRepository modelRepository) {
        this.assetImportService = assetImportService;
        this.modelRepository = modelRepository;
    }

    @Override
    public void run(String... args) {
        try {
            System.out.println("========== [SimVex 초기화] 시작 ==========");
            
            // 1. 폴더 스캔 및 DB 등록 (공백 -> 언더바 강제 변환)
            initializeModelsFromProjectDir();

            // 2. 부품 데이터 연결
            assetImportService.importAllFromResources();
            
            System.out.println("========== [SimVex 초기화] 완료 ==========");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Transactional
    protected void initializeModelsFromProjectDir() {
        String userDir = System.getProperty("user.dir");
        // 경로 찾기
        Path assetsDir = findPath(userDir, "src/main/resources/static/assets/3d");
        
        if (assetsDir == null || !Files.exists(assetsDir)) {
             // 혹시 simvex-api 폴더 안에 있을 경우
             assetsDir = findPath(userDir, "simvex-api-main/src/main/resources/static/assets/3d");
        }

        if (assetsDir == null) {
            System.out.println("⚠️ 3D 폴더를 찾을 수 없습니다.");
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(assetsDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    String rawFolderName = entry.getFileName().toString();
                    
                    // [핵심] 폴더명에 공백이 있든 없든, DB에는 무조건 언더바로 저장하여 통일
                    String standardizedTitle = rawFolderName.replace(" ", "_");
                    
                    // URL 생성 (파일명도 .glb로 가정)
                    String correctUrl = "/assets/3d/" + standardizedTitle + "/" + standardizedTitle + ".glb";

                    createOrUpdateModel(standardizedTitle, correctUrl);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Path findPath(String base, String sub) {
        Path p = Paths.get(base, sub);
        return Files.exists(p) ? p : null;
    }

    private void createOrUpdateModel(String title, String url) {
        Optional<ModelEntity> existing = modelRepository.findByTitle(title);
        if (existing.isEmpty()) {
            modelRepository.save(new ModelEntity(title, url));
            System.out.println("✅ 모델 생성: " + title);
        } else {
            ModelEntity m = existing.get();
            if (!m.getModelUrl().equals(url)) {
                m.setModelUrl(url);
                modelRepository.save(m);
                System.out.println("🔄 모델 URL 업데이트: " + title);
            }
        }
    }
}