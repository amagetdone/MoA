#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

export BASE="${BASE:-http://127.0.0.1:8080/api}"

if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

LOCAL_USER_ID="${LOCAL_USER_ID:-6}"
LOCAL_PROVIDER="${LOCAL_PROVIDER:-KAKAO}"
JWT_SECRET="${JWT_SECRET:-}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-amagetdone-dev-postgres}"
POSTGRES_USER="${POSTGRES_USER:-d101}"
POSTGRES_DB="${POSTGRES_DB:-amagetdone}"

if [[ -z "$JWT_SECRET" || -z "$REDIS_PASSWORD" ]]; then
  echo "FAIL: JWT_SECRET and REDIS_PASSWORD are required for local user flow"
  exit 1
fi

if [[ -z "$LOCAL_USER_ID" ]]; then
  echo "FAIL: LOCAL_USER_ID is required"
  exit 1
fi

python_cmd() {
  if command -v python >/dev/null 2>&1; then
    python "$@"
    return
  fi
  if command -v py >/dev/null 2>&1; then
    py -3 "$@"
    return
  fi
  echo "FAIL: python interpreter not found" >&2
  exit 1
}

USER_ROW=$(
  docker exec "$POSTGRES_CONTAINER_NAME" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -F '|' -c \
    "select coalesce(email, ''), coalesce(user_key, '') from users where id = ${LOCAL_USER_ID};" 2>/dev/null || true
)
LOCAL_USER_EMAIL="${USER_ROW%%|*}"
USER_KEY="${USER_ROW#*|}"
if [[ "$USER_ROW" != *"|"* ]]; then
  LOCAL_USER_EMAIL=""
  USER_KEY=""
fi

if [[ -z "$LOCAL_USER_EMAIL" ]]; then
  echo "FAIL: local user ${LOCAL_USER_ID} not found"
  exit 1
fi

if [[ -z "$USER_KEY" ]]; then
  FALLBACK_USER_KEY=$(
    docker exec "$POSTGRES_CONTAINER_NAME" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -c \
      "select user_key from users where email = '${LOCAL_USER_EMAIL}' and id <> ${LOCAL_USER_ID} and user_key is not null and user_key <> '' order by id limit 1;" 2>/dev/null || true
  )
  if [[ -n "$FALLBACK_USER_KEY" ]]; then
    docker exec "$POSTGRES_CONTAINER_NAME" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c \
      "update users set user_key = '${FALLBACK_USER_KEY}' where id = ${LOCAL_USER_ID};" >/dev/null
    USER_KEY="$FALLBACK_USER_KEY"
  fi
fi

export USER_KEY

TOKEN_VERSION=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --raw GET "auth:token-version:$LOCAL_USER_ID" 2>/dev/null || true)
TOKEN_VERSION="${TOKEN_VERSION:-0}"

ACCESS_TOKEN=$(LOCAL_USER_ID="$LOCAL_USER_ID" LOCAL_PROVIDER="$LOCAL_PROVIDER" TOKEN_VERSION="$TOKEN_VERSION" JWT_SECRET="$JWT_SECRET" python_cmd - <<'PY'
import base64
import hashlib
import hmac
import json
import os
import time

def b64url(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode()

header = {"alg": "HS256", "typ": "JWT"}
now = int(time.time())
payload = {
    "sub": os.environ["LOCAL_USER_ID"],
    "provider": os.environ["LOCAL_PROVIDER"] or "KAKAO",
    "tokenType": "access",
    "tokenVersion": int(os.environ["TOKEN_VERSION"] or "0"),
    "iat": now,
    "exp": now + 3600,
}
segments = [
    b64url(json.dumps(header, separators=(",", ":")).encode()),
    b64url(json.dumps(payload, separators=(",", ":")).encode()),
]
signing_input = ".".join(segments).encode()
secret = os.environ["JWT_SECRET"].encode()
signature = hmac.new(secret, signing_input, hashlib.sha256).digest()
print(".".join([*segments, b64url(signature)]))
PY
)
export ACCESS_TOKEN

exec "$SCRIPT_DIR/user_flow.sh" "$@"
