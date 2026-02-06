# SIMVEX API

**3D 모델 기반 학습 플랫폼 백엔드**

Spring Boot + PostgreSQL + OpenAI

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-red.svg)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-required-blue.svg)](https://www.docker.com/)

</div>

---

## 🚀 빠른 시작

**10분 안에 실행하기:**

```bash
# 1. 클론
git clone https://github.com/dosacha/simvex-api
cd simvex-api

# 2. Docker PostgreSQL 실행
docker run -d --name simvex-pg   -e POSTGRES_DB=simvex   -e POSTGRES_USER=simvex   -e POSTGRES_PASSWORD=simvexpw   -p 5432:5432 postgres:15

# 3. 서버 실행
.\gradlew clean bootRun

# 4. 확인
curl http://localhost:8080/api/models
```

**✅ 성공!** 이제 http://localhost:8080 에서 실행 중입니다.

**더 상세한 설치 가이드**: 👉 **[SETUP.md](SETUP.md)** 참고

---

## 📖 문서

| 문서 | 내용 | 대상 |
|------|------|------|
| 👉 **[SETUP.md](SETUP.md)** | **🔧 설치 및 실행 가이드** | **신규 개발자 (필수)** |
| 👉 **[DEVELOPMENT.md](DEVELOPMENT.md)** | **💻 개발 가이드 (상세)** | **기존 개발자** |
| [docs/API.md](docs/API.md) | 📡 API 명세서 | API 사용자 |
| [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | 🐛 문제 해결 | 모든 개발자 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 🏗 시스템 아키텍처 | 아키텍트 |

### 📌 문서 읽는 순서

```
처음 오신 분
  ↓
README.md (지금 여기) → SETUP.md → 실행 성공!
  ↓
코드 작성 시작
  ↓
궁금한 점 생기면 → DEVELOPMENT.md 참고
  ↓
문제 발생 시 → TROUBLESHOOTING.md
```

---

## ✨ 주요 기능

### 🎨 3D 모델 관리
- GLB 파일 자동 임포트 및 DB 동기화
- 부품별 메타데이터 관리 (JSONB)
- 파일 시스템 기반 모델 검색

### 🤖 AI 어시스턴트
- OpenAI API 연동 (gpt-4-mini)
- 컨텍스트 인식 질의응답
- Mock 모드 지원 (API 키 없이 테스트 가능)

### 📦 유연한 데이터 구조
- PostgreSQL JSONB로 확장 가능한 스키마
- 도메인/카테고리 기반 모델 분류
- Unique 제약으로 데이터 무결성 보장

### 🔄 자동화 시스템
- 서버 시작 시 자동 에셋 임포트
- 파일명/폴더명 기반 모델 매칭
- 중복 방지 Upsert 로직

---

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **Build Tool**: Gradle

### Database
- **Main DB**: PostgreSQL 15 (Docker)
- **Test DB**: H2 (In-memory)
- **ORM**: Spring Data JPA, Hibernate 6

### AI Integration
- **Provider**: OpenAI
- **Model**: gpt-4-mini
- **API**: Responses API v1

### Infrastructure
- **Containerization**: Docker
- **Web Client**: Spring WebFlux WebClient

---

## 📡 주요 API

### Study API (권장)

```bash
# 카탈로그 조회
GET /api/study/catalog?domain=engineering-dict

# 카테고리별 모델 목록
GET /api/study/{domain}/{category}/models

# 모델 상세 + 부품 번들
GET /api/study/{domain}/{category}/{slug}/bundle
```

### Model API (레거시)

```bash
# 모델 목록
GET /api/models

# 모델 부품
GET /api/models/{id}/parts
```

### AI API

```bash
# AI 질문
POST /api/ai/ask
Content-Type: application/json

{
  "modelId": 1,
  "meshName": "Crankshaft",
  "question": "이 부품의 역할은?",
  "notes": "{...}"
}
```

**전체 API 명세**: [docs/API.md](docs/API.md)

---

## 🏗 시스템 아키텍처

```
┌─────────────────────────────────────────┐
│         Frontend (React + Three.js)     │
│              Port 3000                   │
└─────────────────┬───────────────────────┘
                  │ HTTP/REST
                  ↓
┌─────────────────────────────────────────┐
│         Backend (Spring Boot)            │
│              Port 8080                   │
│  ┌─────────────────────────────────┐   │
│  │  Controllers (REST API)          │   │
│  │  - StudyController               │   │
│  │  - ModelController               │   │
│  │  - AiController                  │   │
│  └──────────┬──────────────────────┘   │
│             ↓                            │
│  ┌─────────────────────────────────┐   │
│  │  Services (Business Logic)      │   │
│  │  - AiService                     │   │
│  │  - AssetImportService            │   │
│  └──────────┬──────────────────────┘   │
│             ↓                            │
│  ┌─────────────────────────────────┐   │
│  │  Repositories (Data Access)     │   │
│  │  - ModelRepository               │   │
│  │  - PartRepository                │   │
│  └──────────┬──────────────────────┘   │
└─────────────┼───────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│      PostgreSQL 15 (Docker)              │
│      - models (모델 정보)                │
│      - model_parts (부품 정보, JSONB)    │
└─────────────────────────────────────────┘

              +
              
┌─────────────────────────────────────────┐
│         OpenAI API                       │
│         gpt-4-mini                       │
└─────────────────────────────────────────┘
```

**상세 아키텍처**: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## 🗂 프로젝트 구조

```
simvex-api/
├── src/main/java/com/simvex/simvex_api/
│   ├── ai/                      # AI 관련
│   │   ├── AiService.java
│   │   ├── OpenAIClient.java
│   │   ├── MockAiClient.java
│   │   └── ...
│   ├── bootstrap/               # 초기화
│   │   └── AssetImportRunner.java
│   ├── controller/              # REST API
│   │   ├── StudyController.java
│   │   ├── ModelController.java
│   │   └── AiController.java
│   ├── model/                   # 모델 엔티티
│   │   ├── ModelEntity.java
│   │   ├── ModelRepository.java
│   │   └── AssetImportService.java
│   ├── part/                    # 부품 엔티티
│   │   ├── PartEntity.java
│   │   └── PartRepository.java
│   └── dto/                     # 데이터 전송 객체
│       ├── StudyCatalogDto.java
│       ├── StudyBundleDto.java
│       └── ...
│
└── src/main/resources/
    ├── application.yml          # 설정 파일
    ├── static/assets/3d/        # 3D 모델 파일 (GLB)
    │   ├── V4_Engine/
    │   ├── Robot_Arm/
    │   └── Drone/
    └── prompts/                 # AI 프롬프트 템플릿
        ├── global.txt
        ├── part.txt
        └── part_with_notes.txt
```

---

## 🎯 주요 개념

### 1. Asset Import System

서버 시작 시 `src/main/resources/static/assets/3d/` 폴더를 스캔하여:
- 각 폴더 → ModelEntity
- GLB/PNG/JPG 파일 → PartEntity
- 자동으로 DB에 Upsert

### 2. AI Context System

사용자 질문 시 자동으로 컨텍스트 구성:
- **GLOBAL 모드**: 일반 질문 (부품 미선택)
- **PART 모드**: 특정 부품 질문 (부품 선택 시)

### 3. JSONB 활용

PostgreSQL의 JSONB 타입으로 부품 메타데이터 저장:
- 유연한 스키마 (필드 추가/삭제 자유)
- JSON 쿼리 가능
- 인덱싱 지원

---

## 🤝 기여하기

### 개발 워크플로우

1. Fork this repository
2. Create your feature branch
   ```bash
   git checkout -b feature/AmazingFeature
   ```
3. Commit your changes
   ```bash
   git commit -m 'feat: Add AmazingFeature'
   ```
4. Push to the branch
   ```bash
   git push origin feature/AmazingFeature
   ```
5. Open a Pull Request

### 커밋 메시지 규칙

```
feat: 새 기능 추가
fix: 버그 수정
docs: 문서 변경
style: 코드 포맷팅
refactor: 리팩토링
test: 테스트 추가/수정
chore: 빌드 설정 등
```

**개발 가이드**: [DEVELOPMENT.md](DEVELOPMENT.md)

---

## 🧪 테스트

```bash
# 전체 테스트
./gradlew test

# 특정 테스트
./gradlew test --tests ModelControllerTest

# 커버리지 리포트
./gradlew jacocoTestReport
```

---

## 🐛 문제 해결

### 자주 발생하는 문제

**Q: 데이터가 안 들어와요**
- `assets/3d` 폴더가 비어있는지 확인
- `simvex.assets.import.enabled=true` 확인

**Q: DB 연결 에러**
- Docker 컨테이너 실행 확인: `docker ps`
- 포트 5432 충돌 확인

**Q: 8080 포트 충돌**
- 다른 서버 종료 또는 `server.port` 변경

**전체 문제 해결 가이드**: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)

---

## 📞 문의 및 지원

- **Issues**: [GitHub Issues](https://github.com/dosacha/simvex-api/issues)
- **Discussions**: [GitHub Discussions](https://github.com/dosacha/simvex-api/discussions)
- **Email**: support@simvex.com

---

## 📄 라이센스

MIT License - [LICENSE](LICENSE) 파일 참고

---

## 🙏 감사의 말

- **Spring Boot** 팀: 강력한 프레임워크 제공
- **OpenAI**: AI 기술 지원
- **PostgreSQL** 커뮤니티: 훌륭한 데이터베이스
- **모든 기여자분들**: 프로젝트 발전에 기여해주셔서 감사합니다

---

## 📚 관련 프로젝트

- **Frontend**: [simvex-ui](https://github.com/dosacha/simvex-ui) - React + Three.js
- **Mobile**: [simvex-mobile](https://github.com/dosacha/simvex-mobile) - React Native

---

**Made with ❤️ by SIMVEX Team**

**Last Updated**: 2026-02-05
