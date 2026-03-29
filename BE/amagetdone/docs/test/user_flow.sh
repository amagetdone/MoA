#!/usr/bin/env bash
# ==============================================================================
# AmagetDone User Flow
#
# Usage: ./user_flow.sh [--reset]
#   --reset: 사용자 플로우 상태 초기화 후 실행
#
# Required env:
#   ACCESS_TOKEN
#
# Optional env:
#   BASE, ACCOUNT_NO, CARD_NO
# ==============================================================================
set -euo pipefail
export TZ="Asia/Seoul"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
INPUT_BASE="${BASE:-}"
INPUT_ACCESS_TOKEN="${ACCESS_TOKEN:-}"
INPUT_ACCOUNT_NO="${ACCOUNT_NO:-}"
INPUT_CARD_NO="${CARD_NO:-}"
INPUT_USER_KEY="${USER_KEY:-}"
INPUT_FIN_BASE="${FIN_BASE:-}"
INPUT_FIN_API_KEY="${FIN_API_KEY:-}"

if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

BASE="${INPUT_BASE:-${BASE:-https://j14d101.p.ssafy.io/api}}"
ACCESS_TOKEN="${INPUT_ACCESS_TOKEN:-${ACCESS_TOKEN:-}}"
ACCOUNT_NO="${INPUT_ACCOUNT_NO:-${ACCOUNT_NO:-}}"
CARD_NO="${INPUT_CARD_NO:-${CARD_NO:-}}"
USER_KEY="${INPUT_USER_KEY:-${USER_KEY:-}}"
FIN_BASE="${INPUT_FIN_BASE:-${FIN_BASE:-https://finopenapi.ssafy.io/ssafy/api/v1}}"
FIN_API_KEY="${INPUT_FIN_API_KEY:-${FIN_API_KEY:-3eba20d5b7a4430bb53c505cce0c4f18}}"

DO_RESET=false
if [[ "${1:-}" == "--reset" ]]; then
  DO_RESET=true
  shift
fi

if [[ -z "$ACCESS_TOKEN" ]]; then
  echo "FAIL: ACCESS_TOKEN is required (.env or environment)"
  exit 1
fi

STEP=0
step() {
  STEP=$((STEP + 1))
  echo ""
  echo "==== [$STEP] $1 ===="
}

fail() {
  echo "FAIL: $1"
  echo "Response: $2"
  exit 1
}

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

now_in_kst() {
  python_cmd - "$1" <<'PY'
import sys
from datetime import datetime
from zoneinfo import ZoneInfo

print(datetime.now(ZoneInfo("Asia/Seoul")).strftime(sys.argv[1]))
PY
}

gen_unique_no() {
  printf '%s%06d\n' "$(now_in_kst "%Y%m%d%H%M%S")" $((RANDOM % 1000000))
}

fin_header() {
  local api_name="$1"
  local user_key="${2:-}"
  local unique_no now_date now_time
  unique_no=$(gen_unique_no)
  now_date=$(now_in_kst "%Y%m%d")
  now_time=$(now_in_kst "%H%M%S")
  python_cmd - "$api_name" "$now_date" "$now_time" "$unique_no" "$FIN_API_KEY" "$user_key" <<'PY'
import json
import sys

api_name, now_date, now_time, unique_no, api_key, user_key = sys.argv[1:]
header = {
    "apiName": api_name,
    "transmissionDate": now_date,
    "transmissionTime": now_time,
    "institutionCode": "00100",
    "fintechAppNo": "001",
    "apiServiceCode": api_name,
    "institutionTransactionUniqueNo": unique_no,
    "apiKey": api_key,
}
if user_key:
    header["userKey"] = user_key
print(json.dumps(header, separators=(",", ":")))
PY
}

