#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export BASE="${BASE:-http://127.0.0.1:8080/api}"
export FIN_BASE="${FIN_BASE:-https://finopenapi.ssafy.io/ssafy/api/v1}"
export FIN_API_KEY="${FIN_API_KEY:-3eba20d5b7a4430bb53c505cce0c4f18}"
export TEST_EMAIL_PREFIX="${TEST_EMAIL_PREFIX:-moa}"
export TEST_EMAIL_DOMAIN="${TEST_EMAIL_DOMAIN:-tester.com}"

exec "$SCRIPT_DIR/test_flow.sh" "$@"
