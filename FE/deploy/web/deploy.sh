#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NETWORK_NAME="moreadmin_default"

cd "${SCRIPT_DIR}"

mkdir -p "${SCRIPT_DIR}/current" "${SCRIPT_DIR}/nginx"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker command not found."
  exit 1
fi

if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
  echo "ERROR: docker network '${NETWORK_NAME}' not found."
  exit 1
fi

if [ ! -f "${SCRIPT_DIR}/docker-compose.yml" ]; then
  echo "ERROR: docker-compose.yml not found."
  exit 1
fi

if [ ! -f "${SCRIPT_DIR}/nginx/default.conf" ]; then
  echo "ERROR: nginx/default.conf not found."
  exit 1
fi

if [ ! -f "${SCRIPT_DIR}/current/index.html" ]; then
  echo "ERROR: current/index.html not found. Web build artifacts are missing."
  exit 1
fi

docker compose up -d --remove-orphans

for attempt in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1/" >/dev/null && \
     curl -fsS "http://127.0.0.1/api/health" >/dev/null; then
    echo "Frontend deploy succeeded."
    exit 0
  fi
  sleep 2
done

echo "ERROR: frontend health check failed."
docker compose logs --tail=100
exit 1
