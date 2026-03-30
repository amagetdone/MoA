# FE Web Deploy

## Jenkins job 설정
- Jenkins 유형: Multibranch Pipeline
- 저장소: `s14-fintech-finance-sub1/S14P21D101`
- Script Path: `FE/Jenkinsfile`
- 수동 배포 대상 브랜치: `dev/frontend`

## Jenkins credentials
- 별도 Jenkins credential 없이 서버 내부 전용 SSH 키를 사용한다.
- Jenkins 컨테이너 경로: `/var/jenkins_home/.ssh/id_ed25519_front_deploy`
- 대상 계정: `ubuntu`

## 원격 서버 경로
- `/home/ubuntu/front`

## 배포 구조
- `current/`: Flutter web 정적 산출물
- `nginx/default.conf`: Nginx 설정
- `nginx/http.conf`: 인증서 발급 전 HTTP 전용 설정
- `nginx/https.conf`: 인증서 발급 후 HTTPS 설정
- `docker-compose.yml`: FE 웹 컨테이너 구성
- `deploy.sh`: 원격 배포/헬스체크 스크립트
- `enable_https.sh`: Let's Encrypt 인증서 발급 및 HTTPS 전환 스크립트
- `renew_https.sh`: Let's Encrypt 인증서 갱신 스크립트

## Jenkins 배포 범위
- `MORE-FE` Jenkins 배포는 `current/` 정적 산출물만 갱신한다.
- `nginx/`, `docker-compose.yml`, `deploy.sh` 등 프록시/컨테이너 설정은 Jenkins가 덮어쓰지 않는다.
- 공용 Nginx 라우팅(`/`, `/admin`, `/api`)과 HTTPS 설정은 서버 측 인프라 관리 대상으로 분리한다.
- FE 배포 후에는 기존 Nginx 컨테이너가 `current/` 볼륨을 그대로 바라보며 새 정적 파일만 서빙한다.

## 포트 정책
- FE Nginx: `80`, `443`
- Jenkins: `8081`
- Backend: `8080`
- PostgreSQL: `5432`

## 참고
- 기존 백엔드 네트워크 `moreadmin_default` 에 FE Nginx가 외부 네트워크로 참여한다.
- `/api/...` 요청은 `moreadmin-app-1:8080/...` 으로 프록시된다.
- Jenkins는 원격 `/home/ubuntu/front/.env` 를 먼저 받아와서 FE build에 사용한다.
- Web 배포 시 `.env` 자산 파일은 제거하고, 웹 런타임은 `dart-define` 또는 기본값만 사용한다.
- 초기 Nginx/HTTPS/bootstrap 구성은 수동 또는 인프라 작업으로 한 번만 반영한다.
