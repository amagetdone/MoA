# FE Web Deploy

## Jenkins job 설정
- Jenkins 유형: Multibranch Pipeline
- 저장소: `s14-fintech-finance-sub1/S14P21D101`
- Script Path: `FE/Jenkinsfile`
- 수동 배포 대상 브랜치: `dev/frontend`

## Jenkins credentials
- ID: `j14d101-ssh`
- 타입: SSH Username with private key
- Username: `ubuntu`
- Private Key: `J14D101T.pem`

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
