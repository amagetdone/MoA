# 모아(MORE) 포팅 매뉴얼

- 프로젝트: 모아(MORE) — 핀테크 재무관리 앱
- 작성일: 2026-03-30
- 제출 경로: exec/PORTING_MANUAL.md
- 기준 리포지토리: S14P21D101


## 1. 프로젝트 구성

- **Backend**: `BE/amagetdone` — Spring Boot API, PostgreSQL/Redis 연동
- **Frontend**: `FE` — Flutter Web App, Nginx 정적 파일 서빙
- **AI**: `AI` — FastAPI, 가맹점 분류 / 챗봇 / 이미지 생성

모든 서비스는 동일한 웹서버(j14d101.p.ssafy.io)에서 운영되며 Docker 네트워크(`amagetdone-dev_default`)를 공유합니다.
AI는 `docker run`으로 직접 배포(docker-compose 미사용)하며 Backend와 동일 네트워크에 연결됩니다.


## 2. 빌드/런타임 버전

### 2-1. 개발 IDE
IntelliJ IDEA (Backend) / Android Studio · VS Code (Frontend) / VS Code (AI)

### 2-2. 웹서버 (j14d101.p.ssafy.io)

| 항목 | 버전 |
|------|------|
| OS | Ubuntu (SSAFY 제공) |
| Docker | 29.x 이상 |
| Docker Compose | v5.x 이상 |
| Flutter | stable (Jenkins 자동 설치) |
| Nginx | nginx:stable-alpine (Docker) |

### 2-3. Backend

| 항목 | 버전 |
|------|------|
| Java | 17 |
| Spring Boot | 3.5.11 |
| PostgreSQL | 17 |
| Redis | 7-alpine |
| Docker Build / Runtime | eclipse-temurin:17-jdk-jammy / 17-jre-jammy |

### 2-4. Frontend

| 항목 | 버전 |
|------|------|
| Flutter SDK | ^3.10.8 |
| flutter_riverpod | ^2.6.1 |
| go_router | ^14.8.1 |
| kakao_flutter_sdk_user | ^1.9.6 |
| google_sign_in / firebase_messaging | ^6.2.2 / ^16.0.1 |

### 2-5. AI

| 항목 | 버전 |
|------|------|
| Python | 3.12.7 |
| FastAPI | >=0.115.0 |
| PyTorch (CPU) | >=2.6.0 |
| Transformers | >=4.49.0 |
| LangChain / LangGraph | >=1.0.0 / >=1.0.2 |
| Docker Base | python:3.12.7-slim |


## 3. 배포 포트 및 서비스

### 3-1. Backend Compose

| 서비스 | 포트 | 비고 |
|--------|------|------|
| Backend API | `127.0.0.1:8080:8080` | `COMPOSE_HOST_PORT` 기본값 |
| PostgreSQL | `127.0.0.1:5432:5432` | 영구 볼륨 `postgres_data` |
| Redis | `127.0.0.1:6379:6379` | 비밀번호 인증, 영구 볼륨 `redis_data` |
| pgAdmin | `127.0.0.1:8108:80` | DB 관리 UI |

### 3-2. Frontend (Nginx)

| 서비스 | 포트 | 비고 |
|--------|------|------|
| Nginx | 80, 443 | nginx:stable-alpine |

Nginx는 `moreadmin_default`, `amagetdone-dev_default` 두 외부 네트워크에 연결됩니다.
Backend Compose가 먼저 실행되어 있어야 정상 기동됩니다.

### 3-3. AI

| 서비스 | 포트 | 비고 |
|--------|------|------|
| AI API | `8000:8000` | `docker run` 직접 배포, network-alias: `inference-server` |

AI API는 Nginx를 거치지 않고 Backend가 Docker 내부 네트워크로 직접 호출합니다.

### 3-4. Nginx 라우팅

| 경로 | 대상 | 비고 |
|------|------|------|
| `/` | Flutter Web 정적 파일 | SPA 폴백 |
| `/api/` | `amagetdone-dev-backend:8080` | Backend API 프록시 |
| `/api/v1/chatbot/messages/stream` | `amagetdone-dev-backend:8080` | SSE 전용, `proxy_buffering off` |
| `/admin/` | `moreadmin-app-1:8080` | Admin 앱 프록시 |
| `/healthz` | 200 OK | Nginx 헬스체크 |