jq_r() {
  local json="$1"
  local path="$2"
  if command -v jq &>/dev/null; then
    echo "$json" | jq -r "$path"
  else
    echo "$json" | python_cmd -c "
import sys, json, re
data = json.load(sys.stdin)
path = '$path'
tokens = re.findall(r'\w+|\[\d+\]', path)
cur = data
for t in tokens:
    if t.startswith('[') and t.endswith(']'):
        cur = cur[int(t[1:-1])]
    else:
        cur = cur[t]
if cur is None:
    print('null')
elif isinstance(cur, bool):
    print('true' if cur else 'false')
else:
    print(cur)
" 2>/dev/null || echo ""
  fi
}

date_shift_days() {
  python_cmd - "$1" "$2" <<'PY'
import sys
from datetime import date, timedelta

base = date.fromisoformat(sys.argv[1])
shift = int(sys.argv[2])
print((base + timedelta(days=shift)).isoformat())
PY
}

current_year_week() {
  python_cmd - "$1" <<'PY'
import sys
from datetime import date

target = date.fromisoformat(sys.argv[1])
year, week, _ = target.isocalendar()
print(f"{year}-W{week:02d}")
PY
}

iso_weekday() {
  python_cmd - "$1" <<'PY'
import sys
from datetime import date

print(date.fromisoformat(sys.argv[1]).isoweekday())
PY
}

