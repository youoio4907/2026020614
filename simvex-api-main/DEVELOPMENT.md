# SIMVEX API - 개발 가이드

> 📖 **문서 네비게이션**
> - 👈 **프로젝트 소개**: [README.md](README.md)로 돌아가기
> - 🔧 **설치 가이드**: [SETUP.md](SETUP.md)에서 환경 구축
> - 🐛 **문제 발생 시**: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) 참고

이 문서는 SIMVEX API 개발에 필요한 모든 정보를 담고 있습니다.

---

## 📋 목차

1. [프로젝트 아키텍처](#1-프로젝트-아키텍처)
2. [데이터베이스 스키마](#2-데이터베이스-스키마)
3. [AI 시스템](#3-ai-시스템)
4. [Asset Import 시스템](#4-asset-import-시스템)
5. [API 상세 명세](#5-api-상세-명세)
6. [개발 워크플로우](#6-개발-워크플로우)
7. [코드 컨벤션](#7-코드-컨벤션)
8. [테스트](#8-테스트)
9. [성능 최적화](#9-성능-최적화)
10. [배포](#10-배포)

---

## 1. 프로젝트 아키텍처

### 1-1. 전체 시스템 구조

```
┌─────────────────────────────────────────────────────────────┐
│                Frontend (React + Three.js)                   │
│                     Port 3000                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Learnpage.js │  │ThreeViewer.jsx│  │ Productlist  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP/REST (setupProxy.js)
                          ↓
┌─────────────────────────────────────────────────────────────┐
│              Backend (Spring Boot) - Port 8080               │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │           Controller Layer (REST API)              │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────┐│    │
│  │  │StudyController│  │ModelController│  │AiController│    │
│  │  └──────────────┘  └──────────────┘  └──────────┘│    │
│  └────────────────────┬───────────────────────────────┘    │
│                       ↓                                     │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Service Layer                          │    │
│  │  ┌─────────────────────────────────────────────┐  │    │
│  │  │ AiService                                   │  │    │
│  │  │  - buildContext()   컨텍스트 구성           │  │    │
│  │  │  - composePrompt()  프롬프트 생성           │  │    │
│  │  │  - generateAnswer() OpenAI/Mock 호출        │  │    │
│  │  └─────────────────────────────────────────────┘  │    │
│  │  ┌─────────────────────────────────────────────┐  │    │
│  │  │ AssetImportService                          │  │    │
│  │  │  - importAllFromResources() 에셋 임포트    │  │    │
│  │  │  - findBestModelMatch()     모델 매칭      │  │    │
│  │  └─────────────────────────────────────────────┘  │    │
│  └────────────────────┬───────────────────────────────┘    │
│                       ↓                                     │
│  ┌────────────────────────────────────────────────────┐    │
│  │         Repository Layer (Data Access)             │    │
│  │  ┌──────────────┐  ┌──────────────┐              │    │
│  │  │ModelRepository│  │PartRepository│              │    │
│  │  │ JpaRepository │  │ JpaRepository │              │    │
│  │  └──────────────┘  └──────────────┘              │    │
│  └────────────────────┬───────────────────────────────┘    │
│                       ↓                                     │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Entity Layer                           │    │
│  │  ┌──────────────┐  ┌──────────────┐              │    │
│  │  │ ModelEntity  │  │  PartEntity  │              │    │
│  │  │ @Entity      │  │  @Entity     │              │    │
│  │  │ models 테이블│  │ model_parts  │              │    │
│  │  └──────────────┘  └──────────────┘              │    │
│  └────────────────────────────────────────────────────┘    │
└──────────────────────┬──────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────────┐
│            PostgreSQL 15 (Docker Container)                  │
│                                                              │
│  ┌──────────────┐  ┌──────────────────────────────┐       │
│  │ models       │  │ model_parts                  │       │
│  │              │  │                              │       │
│  │ id (PK)      │  │ id (PK)                      │       │
│  │ title        │  │ model_id (FK)                │       │
│  │ model_url    │  │ mesh_name                    │       │
│  │ domain_key   │  │ content (JSONB)              │       │
│  │ category_key │  │                              │       │
│  │ slug         │  │ UK: (model_id, mesh_name)    │       │
│  └──────────────┘  └──────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘

                       +

┌─────────────────────────────────────────────────────────────┐
│                    OpenAI API                                │
│                                                              │
│  Endpoint: POST https://api.openai.com/v1/responses        │
│  Model: gpt-4-mini                                          │
│  Auth: Bearer ${OPENAI_API_KEY}                             │
└─────────────────────────────────────────────────────────────┘

                       +

┌─────────────────────────────────────────────────────────────┐
│              File System (3D Assets)                         │
│                                                              │
│  src/main/resources/static/assets/3d/                       │
│  ├─ V4_Engine/                                              │
│  │   ├─ V4_Engine.glb       ← 통합 모델                     │
│  │   ├─ Crankshaft.glb      ← 개별 부품                     │
│  │   └─ assembly.png        ← 조립도                        │
│  ├─ Robot_Arm/                                              │
│  └─ Drone/                                                  │
└─────────────────────────────────────────────────────────────┘
```

### 1-2. 레이어별 책임

| 레이어 | 책임 | 예시 |
|--------|------|------|
| **Controller** | HTTP 요청/응답 처리 | `@GetMapping`, `@PostMapping` |
| **Service** | 비즈니스 로직 | AI 컨텍스트 구성, 에셋 임포트 |
| **Repository** | 데이터 접근 | JPA 쿼리 실행 |
| **Entity** | 데이터 모델 | DB 테이블 매핑 |
| **DTO** | 데이터 전송 | API 응답 형식 |

### 1-3. 패키지 구조

```
com.simvex.simvex_api/
├── ai/                      # AI 관련 (독립적 모듈)
│   ├── AiService.java
│   ├── AiClient.java        # 인터페이스
│   ├── OpenAIClient.java    # 구현체 1
│   ├── MockAiClient.java    # 구현체 2
│   ├── AiContextResult.java
│   ├── AiAnswerCache.java
│   ├── PromptTemplateService.java
│   └── OpenAIConfig.java
│
├── bootstrap/               # 초기화 로직
│   └── AssetImportRunner.java  # CommandLineRunner
│
├── controller/              # REST API 엔드포인트
│   ├── StudyController.java
│   ├── ModelController.java
│   └── AiController.java
│
├── model/                   # 모델 도메인
│   ├── ModelEntity.java
│   ├── ModelRepository.java
│   └── AssetImportService.java
│
├── part/                    # 부품 도메인
│   ├── PartEntity.java
│   └── PartRepository.java
│
└── dto/                     # 데이터 전송 객체
    ├── StudyCatalogDto.java
    ├── StudyBundleDto.java
    ├── PartDto.java
    ├── ModelDto.java
    ├── AiAskRequestDto.java
    └── AiAskResponseDto.java
```

---

## 2. 데이터베이스 스키마

### 2-1. models 테이블

```sql
CREATE TABLE models (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    model_url VARCHAR(500) NOT NULL,
    domain_key VARCHAR(100),
    category_key VARCHAR(100),
    slug VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_models_domain_category_slug 
        UNIQUE (domain_key, category_key, slug)
);
```

**컬럼 설명**:

| 컬럼 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `id` | BIGINT | 기본 키 (자동 증가) | 1 |
| `title` | VARCHAR | 모델 제목 | "V4_Engine" |
| `model_url` | VARCHAR | GLB 파일 경로 | "/assets/3d/V4_Engine/" |
| `domain_key` | VARCHAR | 도메인 구분 | "engineering-dict" |
| `category_key` | VARCHAR | 카테고리 구분 | "mechanics" |
| `slug` | VARCHAR | URL 친화적 식별자 | "v4-engine" |

**Unique 제약**:
- `(domain_key, category_key, slug)` 조합이 유니크해야 함
- URL 충돌 방지

### 2-2. model_parts 테이블

```sql
CREATE TABLE model_parts (
    id BIGSERIAL PRIMARY KEY,
    model_id BIGINT NOT NULL,
    mesh_name VARCHAR(255) NOT NULL,
    content JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_model_parts_model 
        FOREIGN KEY (model_id) REFERENCES models(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT uk_model_mesh 
        UNIQUE (model_id, mesh_name)
);

CREATE INDEX idx_model_parts_model_id ON model_parts(model_id);
CREATE INDEX idx_model_parts_content ON model_parts USING GIN(content);
```

**컬럼 설명**:

| 컬럼 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `id` | BIGINT | 기본 키 | 1 |
| `model_id` | BIGINT | 모델 외래 키 | 1 |
| `mesh_name` | VARCHAR | GLB 메쉬 이름 ⭐ | "Crankshaft" |
| `content` | JSONB | 부품 메타데이터 | `{...}` |

**Unique 제약**:
- `(model_id, mesh_name)` 조합이 유니크해야 함
- 같은 모델 내에서 부품 이름 중복 방지

**JSONB content 구조**:

```json
{
  "name": "크랭크축",
  "type": "part",
  "fileUrl": "/assets/3d/V4_Engine/V4_Engine.glb",
  "integratedFile": "V4_Engine.glb",
  "description": "피스톤의 왕복 운동을 회전 운동으로 변환",
  "position": [0, 0, 0],
  "vector": [1, 0, 0],
  "explodeVector": [0, 1, 0],
  "raw": {
    "id": "Crankshaft",
    "title": "Crankshaft",
    "desc": "..."
  }
}
```

### 2-3. Entity 클래스

#### ModelEntity.java

```java
@Entity
@Table(
    name = "models",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_models_domain_category_slug",
            columnNames = {"domain_key", "category_key", "slug"}
        )
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
    
    @Column(name = "domain_key")
    private String domainKey;
    
    @Column(name = "category_key")
    private String categoryKey;
    
    @Column(name = "slug")
    private String slug;
    
    @OneToMany(
        mappedBy = "model",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<PartEntity> parts = new ArrayList<>();
    
    // Getters, Setters, Helper methods...
}
```

**주요 특징**:
- `@OneToMany`: 한 모델이 여러 부품을 가짐
- `cascade = CascadeType.ALL`: 모델 삭제 시 부품도 삭제
- `orphanRemoval = true`: 연관관계가 끊어진 부품 자동 삭제

#### PartEntity.java

```java
@Entity
@Table(
    name = "model_parts",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_model_mesh",
        columnNames = {"model_id", "mesh_name"}
    )
)
public class PartEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "mesh_name", nullable = false)
    private String meshName;
    
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> content = new LinkedHashMap<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private ModelEntity model;
    
    // Getters, Setters...
}
```

**주요 특징**:
- `@JdbcTypeCode(SqlTypes.JSON)`: Hibernate 6에서 JSONB 매핑
- `Map<String, Object>`: 유연한 구조 (스키마 변경 불필요)
- `@ManyToOne`: 여러 부품이 하나의 모델에 속함

---

## 3. AI 시스템

### 3-1. 아키텍처

```
사용자 질문
    ↓
AiController.ask()
    ↓
AiService.buildContext()     → 컨텍스트 구성
    ├─ GLOBAL 모드: 일반 질문
    └─ PART 모드: 부품 선택 시
    ↓
AiService.composePrompt()     → 프롬프트 생성
    ├─ 템플릿 선택
    └─ 변수 치환
    ↓
AiService.generateAnswer()    → 답변 생성
    ├─ OpenAI 키 있음 → OpenAIClient
    └─ 키 없음 → MockAiClient
    ↓
AiAskResponseDto 반환
```

### 3-2. 컨텍스트 구성

#### buildContext() 로직

```java
public AiContextResult buildContext(Long modelId, String meshName) {
    // GLOBAL 모드: 부품 미선택
    if (modelId == null || meshName == null || meshName.isBlank()) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("partFound", false);
        
        return new AiContextResult(
            "GLOBAL",
            "- 전역 질문 모드 (부품 미선택)",
            meta
        );
    }
    
    // PART 모드: 부품 선택
    Optional<PartEntity> partOpt = 
        partRepository.findByModel_IdAndMeshName(modelId, meshName);
    
    Map<String, Object> meta = new HashMap<>();
    meta.put("partFound", partOpt.isPresent());
    
    if (partOpt.isPresent()) {
        PartEntity part = partOpt.get();
        
        String context = """
            - meshName: %s
            - content: %s
            """.formatted(meshName, String.valueOf(part.getContent()));
        
        return new AiContextResult("PART", context, meta);
    }
    
    return new AiContextResult(
        "PART",
        "- 해당 부품을 찾지 못했다",
        meta
    );
}
```

**모드별 차이**:

| 모드 | 조건 | 컨텍스트 | 사용 케이스 |
|------|------|----------|-------------|
| **GLOBAL** | modelId == null 또는 meshName == null | 전역 정보 | "시뮬레이션이 뭔가요?" |
| **PART** | modelId + meshName 있음 | 부품 상세 정보 | "이 부품의 역할은?" |

### 3-3. 프롬프트 템플릿

#### 템플릿 파일 위치

```
src/main/resources/prompts/
├── global.txt           # GLOBAL 모드용
├── part.txt             # PART 모드용 (기본)
└── part_with_notes.txt  # PART 모드용 (notes 있을 때)
```

#### global.txt

```
너는 3D 모델 기반 학습 도우미다.

[컨텍스트]
{{context}}

[질문]
{{question}}

[요구사항]
- 간결하고 정확하게 한국어로 설명한다
- 전문 용어는 쉽게 풀어서 설명한다
- 예시를 들어 이해를 돕는다
```

#### part.txt

```
너는 3D 모델 기반 학습 도우미다.
현재 사용자가 특정 부품을 선택한 상태다.

[부품 정보]
{{context}}

[질문]
{{question}}

[요구사항]
- 해당 부품에 대해 정확하게 설명한다
- 부품의 기능, 재질, 구조를 중심으로 답변한다
- 다른 부품과의 관계도 설명한다
```

### 3-4. OpenAI 클라이언트

#### OpenAIClient.java

```java
@Component
public class OpenAIClient {
    private final WebClient webClient;
    private final String apiKey;
    
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
    
    public String ask(String prompt) {
        var req = new ResponsesRequest(
            "gpt-4-mini",
            List.of(
                new InputMessage(
                    "user",
                    List.of(new ContentPart("input_text", prompt))
                )
            )
        );
        
        try {
            ResponsesResponse res = webClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(ResponsesResponse.class)
                .timeout(Duration.ofSeconds(60))
                .block();
            
            // 응답에서 텍스트 추출
            return extractOutputText(res.output);
            
        } catch (WebClientResponseException e) {
            System.out.println("OPENAI ERROR STATUS = " + e.getStatusCode());
            System.out.println("OPENAI ERROR BODY = " + e.getResponseBodyAsString());
            throw e;
        }
    }
}
```

**주요 포인트**:
- Responses API 사용 (`/v1/responses`)
- `gpt-4-mini` 모델
- 60초 타임아웃
- 에러 처리 및 로깅

#### MockAiClient.java

```java
@Component
public class MockAiClient implements AiClient {
    @Override
    public String ask(String prompt) {
        return "Mock 답변\n"
            + "- prompt 길이: " + (prompt == null ? 0 : prompt.length()) + "\n"
            + "- 생성시각: " + Instant.now();
    }
}
```

**사용 시나리오**:
- OpenAI API 키 없을 때
- 개발/테스트 환경
- 비용 절감

### 3-5. 에러 처리

#### AiService.generateAnswer()

```java
public AiAnswerResult generateAnswer(String prompt) {
    // Mock 모드
    if (!openAIClient.enabled()) {
        String a = mockAiClient.ask(prompt);
        return new AiAnswerResult(a, "mock", null, null);
    }
    
    // OpenAI 모드
    try {
        String a = openAIClient.ask(prompt);
        if (a == null || a.isBlank()) {
            return new AiAnswerResult(
                "",
                "openai",
                "empty_answer",
                "OpenAI 응답 텍스트가 비어있다"
            );
        }
        return new AiAnswerResult(a, "openai", null, null);
        
    } catch (WebClientResponseException e) {
        return new AiAnswerResult(
            "현재 AI 응답을 불러올 수 없다. (OpenAI 호출 실패)",
            "openai",
            "openai_http_" + e.getStatusCode().value(),
            safeShort(e.getResponseBodyAsString())
        );
    } catch (Exception e) {
        return new AiAnswerResult(
            "현재 AI 응답을 불러올 수 없다. (OpenAI 호출 실패)",
            "openai",
            "openai_error",
            safeShort(e.getMessage())
        );
    }
}
```

**에러 코드**:

| 에러 코드 | 의미 | 예시 |
|-----------|------|------|
| `empty_answer` | 응답 텍스트가 비어있음 | OpenAI가 빈 문자열 반환 |
| `openai_http_401` | 인증 실패 | API 키 잘못됨 |
| `openai_http_429` | Rate limit 초과 | 사용량 제한 |
| `openai_http_500` | OpenAI 서버 에러 | 서비스 장애 |
| `openai_error` | 기타 에러 | 네트워크 오류 등 |

---

## 4. Asset Import 시스템

### 4-1. 동작 원리

```
서버 시작
    ↓
AssetImportRunner.run()
    ↓
AssetImportService.importAllFromResources()
    ↓
1. resources/import/Data_*.json 스캔
    ↓
2. 각 JSON 파일 로드
    ↓
3. 기존 모델 목록 조회
    ↓
4. 파일명/폴더명으로 모델 매칭
    ↓
5. 부품 데이터 Upsert
    ↓
완료
```

### 4-2. JSON 데이터 형식

#### Data_V4_Engine.json

```json
{
  "integrated_file": "V4_Engine.glb",
  "assets": [
    {
      "id": "Crankshaft",
      "title": "Crankshaft",
      "desc": "피스톤의 왕복 운동을 회전 운동으로 변환",
      "position": [0, 0, 0],
      "vector": [1, 0, 0],
      "explodeVector": [0, 1, 0]
    },
    {
      "id": "Piston01",
      "title": "Piston01",
      "desc": "실린더 내에서 왕복 운동",
      "position": [0, 1, 0],
      "vector": [0, 1, 0],
      "explodeVector": [0, 2, 0]
    }
  ]
}
```

**필드 설명**:

| 필드 | 필수 | 설명 | 예시 |
|------|------|------|------|
| `integrated_file` | ✅ | 통합 GLB 파일명 | "V4_Engine.glb" |
| `assets` | ✅ | 부품 배열 | `[{...}, {...}]` |
| `assets[].id` | ✅ | 부품 ID (meshName으로 사용) | "Crankshaft" |
| `assets[].title` | ✅ | 부품 표시명 | "Crankshaft" |
| `assets[].desc` | ⚪ | 부품 설명 | "피스톤의..." |
| `assets[].position` | ⚪ | 위치 좌표 | `[0, 0, 0]` |
| `assets[].vector` | ⚪ | 방향 벡터 | `[1, 0, 0]` |
| `assets[].explodeVector` | ⚪ | 분해 방향 | `[0, 1, 0]` |

### 4-3. 모델 매칭 로직

#### findBestModelMatch()

```java
private ModelEntity findBestModelMatch(
    List<ModelEntity> existing, 
    String rawKey, 
    String integratedFile
) {
    // 정규화 (대소문자/특수문자 제거)
    String n1 = norm(rawKey);              // "v4engine"
    String n2 = norm(integratedFile.replace(".glb", ""));  // "v4engine"
    
    // 1차 매칭: title 기준
    for (ModelEntity m : existing) {
        String mt = norm(m.getTitle());
        if (mt.equals(n1) || mt.equals(n2)) return m;
    }
    
    // 2차 매칭: modelUrl 폴더명 기준
    for (ModelEntity m : existing) {
        String folder = extractFolderName(m.getModelUrl(), m.getTitle());
        if (norm(folder).equals(n1) || norm(folder).equals(n2)) return m;
    }
    
    return null;
}

private String norm(String s) {
    if (s == null) return "";
    return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
}
```

**매칭 예시**:

| JSON 파일명 | DB title | modelUrl | 매칭 여부 |
|-------------|----------|----------|-----------|
| `Data_V4_Engine.json` | "V4_Engine" | "/assets/3d/V4_Engine/" | ✅ 1차 매칭 |
| `Data_V4Engine.json` | "V4 Engine" | "/assets/3d/V4_Engine/" | ✅ 정규화 후 매칭 |
| `Data_Drone.json` | "드론" | "/assets/3d/Drone/" | ✅ 2차 매칭 (폴더명) |

### 4-4. Upsert 로직

```java
for (Map<String, Object> a : assets) {
    String meshName = firstNonBlank(
        asString(a.get("title")),
        asString(a.get("id"))
    );
    
    if (meshName == null || meshName.isBlank()) continue;
    
    // content 구성
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("name", meshName);
    content.put("type", "part");
    content.put("fileUrl", fileUrl);
    content.put("integratedFile", integratedFile);
    content.put("description", asString(a.get("desc")));
    content.put("position", a.get("position"));
    content.put("vector", a.get("vector"));
    content.put("explodeVector", a.get("explodeVector"));
    content.put("raw", a);  // 원본 데이터 보존
    
    // Upsert
    PartEntity part = findPart(model.getId(), meshName)
        .orElseGet(() -> new PartEntity(model, meshName, new LinkedHashMap<>()));
    
    part.setMeshName(meshName);
    part.setContent(content);
    part.setModel(model);
    
    partRepository.save(part);  // INSERT or UPDATE
}
```

**Unique 제약으로 중복 방지**:
```sql
CONSTRAINT uk_model_mesh UNIQUE (model_id, mesh_name)
```

---

## 5. API 상세 명세

### 5-1. Study API (신규 - 권장)

#### GET /api/study/catalog

**Description**: 특정 도메인의 전체 카탈로그 조회

**Parameters**:
- `domain` (required): 도메인 키 (예: `engineering-dict`)

**Request**:
```http
GET /api/study/catalog?domain=engineering-dict HTTP/1.1
Host: localhost:8080
```

**Response**: 200 OK
```json
{
  "domainKey": "engineering-dict",
  "categories": [
    {
      "categoryKey": "mechanics",
      "title": "mechanics",
      "models": [
        {
          "id": 1,
          "title": "V4_Engine",
          "slug": "v4-engine",
          "modelUrl": "/assets/3d/V4_Engine/",
          "domainKey": "engineering-dict",
          "categoryKey": "mechanics"
        }
      ]
    }
  ]
}
```

#### GET /api/study/{domain}/{category}/models

**Description**: 특정 카테고리의 모델 목록 조회

**Path Parameters**:
- `domain`: 도메인 키
- `category`: 카테고리 키

**Request**:
```http
GET /api/study/engineering-dict/mechanics/models HTTP/1.1
```

**Response**: 200 OK
```json
[
  {
    "id": 1,
    "title": "V4_Engine",
    "slug": "v4-engine",
    "modelUrl": "/assets/3d/V4_Engine/",
    "domainKey": "engineering-dict",
    "categoryKey": "mechanics"
  }
]
```

#### GET /api/study/{domain}/{category}/{slug}/bundle

**Description**: 모델 상세 정보 + 부품 번들 조회

**Path Parameters**:
- `domain`: 도메인 키
- `category`: 카테고리 키
- `slug`: 모델 슬러그

**Request**:
```http
GET /api/study/engineering-dict/mechanics/v4-engine/bundle HTTP/1.1
```

**Response**: 200 OK
```json
{
  "model": {
    "id": 1,
    "title": "V4_Engine",
    "slug": "v4-engine",
    "modelUrl": "/assets/3d/V4_Engine/",
    "domainKey": "engineering-dict",
    "categoryKey": "mechanics"
  },
  "parts": [
    {
      "id": 1,
      "meshName": "Crankshaft",
      "content": {
        "name": "크랭크축",
        "type": "part",
        "fileUrl": "/assets/3d/V4_Engine/V4_Engine.glb",
        "description": "피스톤의 왕복 운동을 회전 운동으로 변환"
      }
    }
  ]
}
```

### 5-2. Model API (레거시)

#### GET /api/models

**Description**: 전체 모델 목록 조회

**Response**: 200 OK
```json
[
  {
    "id": 1,
    "title": "V4_Engine",
    "modelUrl": "/assets/3d/V4_Engine/"
  }
]
```

#### GET /api/models/{id}/parts

**Description**: 특정 모델의 부품 목록 조회

**Path Parameters**:
- `id`: 모델 ID

**Response**: 200 OK
```json
[
  {
    "id": 1,
    "meshName": "Crankshaft",
    "content": {
      "name": "크랭크축",
      "description": "..."
    }
  }
]
```

### 5-3. AI API

#### POST /api/ai/ask

**Description**: AI에게 질문하기

**Request Body**:
```json
{
  "modelId": 1,
  "meshName": "Crankshaft",
  "question": "이 부품의 역할은 무엇인가요?",
  "notes": "{\"ui\":{\"activeTab\":\"parts\"}}"
}
```

**Response**: 200 OK (성공)
```json
{
  "answer": "크랭크축은 피스톤의 왕복 운동을 회전 운동으로 변환하는...",
  "context": "- meshName: Crankshaft\n- content: {...}",
  "mode": "PART",
  "meta": {
    "partFound": true,
    "provider": "openai"
  }
}
```

**Response**: 200 OK (OpenAI 에러)
```json
{
  "answer": "현재 AI 응답을 불러올 수 없다. (OpenAI 호출 실패)",
  "context": "...",
  "mode": "PART",
  "meta": {
    "partFound": true,
    "provider": "openai",
    "aiErrorCode": "openai_http_429",
    "aiErrorMessage": "Rate limit exceeded"
  }
}
```

---

## 6. 개발 워크플로우

### 6-1. 브랜치 전략

```
main (production)
  ↑
  └─ develop (staging)
       ↑
       ├─ feature/new-api
       ├─ feature/ai-improvement
       ├─ fix/부품-매칭-버그
       └─ hotfix/critical-bug
```

### 6-2. 새 기능 개발

```bash
# 1. develop에서 브랜치 생성
git checkout develop
git pull origin develop
git checkout -b feature/new-api

# 2. 개발 진행
# (코드 작성...)

# 3. 커밋
git add .
git commit -m "feat: Add new API endpoint for..."

# 4. 푸시
git push origin feature/new-api

# 5. Pull Request 생성
# GitHub에서 develop으로 PR 생성

# 6. 코드 리뷰 후 머지
# Squash and merge 또는 Merge commit

# 7. 로컬 브랜치 정리
git checkout develop
git pull origin develop
git branch -d feature/new-api
```

### 6-3. 버그 수정

```bash
# Hotfix (긴급)
git checkout main
git checkout -b hotfix/critical-bug
# (수정...)
git push origin hotfix/critical-bug
# PR → main + develop 둘 다 머지

# 일반 버그
git checkout develop
git checkout -b fix/부품-매칭-버그
# (수정...)
# PR → develop
```

---

## 7. 코드 컨벤션

### 7-1. 네이밍 규칙

| 항목 | 규칙 | 예시 |
|------|------|------|
| **클래스** | PascalCase | `ModelEntity`, `AiService` |
| **메서드** | camelCase | `findById()`, `buildContext()` |
| **변수** | camelCase | `modelId`, `meshName` |
| **상수** | UPPER_SNAKE_CASE | `TTL_SECONDS`, `MAX_RETRIES` |
| **패키지** | lowercase | `com.simvex.simvex_api.ai` |

### 7-2. 주석 규칙

```java
/**
 * 모델의 부품 목록을 조회한다
 * 
 * @param id 모델 ID
 * @return 부품 DTO 리스트
 */
@GetMapping("/{id}/parts")
public ResponseEntity<List<PartDto>> listParts(@PathVariable Long id) {
    // ...
}
```

### 7-3. 예외 처리

```java
// BAD ❌
try {
    // ...
} catch (Exception e) {
    e.printStackTrace();
}

// GOOD ✅
try {
    // ...
} catch (Exception e) {
    log.error("Failed to import assets", e);
    throw new AssetImportException("Asset import failed", e);
}
```

---

## 8. 테스트

### 8-1. 테스트 구조

```
src/test/java/com/simvex/simvex_api/
├── controller/
│   ├── ModelControllerTest.java
│   ├── AiControllerTest.java
│   └── StudyControllerTest.java
├── service/
│   ├── AiServiceTest.java
│   └── AssetImportServiceTest.java
└── repository/
    ├── ModelRepositoryTest.java
    └── PartRepositoryTest.java
```

### 8-2. 테스트 예시

#### ModelControllerTest.java

```java
@SpringBootTest
@AutoConfigureMockMvc
class ModelControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ModelRepository modelRepository;
    
    @BeforeEach
    void setUp() {
        modelRepository.deleteAll();
        
        ModelEntity model = new ModelEntity("TestModel", "/assets/3d/Test/");
        model.setDomainKey("test-domain");
        model.setCategoryKey("test-category");
        model.setSlug("test-model");
        modelRepository.save(model);
    }
    
    @Test
    void testListModels() throws Exception {
        mockMvc.perform(get("/api/models"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].title").value("TestModel"));
    }
    
    @Test
    void testListParts() throws Exception {
        mockMvc.perform(get("/api/models/1/parts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
```

#### AiServiceTest.java

```java
@SpringBootTest
class AiServiceTest {
    
    @Autowired
    private AiService aiService;
    
    @MockBean
    private PartRepository partRepository;
    
    @Test
    void testBuildContext_GLOBAL() {
        AiContextResult result = aiService.buildContext(null, null);
        
        assertEquals("GLOBAL", result.mode);
        assertFalse((Boolean) result.meta.get("partFound"));
    }
    
    @Test
    void testBuildContext_PART() {
        PartEntity part = new PartEntity();
        part.setMeshName("TestPart");
        
        when(partRepository.findByModel_IdAndMeshName(1L, "TestPart"))
            .thenReturn(Optional.of(part));
        
        AiContextResult result = aiService.buildContext(1L, "TestPart");
        
        assertEquals("PART", result.mode);
        assertTrue((Boolean) result.meta.get("partFound"));
    }
}
```

### 8-3. 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 클래스
./gradlew test --tests ModelControllerTest

# 특정 메서드
./gradlew test --tests ModelControllerTest.testListModels

# 커버리지 리포트
./gradlew jacocoTestReport
# build/reports/jacoco/test/html/index.html
```

---

## 9. 성능 최적화

### 9-1. 데이터베이스 최적화

#### 인덱스 추가

```sql
-- model_parts 테이블
CREATE INDEX idx_model_parts_model_id ON model_parts(model_id);
CREATE INDEX idx_model_parts_content ON model_parts USING GIN(content);

-- models 테이블
CREATE INDEX idx_models_domain_category ON models(domain_key, category_key);
```

#### N+1 문제 해결

```java
// BAD ❌ - N+1 쿼리
@GetMapping("/{id}/parts")
public ResponseEntity<List<PartDto>> listParts(@PathVariable Long id) {
    ModelEntity model = modelRepository.findById(id).orElse(null);
    List<PartEntity> parts = model.getParts();  // Lazy loading → N+1
    // ...
}

// GOOD ✅ - Fetch Join
@Query("SELECT m FROM ModelEntity m LEFT JOIN FETCH m.parts WHERE m.id = :id")
Optional<ModelEntity> findByIdWithParts(@Param("id") Long id);
```

### 9-2. 캐싱

#### AiAnswerCache

```java
@Component
public class AiAnswerCache {
    private static final long TTL_SECONDS = 60 * 10; // 10분
    
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;
        
        if (Instant.now().getEpochSecond() > entry.expiresAt) {
            cache.remove(key);
            return null;
        }
        return entry.answer;
    }
    
    public void put(String key, String answer) {
        cache.put(key, new CacheEntry(
            answer,
            Instant.now().getEpochSecond() + TTL_SECONDS
        ));
    }
}
```

### 9-3. 응답 압축

```yml
# application.yml
server:
  compression:
    enabled: true
    min-response-size: 1024
    mime-types:
      - application/json
      - application/xml
      - text/html
      - text/xml
      - text/plain
```

---

## 10. 배포

### 10-1. 프로덕션 빌드

```bash
# 빌드
./gradlew clean build -Pprofile=prod

# JAR 파일 생성 확인
ls -lh build/libs/
# simvex-api-0.0.1-SNAPSHOT.jar
```

### 10-2. Docker 이미지

#### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src
RUN ./gradlew clean build -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 빌드 및 실행

```bash
# 이미지 빌드
docker build -t simvex-api:latest .

# 컨테이너 실행
docker run -d -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/simvex \
  -e SPRING_DATASOURCE_USERNAME=simvex \
  -e SPRING_DATASOURCE_PASSWORD=simvexpw \
  -e OPENAI_API_KEY=sk-your-key \
  --name simvex-api \
  simvex-api:latest
```

### 10-3. Docker Compose

#### docker-compose.yml

```yml
version: '3.8'

services:
  db:
    image: postgres:15
    environment:
      POSTGRES_DB: simvex
      POSTGRES_USER: simvex
      POSTGRES_PASSWORD: simvexpw
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  api:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/simvex
      SPRING_DATASOURCE_USERNAME: simvex
      SPRING_DATASOURCE_PASSWORD: simvexpw
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    depends_on:
      - db

volumes:
  postgres_data:
```

```bash
# 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f api

# 종료
docker-compose down
```

---

## 📚 추가 리소스

- **API 명세**: [docs/API.md](docs/API.md)
- **문제 해결**: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)
- **아키텍처**: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

**Happy Coding! 🚀**
