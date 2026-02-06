# SIMVEX API - 설치 및 실행 가이드

> 📖 **문서 네비게이션**
> - 👈 **프로젝트 소개**: [README.md](README.md)로 돌아가기
> - 💻 **개발 가이드**: [DEVELOPMENT.md](DEVELOPMENT.md)에서 상세 정보 확인
> - 🐛 **문제 발생 시**: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) 참고

이 문서는 **아무 세팅 없는 환경**에서 SIMVEX API를 처음 실행하는 방법을 안내합니다.

---

## 📋 목차

1. [프로젝트 다운로드](#1-프로젝트-다운로드)
2. [필수 프로그램 설치](#2-필수-프로그램-설치)
3. [PostgreSQL 실행 (Docker)](#3-postgresql-실행-docker)
4. [application.yml 설정](#4-applicationyml-설정)
5. [에셋 폴더 설정](#5-에셋-폴더-설정)
6. [OpenAI API Key 설정](#6-openai-api-key-설정-선택)
7. [서버 실행](#7-서버-실행)
8. [API 동작 확인](#8-api-동작-확인)
9. [주요 API 엔드포인트](#9-주요-api-엔드포인트)
10. [프로젝트 구조](#10-프로젝트-구조)
11. [프론트엔드 연동 기준](#11-프론트엔드-연동-기준)
12. [자주 발생하는 문제](#12-자주-발생하는-문제)

---

## 1. 프로젝트 다운로드

```bash
git clone https://github.com/dosacha/simvex-api
cd simvex-api
```

---

## 2. 필수 프로그램 설치

### 2-1. Java 17 설치

아래 중 하나만 설치하면 됩니다.

#### Option 1: Temurin (OpenJDK) 17 ⭐ 추천

**Windows**:
```powershell
# Chocolatey 사용
choco install temurin17
```

**macOS**:
```bash
# Homebrew 사용
brew install --cask temurin@17
```

**Linux (Ubuntu)**:
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

#### Option 2: Oracle JDK 17

[Oracle 공식 사이트](https://www.oracle.com/java/technologies/downloads/#java17)에서 다운로드

#### 설치 확인

```bash
java -version
```

**정상 출력 예시**:
```
openjdk version "17.0.x" 2023-xx-xx
OpenJDK Runtime Environment Temurin-17.x (build 17.0.x+x)
```

---

### 2-2. Docker Desktop 설치

PostgreSQL은 Docker 컨테이너로만 실행합니다.  
**로컬 DB 설치는 필요 없습니다.**

#### 다운로드

- **Windows/macOS**: [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- **Linux**: [Docker Engine](https://docs.docker.com/engine/install/)

#### 설치 확인

```bash
docker --version
```

**정상 출력 예시**:
```
Docker version 24.0.x, build xxxxxxx
```

---

## 3. PostgreSQL 실행 (Docker)

### 3-1. 컨테이너 실행

```bash
docker run -d --name simvex-pg \
  -e POSTGRES_DB=simvex \
  -e POSTGRES_USER=simvex \
  -e POSTGRES_PASSWORD=simvexpw \
  -p 5432:5432 \
  postgres:15
```

**옵션 설명**:
- `-d`: 백그라운드 실행
- `--name simvex-pg`: 컨테이너 이름
- `-e`: 환경 변수 (DB 이름, 사용자, 비밀번호)
- `-p 5432:5432`: 포트 매핑 (호스트:컨테이너)
- `postgres:15`: 이미지 이름 및 버전

### 3-2. 실행 확인

```bash
docker ps
```

**정상 출력 예시**:
```
CONTAINER ID   IMAGE         STATUS          PORTS                    NAMES
abc123def456   postgres:15   Up 10 seconds   0.0.0.0:5432->5432/tcp   simvex-pg
```

### 3-3. 문제 발생 시 초기화

```bash
# 컨테이너 중지 및 삭제
docker rm -f simvex-pg

# 다시 실행
docker run -d --name simvex-pg ...
```

---

## 4. application.yml 설정

### 4-1. 파일 위치

```
src/main/resources/application.yml
```

### 4-2. 기본 설정

```yml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/simvex
    username: simvex
    password: simvexpw
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

logging:
  level:
    org.hibernate.SQL: debug

openai:
  api-key: ${OPENAI_API_KEY:}

simvex:
  assets:
    import:
      enabled: true
      root: ${user.dir}/src/main/resources/static/assets/3d
```

### 4-3. 설정 설명

| 설정 | 설명 | 기본값 |
|------|------|--------|
| `server.port` | 서버 포트 | 8080 |
| `spring.datasource.url` | DB 연결 URL | localhost:5432 |
| `spring.jpa.hibernate.ddl-auto` | 스키마 자동 관리 | update |
| `openai.api-key` | OpenAI API 키 (선택) | 환경 변수 |
| `simvex.assets.import.enabled` | 에셋 자동 임포트 | true |
| `simvex.assets.import.root` | 3D 파일 루트 폴더 | (프로젝트 경로) |

---

## 5. 에셋 폴더 설정

### 5-1. simvex.assets.import.root 설명 ⭐ 중요

`simvex.assets.import.root`는 서버 시작 시 3D 에셋을 자동으로 DB에 등록하는 **기준 폴더**입니다.

### 5-2. 실제 기준 경로

```
src/main/resources/static/assets/3d
```

### 5-3. 폴더 구조 예시

```
assets/3d/
├─ Drone/
│   ├─ Drone.glb
│   ├─ Arm gear.glb
│   ├─ Main frame.glb
│   ├─ 조립도1.png
│   └─ ...
├─ Robot_Arm/
│   ├─ Robot_Arm.glb
│   └─ ...
├─ Leaf_Spring/
│   ├─ Leaf_Spring.glb
│   └─ ...
└─ V4_Engine/
    ├─ V4_Engine.glb
    ├─ Crankshaft.glb
    └─ ...
```

### 5-4. 동작 방식

```
폴더 이름 (예: Drone)
    ↓
ModelEntity 생성 (title = "Drone")
    ↓
폴더 내 파일들 (*.glb, *.png, *.jpg)
    ↓
각 파일 → PartEntity 생성
    ↓
서버 실행 시 AssetImportRunner가 자동으로 DB Upsert
```

### 5-5. 명명 규칙 ⭐ 중요

| 항목 | 규칙 | 예시 |
|------|------|------|
| **폴더명** | 모델 타이틀 (공백 → 언더스코어) | `V4_Engine`, `Robot_Arm` |
| **GLB 파일명** | 자유 (meshName으로 사용) | `Crankshaft.glb`, `Piston01.glb` |
| **이미지 파일** | 자유 (참조용) | `assembly_guide.png` |

### 5-6. 주의사항

⚠️ **폴더 위치를 변경하면** `simvex.assets.import.root`도 **반드시 수정**해야 합니다!

```yml
# 예: 외부 폴더 사용 시
simvex:
  assets:
    import:
      root: /path/to/external/3d-models
```

---

## 6. OpenAI API Key 설정 (선택)

AI 기능을 **실제 OpenAI**로 사용하려면 필요합니다.  
**키가 없어도 Mock AI로 서버는 정상 실행됩니다.**

### 6-1. API 키 발급

1. [OpenAI Platform](https://platform.openai.com/)에 로그인
2. API Keys 메뉴에서 새 키 생성
3. 키 복사 (sk-로 시작)

### 6-2. 환경 변수 설정

#### Windows (PowerShell)

```powershell
setx OPENAI_API_KEY "sk-your-openai-api-key"
```

> ⚠️ **새 PowerShell 창을 열어야 적용됩니다.**

#### macOS / Linux (Bash)

```bash
# ~/.bashrc 또는 ~/.zshrc에 추가
export OPENAI_API_KEY="sk-your-openai-api-key"

# 적용
source ~/.bashrc
```

### 6-3. 확인

```bash
# Windows (PowerShell)
echo $env:OPENAI_API_KEY

# macOS / Linux
echo $OPENAI_API_KEY
```

### 6-4. Mock 모드

API 키가 없으면 자동으로 **Mock 모드**로 동작합니다:

```java
Mock 답변
- prompt 길이: 512
- 생성시각: 2026-02-05T10:30:00Z
```

---

## 7. 서버 실행

### 7-1. Gradle로 실행

#### Windows

```bash
.\gradlew clean bootRun
```

#### macOS / Linux

```bash
./gradlew clean bootRun
```

### 7-2. JAR로 실행 (선택)

```bash
# 빌드
./gradlew clean build

# 실행
java -jar build/libs/simvex-api-0.0.1-SNAPSHOT.jar
```

### 7-3. 정상 로그 예시

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

2026-02-05 10:30:00 INFO  --- Starting SimvexApiApplication
2026-02-05 10:30:01 INFO  --- Tomcat started on port 8080
2026-02-05 10:30:01 INFO  --- [AssetImportRunner] 시작
2026-02-05 10:30:01 INFO  --- [AssetImportRunner] scanning: .../assets/3d
2026-02-05 10:30:02 INFO  --- [AssetImportRunner] model upsert: Drone
2026-02-05 10:30:02 INFO  --- [AssetImportRunner] part upsert: Drone / Arm gear
2026-02-05 10:30:02 INFO  --- [AssetImportRunner] part upsert: Drone / Main frame
2026-02-05 10:30:03 INFO  --- [AssetImportRunner] 종료
```

### 7-4. 서버 실행 확인

브라우저에서 http://localhost:8080 접속

**또는**

```bash
curl http://localhost:8080/actuator/health
```

---

## 8. API 동작 확인

### 8-1. 전체 모델 조회

```bash
curl http://localhost:8080/api/models
```

**정상 응답 예시**:
```json
[
  {
    "id": 1,
    "title": "Drone",
    "modelUrl": "/assets/3d/Drone/"
  },
  {
    "id": 2,
    "title": "V4_Engine",
    "modelUrl": "/assets/3d/V4_Engine/"
  }
]
```

### 8-2. 특정 모델의 부품 조회

```bash
curl http://localhost:8080/api/models/1/parts
```

**정상 응답 예시**:
```json
[
  {
    "id": 1,
    "meshName": "Arm gear",
    "content": {
      "name": "Arm gear",
      "type": "part",
      "fileUrl": "/assets/3d/Drone/Drone.glb",
      "description": "암 기어 부품"
    }
  }
]
```

### 8-3. AI 질문 테스트

```bash
curl -X POST http://localhost:8080/api/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "modelId": 1,
    "meshName": "Arm gear",
    "question": "이 부품의 역할은?",
    "notes": ""
  }'
```

**정상 응답 예시** (Mock 모드):
```json
{
  "answer": "Mock 답변\n- prompt 길이: 256\n- 생성시각: 2026-02-05T10:30:00Z",
  "context": "- meshName: Arm gear\n- content: {...}",
  "mode": "PART",
  "meta": {
    "partFound": true,
    "provider": "mock"
  }
}
```

---

## 9. 주요 API 엔드포인트

### 9-1. Study API (신규 - 권장) 🆕

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/study/catalog?domain={key}` | 도메인 카탈로그 조회 |
| GET | `/api/study/{domain}/{category}/models` | 카테고리별 모델 목록 |
| GET | `/api/study/{domain}/{category}/{slug}/bundle` | 모델 상세 + 부품 번들 |

**예시**:
```bash
# 카탈로그 조회
curl "http://localhost:8080/api/study/catalog?domain=engineering-dict"

# 카테고리별 모델
curl "http://localhost:8080/api/study/engineering-dict/mechanics/models"

# 모델 상세
curl "http://localhost:8080/api/study/engineering-dict/mechanics/v4-engine/bundle"
```

### 9-2. Model API (레거시)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/models` | 전체 모델 목록 |
| GET | `/api/models/{id}/parts` | 모델별 부품 목록 |

### 9-3. AI API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/ai/ask` | AI 질문 |

**상세 API 명세**: [docs/API.md](docs/API.md) 참고

---

## 10. 프로젝트 구조

```
simvex-api/
├── src/main/java/com/simvex/simvex_api/
│   ├── ai/                      # AI 관련
│   │   ├── AiService.java       # 핵심 서비스
│   │   ├── OpenAIClient.java    # OpenAI 구현
│   │   ├── MockAiClient.java    # Mock 구현
│   │   └── ...
│   │
│   ├── bootstrap/               # 초기화
│   │   └── AssetImportRunner.java  # 에셋 자동 임포트
│   │
│   ├── controller/              # REST API
│   │   ├── StudyController.java
│   │   ├── ModelController.java
│   │   └── AiController.java
│   │
│   ├── model/                   # 모델 엔티티
│   │   ├── ModelEntity.java
│   │   ├── ModelRepository.java
│   │   └── AssetImportService.java
│   │
│   ├── part/                    # 부품 엔티티
│   │   ├── PartEntity.java
│   │   └── PartRepository.java
│   │
│   └── dto/                     # 데이터 전송 객체
│       ├── StudyCatalogDto.java
│       ├── StudyBundleDto.java
│       ├── PartDto.java
│       └── ...
│
└── src/main/resources/
    ├── application.yml          # 설정 파일
    ├── static/assets/3d/        # ⭐ 3D 모델 파일 (GLB)
    │   ├── Drone/
    │   ├── Robot_Arm/
    │   ├── Leaf_Spring/
    │   └── V4_Engine/
    └── prompts/                 # AI 프롬프트 템플릿
        ├── global.txt
        ├── part.txt
        └── part_with_notes.txt
```

---

## 11. 프론트엔드 연동 기준

프론트엔드(simvex-ui)는 **UX 흐름 기준**으로 연동됩니다.

### 11-1. 사용자 플로우

```
/Home (홈페이지)
    ↓
지금 시작하기 버튼 클릭
    ↓
/Study (학습 선택 페이지)
    ↓
분야별 탐색 (도메인/카테고리 선택)
    ↓
학습 뷰 (Learnpage)
    ├─ 3D Viewer (ThreeViewer.jsx)
    ├─ Note (메모장)
    └─ AI Assistant (챗봇)
```

### 11-2. 백엔드 역할

백엔드는 다음을 제공하는 역할만 담당합니다:

- ✅ **모델 선택 상태** (현재 보고 있는 모델)
- ✅ **선택된 모델의 Part 정보** (부품 목록 및 상세)
- ✅ **AI 컨텍스트용 메타데이터** (질문 답변에 필요한 정보)

### 11-3. 데이터 흐름

```
Frontend                Backend
   │                       │
   ├─ 모델 선택 ──────────→ GET /api/study/.../bundle
   │                       │
   │                  ┌────┴────┐
   │                  │ ModelEntity
   │                  │ + PartEntity[]
   │                  └────┬────┘
   │                       │
   ←─ 모델+부품 정보 ─────┤
   │                       │
   ├─ 부품 클릭 ──────────→ (프론트에서 처리)
   │                       │
   ├─ AI 질문 ───────────→ POST /api/ai/ask
   │                       │
   │                  ┌────┴────┐
   │                  │ AiService
   │                  │ → OpenAI
   │                  └────┬────┘
   │                       │
   ←─ AI 답변 ────────────┤
```

---

## 12. 자주 발생하는 문제

### 12-1. 데이터가 안 들어오는 경우

**증상**:
```bash
curl http://localhost:8080/api/models
# 응답: []
```

**원인**:
- `assets/3d` 폴더가 비어 있음
- `simvex.assets.import.enabled=false`

**해결**:
1. `src/main/resources/static/assets/3d/` 폴더에 3D 파일 배치
2. `application.yml`에서 `simvex.assets.import.enabled=true` 확인
3. 서버 재시작

---

### 12-2. DB 연결 에러

**증상**:
```
HikariPool-1 - Exception during pool initialization.
org.postgresql.util.PSQLException: Connection refused
```

**원인**:
- Docker 컨테이너 미실행
- 포트 5432 충돌

**해결**:
```bash
# Docker 상태 확인
docker ps

# 컨테이너가 없으면 다시 실행
docker run -d --name simvex-pg ...

# 포트 충돌 시 다른 포트 사용
docker run -d --name simvex-pg \
  ... \
  -p 5433:5432 \
  postgres:15

# application.yml도 수정
spring.datasource.url: jdbc:postgresql://localhost:5433/simvex
```

---

### 12-3. 8080 포트 충돌

**증상**:
```
Web server failed to start. Port 8080 was already in use.
```

**해결**:

**Option 1: 다른 서버 종료**
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <프로세스ID> /F

# macOS / Linux
lsof -ti:8080 | xargs kill -9
```

**Option 2: 포트 변경**
```yml
# application.yml
server:
  port: 8081
```

---

### 12-4. OpenAI API 에러

**증상**:
```json
{
  "answer": "현재 AI 응답을 불러올 수 없다. (OpenAI 호출 실패)",
  "meta": {
    "aiErrorCode": "openai_http_429",
    "aiErrorMessage": "Rate limit exceeded"
  }
}
```

**원인**:
- API 키 누락 또는 잘못됨
- OpenAI 사용량 초과

**해결**:
1. API 키 확인: `echo $env:OPENAI_API_KEY`
2. [OpenAI Usage](https://platform.openai.com/usage) 페이지에서 사용량 확인
3. 임시로 Mock 모드 사용 (API 키 제거)

---

### 12-5. Gradle 빌드 에러

**증상**:
```
Could not resolve all dependencies
```

**해결**:
```bash
# Gradle 캐시 삭제
./gradlew clean --refresh-dependencies

# 또는 Gradle Wrapper 재생성
./gradlew wrapper --gradle-version 8.5
```

---

## 📚 다음 단계

설치가 완료되었다면 다음 문서들을 참고하세요:

- 💻 **[DEVELOPMENT.md](DEVELOPMENT.md)**: 개발 가이드 (상세)
- 📡 **[docs/API.md](docs/API.md)**: API 명세서
- 🐛 **[docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)**: 문제 해결 가이드
- 🏗 **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**: 시스템 아키텍처

---

## 📞 추가 지원

문제가 계속 발생하면:
- [GitHub Issues](https://github.com/dosacha/simvex-api/issues) 등록
- 또는 support@simvex.com으로 문의

---

**행운을 빕니다! 🚀**