calc_target_weeks() {
  python_cmd - "$1" "$2" <<'PY'
import sys
from datetime import date

start = date.fromisoformat(sys.argv[1])
end = date.fromisoformat(sys.argv[2])
days = (end - start).days + 1
print((days + 6) // 7)
PY
}

compact_date_shift_days() {
  python_cmd - "$1" "$2" <<'PY'
import sys
from datetime import date, timedelta

base = date.fromisoformat(sys.argv[1])
shift = int(sys.argv[2])
print((base + timedelta(days=shift)).strftime("%Y%m%d"))
PY
}

plan_field_from_list() {
  local json="$1"
  local plan_id="$2"
  local field="$3"
  echo "$json" | python_cmd -c "
import json
import sys

plan_id = int(sys.argv[1])
field = sys.argv[2]
payload = json.load(sys.stdin).get('data', [])
for plan in payload:
    if int(plan.get('planId', -1)) == plan_id:
        value = plan.get(field)
        print('null' if value is None else value)
        break
else:
    print('')
" "$plan_id" "$field"
}

print_plan_from_list() {
  local json="$1"
  local plan_id="$2"
  echo "$json" | python_cmd -c "
import json
import sys

plan_id = int(sys.argv[1])
payload = json.load(sys.stdin).get('data', [])
for plan in payload:
    if int(plan.get('planId', -1)) == plan_id:
        print(
            f\"  -> planId={plan.get('planId')} | title={plan.get('title')} | \"
            f\"status={plan.get('status')} | saved={plan.get('savedAmount')} | \"
            f\"target={plan.get('targetAmount')} | weeks={plan.get('weekNumber')} | \"
            f\"dday={plan.get('dday')} | rate={plan.get('achievementRate')}\"
        )
        break
else:
    print('  -> plan not found')
" "$plan_id"
}

AUTH="Authorization: Bearer $ACCESS_TOKEN"
TOKEN_SUB=$(python_cmd - <<'PY'
import base64
import json
import os

token = os.environ.get("ACCESS_TOKEN", "")
parts = token.split(".")
if len(parts) < 2:
    print("?")
    raise SystemExit
payload = parts[1] + "=" * ((4 - len(parts[1]) % 4) % 4)
try:
    data = json.loads(base64.urlsafe_b64decode(payload.encode()).decode())
    print(data.get("sub", "?"))
except Exception:
    print("?")
PY
)

echo "=========================================="
echo " AmagetDone User Flow - User ID: $TOKEN_SUB"
echo "=========================================="

if [[ "$DO_RESET" == "true" ]]; then
  step "사용자 플로우 상태 초기화"
  RESET_RES=$(curl -sk -X POST "$BASE/v1/members/me/flow-reset" \
    -H "$AUTH" -H "Content-Type: application/json")
  RESET_OK=$(jq_r "$RESET_RES" '.data.success')
  if [[ "$RESET_OK" != "true" ]]; then
    fail "플로우 상태 초기화 실패" "$RESET_RES"
  fi
  echo "  -> 사용자 플로우 상태 초기화 완료"
fi

step "내 계좌 조회"
ACCOUNTS_RES=$(curl -sk "$BASE/v1/accounts/me" -H "$AUTH")
HAS_ACCOUNT=true
if [[ -z "$ACCOUNT_NO" ]]; then
  ACCOUNT_NO=$(jq_r "$ACCOUNTS_RES" '.data[0].accountNo')
fi
if [[ -z "$ACCOUNT_NO" || "$ACCOUNT_NO" == "null" ]]; then
  HAS_ACCOUNT=false
  ACCOUNT_NO=""
  echo "  -> 연결 계좌 없음: 자동 계좌 연결을 시도합니다."
else
  export ACCOUNT_NO
  echo "  -> 계좌 선택: $ACCOUNT_NO"
fi

if [[ "$HAS_ACCOUNT" != "true" ]]; then
  step "계정계 계좌 조회"
  BANK_RES=$(curl -sk "$BASE/v1/accounts/bank" -H "$AUTH")
  ACCOUNT_NO=$(jq_r "$BANK_RES" '.data[0].accountNo')
  if [[ -z "$ACCOUNT_NO" || "$ACCOUNT_NO" == "null" ]]; then
    fail "계정계 계좌 조회 실패 또는 계좌 없음" "$BANK_RES"
  fi
  export ACCOUNT_NO
  echo "  -> 계정계 계좌 선택: $ACCOUNT_NO"

  step "계좌 실소유 확인"
  CONFIRM_RES=$(curl -sk -X POST "$BASE/v1/accounts/confirm" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d "{\"accountNo\": \"$ACCOUNT_NO\"}")
  CONFIRMED=$(jq_r "$CONFIRM_RES" '.data.confirmed')
  if [[ "$CONFIRMED" != "true" ]]; then
    fail "계좌 실소유 확인 실패" "$CONFIRM_RES"
  fi
  echo "  -> 실소유 확인 완료"

  step "1원 인증 요청"
  VERIFY_REQ_RES=$(curl -sk -X POST "$BASE/v1/accounts/verification/requests" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d "{\"accountNo\": \"$ACCOUNT_NO\"}")
  VERIFY_REQ_ID=$(jq_r "$VERIFY_REQ_RES" '.data.verificationId')
  if [[ -z "$VERIFY_REQ_ID" || "$VERIFY_REQ_ID" == "null" ]]; then
    fail "1원 인증 요청 실패" "$VERIFY_REQ_RES"
  fi
  echo "  -> verificationId=$VERIFY_REQ_ID"

  if [[ -z "$USER_KEY" || -z "$FIN_API_KEY" ]]; then
    fail "1원 인증 코드 조회에 필요한 USER_KEY 또는 FIN_API_KEY가 없음" \
      "{\"USER_KEY\":\"${USER_KEY:-}\",\"FIN_API_KEY\":\"${FIN_API_KEY:+SET}\"}"
  fi

  step "1원 인증 코드 조회"
  END_DATE=$(now_in_kst "%Y%m%d")
  START_DATE=$(compact_date_shift_days "$(now_in_kst "%Y-%m-%d")" -7)
  VERIFY_TX_RES=$(curl -sk -X POST "$FIN_BASE/edu/demandDeposit/inquireTransactionHistoryList" \
    -H "Content-Type: application/json" \
    -d "{\"Header\": $(fin_header inquireTransactionHistoryList "$USER_KEY"), \"accountNo\": \"$ACCOUNT_NO\", \"startDate\": \"$START_DATE\", \"endDate\": \"$END_DATE\", \"transactionType\": \"A\", \"orderByType\": \"DESC\"}")
  AUTH_CODE=$(printf '%s' "$VERIFY_TX_RES" | python_cmd -c '
import json
import re
import sys

try:
    payload = json.load(sys.stdin).get("REC", {})
    records = payload.get("list", []) if isinstance(payload, dict) else payload
    for record in records:
        text = " ".join([
            str(record.get("transactionSummary", "") or ""),
            str(record.get("transactionMemo", "") or ""),
        ]).strip()
        match = re.search(r"(\d{4,})", text)
        if match:
            print(match.group(1))
            break
    else:
        print("")
except Exception:
    print("")
')
  if [[ -z "$AUTH_CODE" ]]; then
    fail "1원 인증 코드 조회 실패" "$VERIFY_TX_RES"
  fi
  echo "  -> authCode=$AUTH_CODE"

  step "1원 인증 확인"
  VERIFY_CHECK_RES=$(curl -sk -X POST "$BASE/v1/accounts/verification/check" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d "{\"accountNo\": \"$ACCOUNT_NO\", \"authCode\": \"$AUTH_CODE\"}")
  VERIFIED=$(jq_r "$VERIFY_CHECK_RES" '.data.verified')
  if [[ "$VERIFIED" != "true" ]]; then
    fail "1원 인증 확인 실패" "$VERIFY_CHECK_RES"
  fi
  echo "  -> 계좌 연결 완료"
fi

if [[ "$HAS_ACCOUNT" == "true" ]]; then
  step "내 카드 조회"
  CARDS_RES=$(curl -sk "$BASE/v1/accounts/me/cards" -H "$AUTH")
  if [[ -z "$CARD_NO" ]]; then
    CARD_NO=$(printf '%s' "$CARDS_RES" | python_cmd -c '
import json
import os
import sys

try:
    data = json.load(sys.stdin).get("data", [])
    selected = ""
    for card in data:
        if card.get("linkedAccountNo") == os.environ.get("ACCOUNT_NO"):
            selected = card.get("cardNo", "")
            break
    if not selected and data:
        selected = data[0].get("cardNo", "")
    print(selected)
except Exception:
    print("")
')
  fi
  if [[ -z "$CARD_NO" || "$CARD_NO" == "null" ]]; then
    echo "  -> 연결 카드 없음: 카드 거래내역 초기화는 건너뜁니다."
  else
    echo "  -> 카드 선택: $CARD_NO"
  fi

  step "계좌 거래내역 초기화 (admin/data-init/account)"
  INIT_ACCT_RES=$(curl -sk -X POST "$BASE/admin/data-init/account" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d "{\"accountNo\": \"$ACCOUNT_NO\"}")
  INIT_ACCT_COUNT=$(jq_r "$INIT_ACCT_RES" '.data.transactionCount')
  echo "  -> 계좌 거래내역 ${INIT_ACCT_COUNT}건 초기화"

  if [[ -n "$CARD_NO" && "$CARD_NO" != "null" ]]; then
    step "카드 거래내역 초기화 (admin/data-init/card)"
    INIT_CARD_RES=$(curl -sk -X POST "$BASE/admin/data-init/card" \
      -H "$AUTH" -H "Content-Type: application/json" \
      -d "{\"cardNo\": \"$CARD_NO\"}")
    INIT_CARD_COUNT=$(jq_r "$INIT_CARD_RES" '.data.transactionCount')
    echo "  -> 카드 거래내역 ${INIT_CARD_COUNT}건 초기화"
  fi
fi

if [[ "$HAS_ACCOUNT" != "true" ]]; then
  step "내 카드 조회"
  CARDS_RES=$(curl -sk "$BASE/v1/accounts/me/cards" -H "$AUTH")
  if [[ -z "$CARD_NO" ]]; then
    CARD_NO=$(printf '%s' "$CARDS_RES" | python_cmd -c '
import json
import os
import sys

try:
    data = json.load(sys.stdin).get("data", [])
    selected = ""
    for card in data:
        if card.get("linkedAccountNo") == os.environ.get("ACCOUNT_NO"):
            selected = card.get("cardNo", "")
            break
    if not selected and data:
        selected = data[0].get("cardNo", "")
    print(selected)
except Exception:
    print("")
')
  fi
  if [[ -z "$CARD_NO" || "$CARD_NO" == "null" ]]; then
    echo "  -> 연결 카드 없음: 카드 거래내역 초기화는 건너뜁니다."
  else
    echo "  -> 카드 선택: $CARD_NO"
  fi

  step "계좌 거래내역 초기화 (admin/data-init/account)"
  INIT_ACCT_RES=$(curl -sk -X POST "$BASE/admin/data-init/account" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d "{\"accountNo\": \"$ACCOUNT_NO\"}")
  INIT_ACCT_COUNT=$(jq_r "$INIT_ACCT_RES" '.data.transactionCount')
  echo "  -> 계좌 거래내역 ${INIT_ACCT_COUNT}건 초기화"

  if [[ -n "$CARD_NO" && "$CARD_NO" != "null" ]]; then
    step "카드 거래내역 초기화 (admin/data-init/card)"
    INIT_CARD_RES=$(curl -sk -X POST "$BASE/admin/data-init/card" \
      -H "$AUTH" -H "Content-Type: application/json" \
      -d "{\"cardNo\": \"$CARD_NO\"}")
    INIT_CARD_COUNT=$(jq_r "$INIT_CARD_RES" '.data.transactionCount')
    echo "  -> 카드 거래내역 ${INIT_CARD_COUNT}건 초기화"
  fi
fi

step "거래 분석"
ANALYSIS_RES=$(curl -sk -X POST "$BASE/transactions/analysis" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"months": 6}')
ANALYSIS_MSG=$(jq_r "$ANALYSIS_RES" '.status')
echo "  -> 분석 상태: $ANALYSIS_MSG"

step "월급 등록 (3,000,000원)"
INCOME_RES=$(curl -sk -X POST "$BASE/budgets/income" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"monthlyIncome": 3000000}')
INCOME_MONTH=$(jq_r "$INCOME_RES" '.data.baseMonth')
echo "  -> 기준월: $INCOME_MONTH"

step "예산 배분"
ALLOCATE_RES=$(curl -sk -X POST "$BASE/budgets/allocate" \
  -H "$AUTH" -H "Content-Type: application/json")
ALLOC_SUCCESS=$(jq_r "$ALLOCATE_RES" '.data.success')
echo "  -> 배분 결과: $ALLOC_SUCCESS"

TODAY=$(date +%Y-%m-%d)
if date --version &>/dev/null 2>&1; then
  START_A=$(date -d "$TODAY - 3 months" +%Y-%m-%d)
  END_A=$(date -d "$TODAY" +%Y-%m-%d)
  START_B=$(date -d "$TODAY - 2 months" +%Y-%m-%d)
  END_B=$(date -d "$TODAY + 1 month" +%Y-%m-%d)
  START_C=$(date -d "$TODAY - 1 month" +%Y-%m-%d)
  END_C=$(date -d "$TODAY + 2 months" +%Y-%m-%d)
else
  START_A=$(date -v-3m +%Y-%m-%d)
  END_A=$(date +%Y-%m-%d)
  START_B=$(date -v-2m +%Y-%m-%d)
  END_B=$(date -v+1m +%Y-%m-%d)
  START_C=$(date -v-1m +%Y-%m-%d)
  END_C=$(date -v+2m +%Y-%m-%d)
fi

step "목표 A 생성: 맥북 프로 구매 (~100%)"
echo "  -> 기간: $START_A ~ $END_A"
PLAN_A_RES=$(curl -sk -X POST "$BASE/v1/plans" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"title\": \"맥북 프로 구매\", \"targetAmount\": 900000, \"startDate\": \"$START_A\", \"endDate\": \"$END_A\", \"priority\": 1}")
PLAN_A_ID=$(jq_r "$PLAN_A_RES" '.data.planId')
echo "  -> planId=$PLAN_A_ID"

step "목표 B 생성: 여행 자금 모으기 (~66%)"
echo "  -> 기간: $START_B ~ $END_B"
PLAN_B_RES=$(curl -sk -X POST "$BASE/v1/plans" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"title\": \"여행 자금 모으기\", \"targetAmount\": 600000, \"startDate\": \"$START_B\", \"endDate\": \"$END_B\", \"priority\": 2}")
PLAN_B_ID=$(jq_r "$PLAN_B_RES" '.data.planId')
echo "  -> planId=$PLAN_B_ID"

step "목표 C 생성: 자격증 교재 구매 (~33%)"
echo "  -> 기간: $START_C ~ $END_C"
PLAN_C_RES=$(curl -sk -X POST "$BASE/v1/plans" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"title\": \"자격증 교재 구매\", \"targetAmount\": 300000, \"startDate\": \"$START_C\", \"endDate\": \"$END_C\", \"priority\": 3}")
PLAN_C_ID=$(jq_r "$PLAN_C_RES" '.data.planId')
echo "  -> planId=$PLAN_C_ID"

step "Backfill 실행"
BACKFILL_RES=$(curl -sk -X POST "$BASE/test/budget/savings-accrual/backfill" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"planId\": $PLAN_A_ID}")
BACKFILL_TYPE=$(jq_r "$BACKFILL_RES" '.data.operationType')
BACKFILL_AMOUNT=$(jq_r "$BACKFILL_RES" '.data.appliedAmount')
echo "  -> type=$BACKFILL_TYPE, appliedAmount=$BACKFILL_AMOUNT"

echo ""
echo "  -- Backfill 결과 --"
echo "$BACKFILL_RES" | python_cmd -c "
import sys, json
data = json.load(sys.stdin)
plans = data.get('data', {}).get('plans', [])
for p in plans:
    title = p.get('planTitle', '?')
    saved = p.get('savedAmount', 0)
    target = p.get('finalAmount', 0)
    rate = (saved / target * 100) if target > 0 else 0
    print(f'  {title}: {saved:,} / {target:,} ({rate:.1f}%)')
"

step "홈 화면 조회"
RUN_TAG=$(date +%Y%m%d%H%M%S)
CURRENT_YEAR_WEEK=$(current_year_week "$TODAY")
TODAY_WEEKDAY=$(iso_weekday "$TODAY")

STATE_COMPLETED_TITLE="STATE-COMPLETED-${TOKEN_SUB}-${RUN_TAG}"
STATE_COMPLETED_START=$(date_shift_days "$TODAY" "-$((TODAY_WEEKDAY - 1))")
STATE_COMPLETED_END=$(date_shift_days "$STATE_COMPLETED_START" 6)
STATE_COMPLETED_TARGET=1000

STATE_DELAYED_TITLE="STATE-DELAYED-${TOKEN_SUB}-${RUN_TAG}"
STATE_DELAYED_START=$(date_shift_days "$TODAY" -14)
STATE_DELAYED_END=$(date_shift_days "$TODAY" 14)
STATE_DELAYED_TARGET=400000
STATE_DELAYED_BASE_WEEKS=$(calc_target_weeks "$STATE_DELAYED_START" "$STATE_DELAYED_END")
STATE_DELAYED_EXPECTED_WEEKS=$((STATE_DELAYED_BASE_WEEKS + 1))
STATE_FORCED_FREE_BUDGET_SUBTRACT=2500000

step "Goal-state 완료 목표 생성"
echo "  -> 기간: $STATE_COMPLETED_START ~ $STATE_COMPLETED_END"
STATE_COMPLETED_RES=$(curl -sk -X POST "$BASE/v1/plans" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"title\": \"$STATE_COMPLETED_TITLE\", \"targetAmount\": $STATE_COMPLETED_TARGET, \"startDate\": \"$STATE_COMPLETED_START\", \"endDate\": \"$STATE_COMPLETED_END\", \"priority\": 1}")
STATE_COMPLETED_PLAN_ID=$(jq_r "$STATE_COMPLETED_RES" '.data.planId')
if [[ -z "$STATE_COMPLETED_PLAN_ID" || "$STATE_COMPLETED_PLAN_ID" == "null" ]]; then
  fail "goal-state completed goal creation failed" "$STATE_COMPLETED_RES"
fi
echo "  -> planId=$STATE_COMPLETED_PLAN_ID"

step "Goal-state 완료 목표 현재 주 allocation 강제 실행"
STATE_COMPLETED_FORCE_RES=$(curl -sk -X POST "$BASE/test/budget/weeks/current/savings-allocation/force" \
  -H "$AUTH" -H "Content-Type: application/json")
echo "  -> totalSaved=$(jq_r "$STATE_COMPLETED_FORCE_RES" '.data.totalSavedAmount')"

step "Goal-state 완료 목표 검증"
STATE_COMPLETED_PLANS_RES=$(curl -sk "$BASE/v1/plans" -H "$AUTH")
STATE_COMPLETED_STATUS=$(plan_field_from_list "$STATE_COMPLETED_PLANS_RES" "$STATE_COMPLETED_PLAN_ID" "status")
STATE_COMPLETED_SAVED=$(plan_field_from_list "$STATE_COMPLETED_PLANS_RES" "$STATE_COMPLETED_PLAN_ID" "savedAmount")
if [[ "$STATE_COMPLETED_STATUS" != "COMPLETED" ]]; then
  fail "goal-state completed goal was not completed" "$STATE_COMPLETED_PLANS_RES"
fi
print_plan_from_list "$STATE_COMPLETED_PLANS_RES" "$STATE_COMPLETED_PLAN_ID"

step "Goal-state 연장 목표 free budget 부족분 강제 적용"
STATE_DELAYED_ADJUST_RES=$(curl -sk -X POST "$BASE/budgets/weeks/$CURRENT_YEAR_WEEK/adjustments" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"type\": \"FREE_BUDGET_SUBTRACT\", \"amount\": $STATE_FORCED_FREE_BUDGET_SUBTRACT, \"reason\": \"Force one-week goal shortfall\"}")
if [[ "$(jq_r "$STATE_DELAYED_ADJUST_RES" '.data.adjustmentType')" != "FREE_BUDGET_SUBTRACT" ]]; then
  fail "goal-state delayed free budget adjustment failed" "$STATE_DELAYED_ADJUST_RES"
fi
echo "  -> yearWeek=$CURRENT_YEAR_WEEK"

step "Goal-state 연장 목표 생성"
echo "  -> 기간: $STATE_DELAYED_START ~ $STATE_DELAYED_END"
STATE_DELAYED_CREATE_RES=$(curl -sk -X POST "$BASE/v1/plans" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"title\": \"$STATE_DELAYED_TITLE\", \"targetAmount\": $STATE_DELAYED_TARGET, \"startDate\": \"$STATE_DELAYED_START\", \"endDate\": \"$STATE_DELAYED_END\", \"priority\": 2}")
STATE_DELAYED_PLAN_ID=$(jq_r "$STATE_DELAYED_CREATE_RES" '.data.planId')
if [[ -z "$STATE_DELAYED_PLAN_ID" || "$STATE_DELAYED_PLAN_ID" == "null" ]]; then
  fail "goal-state delayed goal creation failed" "$STATE_DELAYED_CREATE_RES"
fi
echo "  -> planId=$STATE_DELAYED_PLAN_ID"

step "Goal-state 연장 목표 과거 주 backfill"
STATE_DELAYED_BACKFILL_RES=$(curl -sk -X POST "$BASE/test/budget/savings-accrual/backfill" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{}')
echo "  -> appliedAmount=$(jq_r "$STATE_DELAYED_BACKFILL_RES" '.data.appliedAmount')"

step "Goal-state 연장 목표 현재 주 반영"
STATE_DELAYED_HOME_RES=$(curl -sk "$BASE/budgets/home" -H "$AUTH")
echo "  -> weeklySaving=$(jq_r "$STATE_DELAYED_HOME_RES" '.data.weeklySaving.thisWeekSavingAmount')"

step "Goal-state 연장 목표 검증"
STATE_DELAYED_PLANS_RES=$(curl -sk "$BASE/v1/plans" -H "$AUTH")
STATE_DELAYED_STATUS=$(plan_field_from_list "$STATE_DELAYED_PLANS_RES" "$STATE_DELAYED_PLAN_ID" "status")
STATE_DELAYED_WEEKS=$(plan_field_from_list "$STATE_DELAYED_PLANS_RES" "$STATE_DELAYED_PLAN_ID" "weekNumber")
STATE_DELAYED_SAVED=$(plan_field_from_list "$STATE_DELAYED_PLANS_RES" "$STATE_DELAYED_PLAN_ID" "savedAmount")
if [[ "$STATE_DELAYED_STATUS" != "ACTIVE" ]]; then
  fail "goal-state delayed goal should remain active" "$STATE_DELAYED_PLANS_RES"
fi
if [[ "$STATE_DELAYED_WEEKS" != "$STATE_DELAYED_EXPECTED_WEEKS" ]]; then
  fail "goal-state delayed goal was not extended by exactly one week" "$STATE_DELAYED_PLANS_RES"
fi
print_plan_from_list "$STATE_DELAYED_PLANS_RES" "$STATE_DELAYED_PLAN_ID"

step "Goal-state 요약"
echo "  -> completedPlanId=$STATE_COMPLETED_PLAN_ID | status=$STATE_COMPLETED_STATUS | saved=$STATE_COMPLETED_SAVED"
echo "  -> delayedPlanId=$STATE_DELAYED_PLAN_ID | status=$STATE_DELAYED_STATUS | saved=$STATE_DELAYED_SAVED | weeks=$STATE_DELAYED_WEEKS"

HOME_RES=$(curl -sk "$BASE/budgets/home" -H "$AUTH")
echo "$HOME_RES" | python_cmd -c "
import sys, json
data = json.load(sys.stdin).get('data', {})
fs = data.get('freeSpending', {})
print(f'  [자유소비] 사용: {fs.get(\"usedAmount\",0):,} / 총: {fs.get(\"totalAmount\",0):,} | 잔여: {fs.get(\"remainingAmount\",0):,} | 남은일: {fs.get(\"daysRemaining\",0)}일 | 일평균: {fs.get(\"dailyAvailable\",0):,}')
ws = data.get('weeklySaving', {})
print(f'  [주간저축] 이번주: {ws.get(\"thisWeekSavingAmount\",0):,} | 목표잔여: {ws.get(\"remainingToTargetAmount\",0):,} | 진행: {ws.get(\"progressRate\",0)}%')
asset = data.get('assetSummary', {})
print(f'  [자산총액] {asset.get(\"totalAssetAmount\",0):,}')
print()
print('  -- 목표 현황 --')
fmt = '  {:<4} {:<20} {:>12} {:>12} {:>6} {:>6}'
print(fmt.format('P', 'Title', 'Saved', 'Target', 'Rate', 'D-Day'))
print('  ' + '-' * 66)
for p in data.get('plans', []):
    print(fmt.format(
        p.get('priority', '?'),
        p.get('title', '?')[:18],
        f\"{p.get('savedAmount',0):,}\",
        f\"{p.get('targetAmount',0):,}\",
        f\"{p.get('progressRate',0)}%\",
        str(p.get('dDay', '?'))
    ))
"

step "목표 목록 조회"
PLANS_RES=$(curl -sk "$BASE/v1/plans" -H "$AUTH")
echo "$PLANS_RES" | python_cmd -c "
import sys, json
plans = json.load(sys.stdin).get('data', [])
fmt = '  {:<4} {:<20} {:>12} {:>12} {:>8} {:>6}'
print(fmt.format('P', 'Title', 'Saved', 'Target', 'Rate', 'D-Day'))
print('  ' + '-' * 68)
for p in plans:
    print(fmt.format(
        p.get('priority', '?'),
        p.get('title', '?')[:18],
        f\"{p.get('savedAmount',0):,}\",
        f\"{p.get('targetAmount',0):,}\",
        f\"{p.get('achievementRate',0)}%\",
        str(p.get('dday', '?'))
    ))
"

echo ""
echo "=========================================="
echo " User Flow 완료!"
echo "=========================================="
