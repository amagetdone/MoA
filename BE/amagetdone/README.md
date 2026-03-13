# amagetdone BE

`more` 서비스 백엔드 프로젝트입니다. Spring Boot 3 기반으로 동작하며 기본 Servlet Context Path는 `/api` 입니다.

## 실행

로컬 인프라와 함께 실행하려면 아래 명령을 사용합니다.

```bash
docker compose up -d --build
```

애플리케이션 기본 포트는 `8080` 이며, 기본 API 베이스 URL은 아래와 같습니다.

```text
http://localhost:8080/api
```

## Swagger

Swagger UI와 OpenAPI 문서는 `server.servlet.context-path=/api` 를 기준으로 노출됩니다.

- Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

애플리케이션이 다른 호스트나 포트에서 실행 중이면 `localhost:8080` 부분만 해당 주소로 바꾸면 됩니다.

## 참고 엔드포인트

- Health Check: `GET /api/health`
