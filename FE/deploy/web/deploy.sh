#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NETWORK_NAMES=("moreadmin_default" "amagetdone-dev_default")
DOMAIN="j14d101.p.ssafy.io"
HTTP_CONF="${SCRIPT_DIR}/nginx/http.conf"
HTTPS_CONF="${SCRIPT_DIR}/nginx/https.conf"
ACTIVE_CONF="${SCRIPT_DIR}/nginx/default.conf"
CERT_DIR="${SCRIPT_DIR}/certbot/conf/live/${DOMAIN}"

has_https_certificates() {
  if command -v sudo >/dev/null 2>&1 && \
     sudo -n test -f "${CERT_DIR}/fullchain.pem" && \
     sudo -n test -f "${CERT_DIR}/privkey.pem"; then
    return 0
  fi

  if [ -L "${CERT_DIR}/fullchain.pem" ] && [ -L "${CERT_DIR}/privkey.pem" ]; then
    return 0
  fi

  [ -f "${CERT_DIR}/fullchain.pem" ] && [ -f "${CERT_DIR}/privkey.pem" ]
}

cd "${SCRIPT_DIR}"

mkdir -p "${SCRIPT_DIR}/current" "${SCRIPT_DIR}/nginx" \
  "${SCRIPT_DIR}/certbot/www" "${SCRIPT_DIR}/certbot/conf"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker command not found."
  exit 1
fi

for network_name in "${NETWORK_NAMES[@]}"; do
  if ! docker network inspect "${network_name}" >/dev/null 2>&1; then
    echo "ERROR: docker network '${network_name}' not found."
    exit 1
  fi
done

if [ ! -f "${SCRIPT_DIR}/docker-compose.yml" ]; then
  echo "ERROR: docker-compose.yml not found."
  exit 1
fi

if [ ! -f "${HTTP_CONF}" ]; then
  echo "ERROR: nginx/http.conf not found."
  exit 1
fi

if [ ! -f "${HTTPS_CONF}" ]; then
  echo "ERROR: nginx/https.conf not found."
  exit 1
fi

if [ ! -f "${SCRIPT_DIR}/current/index.html" ]; then
  echo "ERROR: current/index.html not found. Web build artifacts are missing."
  exit 1
fi

if has_https_certificates; then
  cp "${HTTPS_CONF}" "${ACTIVE_CONF}"
  HEALTH_CHECK_URL="https://127.0.0.1/healthz"
  CURL_EXTRA_ARGS="--insecure"
else
  cp "${HTTP_CONF}" "${ACTIVE_CONF}"
  HEALTH_CHECK_URL="http://127.0.0.1/healthz"
  CURL_EXTRA_ARGS=""
fi

docker compose up -d --remove-orphans

for attempt in $(seq 1 30); do
  if curl -fsS ${CURL_EXTRA_ARGS} "${HEALTH_CHECK_URL}" >/dev/null && \
     curl -fsS ${CURL_EXTRA_ARGS} "${HEALTH_CHECK_URL%/healthz}/api/health" >/dev/null && \
     curl -fsS ${CURL_EXTRA_ARGS} "${HEALTH_CHECK_URL%/healthz}/admin/login" >/dev/null; then
    echo "Frontend deploy succeeded."
    exit 0
  fi
  sleep 2
done

echo "ERROR: frontend health check failed."
docker compose logs --tail=100
exit 1
