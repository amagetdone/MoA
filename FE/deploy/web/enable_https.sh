#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOMAIN="${1:-j14d101.p.ssafy.io}"
EMAIL="${2:-}"

cd "${SCRIPT_DIR}"

mkdir -p "${SCRIPT_DIR}/certbot/www" "${SCRIPT_DIR}/certbot/conf" "${SCRIPT_DIR}/nginx"

cp "${SCRIPT_DIR}/nginx/http.conf" "${SCRIPT_DIR}/nginx/default.conf"
docker compose up -d nginx

CERTBOT_ARGS=(
  certonly
  --webroot
  -w /var/www/certbot
  -d "${DOMAIN}"
  --non-interactive
  --agree-tos
)

if [ -n "${EMAIL}" ]; then
  CERTBOT_ARGS+=(-m "${EMAIL}")
else
  CERTBOT_ARGS+=(--register-unsafely-without-email)
fi

docker run --rm \
  -v "${SCRIPT_DIR}/certbot/www:/var/www/certbot" \
  -v "${SCRIPT_DIR}/certbot/conf:/etc/letsencrypt" \
  certbot/certbot "${CERTBOT_ARGS[@]}"

cp "${SCRIPT_DIR}/nginx/https.conf" "${SCRIPT_DIR}/nginx/default.conf"
docker compose up -d --remove-orphans
docker exec more-frontend-nginx nginx -s reload

echo "HTTPS enabled for ${DOMAIN}"
