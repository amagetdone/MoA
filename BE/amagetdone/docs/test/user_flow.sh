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

gen_unique_no() {
  date +"%Y%m%d%H%M%S"$(printf "%06d" $((RANDOM % 1000000)))
}

fin_header() {
  local api_name="$1"
  local user_key="${2:-}"
  local unique_no
  unique_no=$(gen_unique_no)
  local now_date
  now_date=$(date +"%Y%m%d")
  local now_time
  now_time=$(date +"%H%M%S")

  local header
  header=$(cat <<HEADER_EOF
{
  "apiName": "${api_name}",
  "transmissionDate": "${now_date}",
  "transmissionTime": "${now_time}",
  "institutionCode": "00100",
  "fintechAppNo": "001",
  "apiServiceCode": "${api_name}",
  "institutionTransactionUniqueNo": "${unique_no}",
  "apiKey": "${FIN_API_KEY}"
HEADER_EOF
)
  if [[ -n "$user_key" ]]; then
    header="${header}, \"userKey\": \"${user_key}\""
  fi
  header="${header} }"
  echo "$header"
}

jq_r() {
  local json="$1"
  local path="$2"
  if command -v jq &>/dev/null; then
    echo "$json" | jq -r "$path"
  else
    echo "$json" | python3 -c "
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

AUTH="Authorization: Bearer $ACCESS_TOKEN"
TOKEN_SUB=$(python3 - <<'PY'
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
  START_DATE=$(date -d "7 days ago" +%Y%m%d 2>/dev/null || date -v-7d +%Y%m%d)
  END_DATE=$(date +%Y%m%d)
  VERIFY_TX_RES=$(curl -sk -X POST "$FIN_BASE/edu/demandDeposit/inquireTransactionHistoryList" \
    -H "Content-Type: application/json" \
    -d "{\"Header\": $(fin_header inquireTransactionHistoryList "$USER_KEY"), \"accountNo\": \"$ACCOUNT_NO\", \"startDate\": \"$START_DATE\", \"endDate\": \"$END_DATE\", \"transactionType\": \"A\", \"orderByType\": \"DESC\"}")
  AUTH_CODE=$(printf '%s' "$VERIFY_TX_RES" | python3 -c '
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
    CARD_NO=$(printf '%s' "$CARDS_RES" | python3 -c '
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
    CARD_NO=$(printf '%s' "$CARDS_RES" | python3 -c '
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
echo "$BACKFILL_RES" | python3 -c "
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
HOME_RES=$(curl -sk "$BASE/budgets/home" -H "$AUTH")
echo "$HOME_RES" | python3 -c "
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
echo "$PLANS_RES" | python3 -c "
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
