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
- `docker-compose.yml`: FE 웹 컨테이너 구성
- `deploy.sh`: 원격 배포/헬스체크 스크립트

## 포트 정책
- FE Nginx: `80`
- Jenkins: `8081`
- Backend: `8080`
- PostgreSQL: `5432`

## 참고
- 기존 백엔드 네트워크 `moreadmin_default` 에 FE Nginx가 외부 네트워크로 참여한다.
- `/api/...` 요청은 `moreadmin-app-1:8080/...` 으로 프록시된다.
- Jenkins는 원격 `/home/ubuntu/front/.env` 를 먼저 받아와서 FE web build에 사용한다.