HTTPS 443 (Let's Encrypt), HTTP → HTTPS 자동 리다이렉트, `client_max_body_size 3m`


## 4. 빌드/배포 절차

### 4-0. 선행 조건

```bash
docker --version        # 29.x 이상
docker compose version  # v5.x 이상
flutter --version       # stable
```

### 4-1. Backend

```bash
cd BE/amagetdone
cp .env.dev.example .env  # 운영은 .env.prod.example
# .env 필수 항목 입력 후 저장
docker compose build && docker compose up -d
```

`.env` 필수 변경 항목:

| 변수 | 설명 |
|------|------|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | DB 계정/비밀번호 |
| `DB_USERNAME` / `DB_PASSWORD` | 위와 동일하게 |
| `JWT_SECRET` | 256-bit 이상, `openssl rand -base64 32` |
| `REDIS_PASSWORD` | Redis 비밀번호 |
| `KAKAO_NATIVE_APP_KEY` | 카카오 개발자 콘솔 |
| `GOOGLE_CLIENT_ID` | GCP 콘솔 |
| `SSAFY_FIN_API_KEY` | SSAFY 포털 발급 |
| `TRANSACTION_MERCHANT_INFERENCE_BASE_URL` | `http://inference-server:8000` |
| `PLAN_AI_BASE_URL` | `http://inference-server:8000` |

검증: `curl -i http://localhost:8080/api/health`

### 4-2. AI

```bash
cd AI
cp .env.example .env
# .env 필수 항목 입력 후 저장
docker build -t merchant-inference-service:latest .
docker run -d \
  --name merchant-inference-service \
  --network amagetdone-dev_default \
  --network-alias inference-server \
  -p 8000:8000 --env-file .env --restart unless-stopped \
  merchant-inference-service:latest
```

`.env` 필수 변경 항목:

| 변수 | 설명 |
|------|------|
| `AI_API_KEY` | AI API 인증 키 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Backend DB와 동일 정보 |
| `GOOGLE_AI_BASE_URL` | SSAFY GMS 제공 |
| `MORE_FINANCE_MCP_URL` | Backend 내부 URL |

검증: `curl -i http://localhost:8000/health`

### 4-3. Frontend

```bash
cd FE
cp .env.example .env  # KAKAO_NATIVE_APP_KEY, GOOGLE_CLIENT_ID 입력
flutter clean && flutter pub get
flutter build web --release --dart-define-from-file=.env
rm -f build/web/assets/.env  # 반드시 삭제
```

Nginx 배포 (최초):
```bash
cd FE/deploy/web
bash enable_https.sh  # 인증서 발급 (최초 1회)
cp -r ../../build/web/. ./current/
docker compose up -d
```

인증서 갱신: `bash renew_https.sh`

검증:
```bash
curl -i https://j14d101.p.ssafy.io/healthz
curl -i https://j14d101.p.ssafy.io/api/health
```

### 4-4. CI/CD

| 항목 | Frontend | Backend | AI |
|------|----------|---------|-----|
| 트리거 브랜치 | `dev/frontend` | `dev/backend` | `dev/ai` |
| 빌드 | `flutter build web --release` | `./gradlew bootJar` | Docker build |
| 배포 방식 | SSH + rsync | SSH + rsync + Docker Compose | GitLab CI + `docker run` |
| `.env` 위치 | `/home/ubuntu/front/.env` | Jenkins Credentials | `/home/ubuntu/inference/.env` |


## 5. 기능 목록

### 5-1. Frontend
인증/회원(소셜 로그인, 회원가입, 온보딩), 자산 관리(계좌/카드 연동, 1원 인증), 예산 관리(월별/주별 설정·조정), 목표(생성/조회/주차별 현황), 소비 분석, AI 챗봇(SSE 스트리밍), 푸시 알림, 마이페이지

### 5-2. Backend
인증(소셜 로그인, JWT), 회원, 계좌/카드(SSAFY 금융 API, 1원 인증), 예산(월소득·배분·주차 조정), 목표(AI 이미지 생성·영상), 거래 분석(동기화·주차/월별), 마이페이지 대시보드, Swagger/OpenAPI, AWS S3

근거 파일: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/`

### 5-3. AI

| 기능 | 주요 엔드포인트 |
|------|----------------|
| 가맹점 카테고리 분류 (단건/배치 최대 1000건) | `POST /v1/merchant`, `POST /v1/merchant/batch` |
| 챗봇 (LangGraph, MCP 연동, 대화 이력 저장) | `POST /v1/chatbot/messages`, `POST /v1/chatbot/messages/stream` |
| AI 이미지 생성 (Gemini) | `POST /v1/image-generation` |
| 헬스체크 | `GET /health` |


## 6. 환경 변수 정리

> 민감정보는 Git에 직접 커밋하지 않음. 실제 값은 `.env` 또는 CI/CD 시크릿에만 주입.

### 6-1. Backend

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `SPRING_PROFILES_ACTIVE` | `dev` / `prod` | Spring 프로파일 |
| `COMPOSE_PROJECT_NAME` | `amagetdone-dev` | Docker Compose 프로젝트명 |
| `COMPOSE_HOST_PORT` | `8080` | Backend 외부 포트 |
| `POSTGRES_DB` | `amagetdone` | DB명 |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | — | DB 계정/비밀번호 (반드시 변경) |
| `DB_URL` | `jdbc:postgresql://postgres:5432/amagetdone` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | — | `POSTGRES_USER/PASSWORD`와 동일 |
| `REDIS_PASSWORD` | — | Redis 비밀번호 (반드시 변경) |
| `JWT_SECRET` | — | JWT 서명 키, 256-bit 이상 (반드시 변경) |
| `JWT_ACCESS_TOKEN_EXPIRATION_SECONDS` | `3600` | 액세스 토큰 만료(초) |
| `JWT_REFRESH_TOKEN_EXPIRATION_SECONDS` | `1209600` | 리프레시 토큰 만료(초) |
| `KAKAO_NATIVE_APP_KEY` / `GOOGLE_CLIENT_ID` | — | 소셜 로그인 키 |
| `SSAFY_FIN_API_KEY` | — | SSAFY 금융 오픈 API 키 |
| `AWS_ACCESS_KEY` / `AWS_SECRET_KEY` / `AWS_S3_BUCKET` | — | AWS S3 인증 정보 |
| `TRANSACTION_MERCHANT_INFERENCE_BASE_URL` | — | AI 가맹점 분류 서비스 URL |
| `PLAN_AI_BASE_URL` / `PLAN_AI_GENERATE_PATH` | — / `/v1/image-generation` | AI 이미지 생성 URL |
| `PYAN_BASE_URL` / `PYAN_API_KEY` | — | PyAN 드로잉 서비스 |
| `PGADMIN_DEFAULT_EMAIL` / `PGADMIN_DEFAULT_PASSWORD` | — | pgAdmin 접속 정보 |
| `TEST_SOCIAL_USER_EMAIL` / `TEST_SOCIAL_USER_API_KEY` | — | 테스트 소셜 유저 정보 |

### 6-2. Frontend

| 변수 | 설명 |
|------|------|
| `KAKAO_NATIVE_APP_KEY` | 카카오 네이티브 앱 키 |
| `GOOGLE_CLIENT_ID` | Google OAuth 클라이언트 ID |

도메인/URL은 `FE/deploy/web/nginx/https.conf`에서 직접 관리. 도메인 변경 시 `server_name`, `proxy_pass` 수정 필요.

### 6-3. AI

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `MODEL_ID` | `kakao1513/merchant-consumption-category-discriminator-v1` | HuggingFace 모델 ID |
| `MODEL_DEVICE` | `cpu` | 연산 장치 (cpu / cuda) |
| `FALLBACK_LABEL` / `FALLBACK_THRESHOLD` | `기타서비스` / `0.70` | 저신뢰도 폴백 설정 |
| `HF_TOKEN` | — | HuggingFace 인증 토큰 (비공개 모델 시) |
| `AI_API_KEY` | — | AI API 인증 키 (반드시 설정) |
| `OPENAI_BASE_URL` / `GOOGLE_AI_BASE_URL` | — / SSAFY GMS | LLM API 엔드포인트 |
| `IMAGE_GENERATION_MODEL` | `gemini-2.5-flash-image` | 이미지 생성 모델 |
| `MORE_FINANCE_MCP_URL` | — | Finance MCP 서버 URL |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | — | PostgreSQL 접속 정보 (Backend DB와 동일) |
| `LANGFUSE_PUBLIC_KEY` / `LANGFUSE_SECRET_KEY` / `LANGFUSE_BASE_URL` | — | Langfuse LLM 트레이싱 |


## 7. DB 접속 정보

- 설정 파일: `BE/amagetdone/src/main/resources/application[-dev|-prod].properties`
- 환경변수 템플릿: `BE/amagetdone/.env.dev.example` / `.env.prod.example`
- AI 서버는 Backend와 **동일한 PostgreSQL DB**에 접속하여 챗봇 대화 이력을 저장
- 스키마: JPA `ddl-auto=update` — 최초 배포 시 자동 생성. 컬럼 삭제/타입 변경은 수동 마이그레이션 필요

DB 덤프 생성:
```bash
docker exec ${POSTGRES_CONTAINER_NAME} pg_dump \
  -U ${POSTGRES_USER} amagetdone > exec/db_dump_YYYYMMDD.sql
```


## 8. 외부 서비스 목록

| 서비스 | 용도 | 환경변수 / 위치 | 비고 |
|--------|------|----------------|------|
| AWS S3 | 이미지/영상 업로드 | `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_S3_BUCKET` | 유료 |
| Google OAuth 2.0 | 소셜 로그인 | `GOOGLE_CLIENT_ID` (FE + BE) | 무료 |
| Kakao OAuth | 소셜 로그인 | `KAKAO_NATIVE_APP_KEY` (FE + BE) | 무료 |
| Firebase Cloud Messaging | 푸시 알림 | `FE/android/app/google-services.json` | 무료 |
| SSAFY 금융 오픈 API | 계좌/카드/거래내역 | `SSAFY_FIN_API_KEY` | 무료 |
| SSAFY GMS (Gemini) | AI 이미지 생성 | `GOOGLE_AI_BASE_URL` | 무료 |
| Let's Encrypt | HTTPS 인증서 | `FE/deploy/web/certbot/` | 무료 |
| Langfuse | LLM 트레이싱 | `LANGFUSE_PUBLIC_KEY`, `LANGFUSE_SECRET_KEY` | 무료 |
| HuggingFace 모델 | 가맹점 분류 | `MODEL_ID` | 자체 호스팅 |
| PyAN | 드로잉/서명 | `PYAN_BASE_URL`, `PYAN_API_KEY` | 자체 호스팅 |


## 9. 배포 시 특이사항

- **기동 순서**: Backend Compose → AI 컨테이너 → Frontend Nginx 순서로 실행. `amagetdone-dev_default` 네트워크가 먼저 생성되어 있어야 함
- **AI DB 공유**: AI `.env`의 DB 접속 정보를 Backend DB 정보와 반드시 일치시켜야 함
- **SSE 스트리밍**: `/api/v1/chatbot/messages/stream`은 별도 location 블록(`proxy_buffering off`) 적용. Nginx 수정 시 해당 블록 유지 필요
- **Flutter `.env` 보안**: 수동 배포 시 `build/web/assets/.env` 직접 삭제 필요 (Jenkins 자동 처리)
- **AI 모델 초기 로딩**: 최초 기동 시 HuggingFace 모델 다운로드 발생 가능. `HF_HOME=/app/.cache/huggingface`에 캐시됨


## 10. 포트/경로 최종 체크표

### 웹서버 라우팅

| 경로 | 대상 | 비고 |
|------|------|------|
| `https://j14d101.p.ssafy.io/` | Flutter Web 정적 파일 | SPA 폴백 |
| `https://j14d101.p.ssafy.io/api/*` | Backend `127.0.0.1:8080` | Spring context-path `/api` |
| `https://j14d101.p.ssafy.io/api/v1/chatbot/messages/stream` | Backend (SSE) | 버퍼링 비활성 |
| `https://j14d101.p.ssafy.io/admin/` | Admin 앱 | 별도 Compose |
| `https://j14d101.p.ssafy.io/healthz` | Nginx 헬스체크 | `200 OK` |

### 컨테이너 포트

| 서비스 | 포트 | 비고 |
|--------|------|------|
| Backend API | `127.0.0.1:8080:8080` | 기본값 |
| PostgreSQL | `127.0.0.1:5432:5432` | 기본값 |
| Redis | `127.0.0.1:6379:6379` | 비밀번호 인증 |
| pgAdmin | `127.0.0.1:8108:80` | 기본값 |
| AI API | `8000:8000` | `docker run` 직접 배포 |

### Swagger (Backend)
- `http://localhost:8080/api/swagger-ui/index.html`
- `http://localhost:8080/api/v3/api-docs`
