#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "${SCRIPT_DIR}"

docker run --rm \
  -v "${SCRIPT_DIR}/certbot/www:/var/www/certbot" \
  -v "${SCRIPT_DIR}/certbot/conf:/etc/letsencrypt" \
  certbot/certbot renew --webroot -w /var/www/certbot --quiet

docker exec more-frontend-nginx nginx -s reload

echo "HTTPS certificates renewed."
