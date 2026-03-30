#!/usr/bin/env bash
# ==============================================================================
# AmagetDone 통합 테스트 플로우
#
# Usage: ./test_flow.sh [--reset] <test_user_id>
#   --reset: 테스트 사용자 데이터 초기화 후 실행
#   test_user_id: 1~5
#
# Phase 0: 계정계 사전 준비 (사용자/계좌/카드 확인 및 생성)
# Phase 1: 인증 & 프로필 & 금융 자산 동기화
# Phase 2: 거래내역 초기화 (우리 서버 Admin API)
# Phase 2.5: 계정계 월급 입금
# Phase 3: 거래 분석
# Phase 4: 예산 설정
# Phase 5: 목표 생성 & Backfill & 검증
# ==============================================================================
set -euo pipefail
export TZ="Asia/Seoul"

# --------------- 설정 ---------------
BASE="${BASE:-https://j14d101.p.ssafy.io/api}"
FIN_BASE="${FIN_BASE:-https://finopenapi.ssafy.io/ssafy/api/v1}"
FIN_API_KEY="${FIN_API_KEY:-3eba20d5b7a4430bb53c505cce0c4f18}"
TEST_EMAIL_PREFIX="${TEST_EMAIL_PREFIX:-moa}"
TEST_EMAIL_DOMAIN="${TEST_EMAIL_DOMAIN:-tester.com}"

# --------------- 인자 검증 ---------------
DO_RESET=false
if [[ "${1:-}" == "--reset" ]]; then
  DO_RESET=true
  shift
fi

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 [--reset] <test_user_id (1-5)>"
  exit 1
fi

USER_ID="$1"
if ! [[ "$USER_ID" =~ ^[1-5]$ ]]; then
  echo "Error: test_user_id must be 1-5"
  exit 1
fi

TEST_EMAIL="${TEST_EMAIL_PREFIX}${USER_ID}@${TEST_EMAIL_DOMAIN}"

# --------------- 유틸 함수 ---------------
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

# 계정계 Header용 고유번호 생성 (20자리)
gen_unique_no() {
  date +"%Y%m%d%H%M%S"$(printf "%06d" $((RANDOM % 1000000)))
}

# 계정계 공통 Header JSON 생성
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

# JSON 필드 추출 (jq 없을 때 python3 fallback)
json_get() {
  local json="$1"
  local field="$2"
  if command -v jq &>/dev/null; then
    echo "$json" | jq -r "$field"
  else
    echo "$json" | python3 -c "import sys,json; data=json.load(sys.stdin); print(eval('data' + '$field'.replace('.','[\\'').replace('[','[\"').replace(']','\"]') if '[' not in '$field' else '$field'))" 2>/dev/null || echo ""
  fi
}

# 간이 jq 래퍼
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
# .foo.bar[0].baz 형식 파싱
tokens = re.findall(r'\w+|\[\d+\]', path)
cur = data
for t in tokens:
    if t.startswith('[') and t.endswith(']'):
        cur = cur[int(t[1:-1])]
    else:
        cur = cur[t]
if cur is None:
    print('null')
else:
    print(cur)
" 2>/dev/null || echo ""
  fi
}

# ==============================================================================
# Phase 0: 계정계 사전 준비
# ==============================================================================
echo "=========================================="
echo " AmagetDone Test Flow - User ID: $USER_ID"
echo "=========================================="

# 0-1. 사용자 검색
step "계정계 사용자 검색"
MEMBER_RES=$(curl -sk -X POST "$FIN_BASE/member/search" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": \"$TEST_EMAIL\", \"apiKey\": \"$FIN_API_KEY\"}")

USER_KEY=$(jq_r "$MEMBER_RES" ".userKey" 2>/dev/null || echo "")

if [[ -z "$USER_KEY" || "$USER_KEY" == "null" ]]; then
  # 사용자 없음 → 생성
  step "계정계 사용자 생성"
  CREATE_MEMBER_RES=$(curl -sk -X POST "$FIN_BASE/member/" \
    -H "Content-Type: application/json" \
    -d "{\"apiKey\": \"$FIN_API_KEY\", \"userId\": \"$TEST_EMAIL\"}")
  USER_KEY=$(jq_r "$CREATE_MEMBER_RES" ".userKey")
  if [[ -z "$USER_KEY" || "$USER_KEY" == "null" ]]; then
    fail "사용자 생성 실패" "$CREATE_MEMBER_RES"
  fi
  echo "  -> 사용자 생성 완료: userKey=$USER_KEY"
else
  echo "  -> 사용자 존재: userKey=$USER_KEY"
fi

# 0-3. 계좌 목록 조회
step "계정계 계좌 확인"
sleep 1
ACCOUNT_LIST_RES=$(curl -sk -X POST "$FIN_BASE/edu/demandDeposit/inquireDemandDepositAccountList" \
  -H "Content-Type: application/json" \
  -d "{\"Header\": $(fin_header inquireDemandDepositAccountList "$USER_KEY")}")

ACCOUNT_NO=$(jq_r "$ACCOUNT_LIST_RES" '.REC[0].accountNo' 2>/dev/null || echo "")

if [[ -z "$ACCOUNT_NO" || "$ACCOUNT_NO" == "null" ]]; then
  # 계좌 없음 → 상품 조회 후 생성
  step "수시입출금 상품 조회"
  sleep 1
  PRODUCT_RES=$(curl -sk -X POST "$FIN_BASE/edu/demandDeposit/inquireDemandDepositList" \
    -H "Content-Type: application/json" \
    -d "{\"Header\": $(fin_header inquireDemandDepositList)}")

  # "입출금상품커피" 상품 선택
  ACCOUNT_TYPE_NO=$(echo "$PRODUCT_RES" | python3 -c "
import sys, json
data = json.load(sys.stdin)
recs = data.get('REC', [])
for r in recs:
    if '입출금상품커피' in r.get('accountName', '') and r.get('bankCode') == '004':
        print(r['accountTypeUniqueNo'])
        sys.exit(0)
# fallback: 첫 번째 상품
if recs:
    print(recs[0]['accountTypeUniqueNo'])
")

  if [[ -z "$ACCOUNT_TYPE_NO" ]]; then
    fail "입출금 상품을 찾을 수 없음" "$PRODUCT_RES"
  fi

  step "계좌 생성"
  sleep 1
  CREATE_ACCOUNT_RES=$(curl -sk -X POST "$FIN_BASE/edu/demandDeposit/createDemandDepositAccount" \
    -H "Content-Type: application/json" \
    -d "{\"Header\": $(fin_header createDemandDepositAccount "$USER_KEY"), \"accountTypeUniqueNo\": \"$ACCOUNT_TYPE_NO\"}")
  ACCOUNT_NO=$(jq_r "$CREATE_ACCOUNT_RES" '.REC.accountNo')
  if [[ -z "$ACCOUNT_NO" || "$ACCOUNT_NO" == "null" ]]; then
    fail "계좌 생성 실패" "$CREATE_ACCOUNT_RES"
  fi
  echo "  -> 계좌 생성 완료: $ACCOUNT_NO"
else
  echo "  -> 계좌 존재: $ACCOUNT_NO"
fi

# 0-5. 카드 목록 조회
step "계정계 카드 확인"
sleep 1
CARD_LIST_RES=$(curl -sk -X POST "$FIN_BASE/edu/creditCard/inquireSignUpCreditCardList" \
  -H "Content-Type: application/json" \
  -d "{\"Header\": $(fin_header inquireSignUpCreditCardList "$USER_KEY")}")

CARD_NO=$(jq_r "$CARD_LIST_RES" '.REC[0].cardNo' 2>/dev/null || echo "")

if [[ -z "$CARD_NO" || "$CARD_NO" == "null" ]]; then
  # 카드 없음 → 상품 조회 후 발급
  step "카드 상품 조회"
  sleep 1
  CARD_PRODUCT_RES=$(curl -sk -X POST "$FIN_BASE/edu/creditCard/inquireCreditCardList" \
    -H "Content-Type: application/json" \
    -d "{\"Header\": $(fin_header inquireCreditCardList)}")

  CARD_UNIQUE_NO=$(jq_r "$CARD_PRODUCT_RES" '.REC[0].cardUniqueNo')
  if [[ -z "$CARD_UNIQUE_NO" || "$CARD_UNIQUE_NO" == "null" ]]; then
    fail "카드 상품을 찾을 수 없음" "$CARD_PRODUCT_RES"
  fi

  step "카드 발급"
  sleep 1
  CREATE_CARD_RES=$(curl -sk -X POST "$FIN_BASE/edu/creditCard/createCreditCard" \
    -H "Content-Type: application/json" \
    -d "{\"Header\": $(fin_header createCreditCard "$USER_KEY"), \"cardUniqueNo\": \"$CARD_UNIQUE_NO\", \"withdrawalAccountNo\": \"$ACCOUNT_NO\", \"withdrawalDate\": \"4\"}")
  CARD_NO=$(jq_r "$CREATE_CARD_RES" '.REC.cardNo')
  if [[ -z "$CARD_NO" || "$CARD_NO" == "null" ]]; then
    fail "카드 발급 실패" "$CREATE_CARD_RES"
  fi
  echo "  -> 카드 발급 완료: $CARD_NO"
else
  echo "  -> 카드 존재: $CARD_NO"
fi

echo ""
echo "======= Phase 0 완료: 계정계 준비 완료 ======="

# ==============================================================================
# Phase 1: 인증 & 프로필
# ==============================================================================

# 1-1. 테스트 로그인
step "테스트 소셜 로그인 (id=$USER_ID)"
LOGIN_RES=$(curl -sk -X POST "$BASE/v1/auth/social/test" \
  -H "Content-Type: application/json" \
  -d "{\"id\": $USER_ID}")

TOKEN=$(jq_r "$LOGIN_RES" '.data.accessToken')
LOGIN_STATUS=$(jq_r "$LOGIN_RES" '.data.status')
if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  fail "로그인 실패" "$LOGIN_RES"
fi
echo "  -> 로그인 성공 (status=$LOGIN_STATUS)"

AUTH="Authorization: Bearer $TOKEN"

# 리셋 (옵션)
if [[ "$DO_RESET" == "true" ]]; then
  step "테스트 사용자 리셋"
  RESET_RES=$(curl -sk -X POST "$BASE/v1/auth/social/test/reset" \
    -H "$AUTH" -H "Content-Type: application/json")
  echo "  -> $(jq_r "$RESET_RES" '.message')"
  # 리셋 후 토큰 재발급
  LOGIN_RES=$(curl -sk -X POST "$BASE/v1/auth/social/test" \
    -H "Content-Type: application/json" \
    -d "{\"id\": $USER_ID}")
  TOKEN=$(jq_r "$LOGIN_RES" '.data.accessToken')
  AUTH="Authorization: Bearer $TOKEN"
fi

# 1-2. 프로필 완성
step "프로필 완성"
PROFILE_RES=$(curl -sk -X PATCH "$BASE/v1/members/me/profile" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"nickname\": \"테스트유저${USER_ID}\", \"email\": \"$TEST_EMAIL\", \"birthDate\": \"1995-06-15\", \"hasFixedIncome\": true}")
PROFILE_STATUS=$(jq_r "$PROFILE_RES" '.data.status')
echo "  -> 프로필 상태: $PROFILE_STATUS"

# 1-3. 금융 자산 자동 동기화
step "금융 자산 자동 동기화"
SYNC_ASSET_RES=$(curl -sk -X POST "$BASE/v1/auth/social/test/sync-assets" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"id\": $USER_ID}")
SYNC_STATUS=$(jq_r "$SYNC_ASSET_RES" '.status')
if [[ "$SYNC_STATUS" != "ok" ]]; then
  fail "자산 동기화 실패" "$SYNC_ASSET_RES"
fi
echo "  -> 자산 동기화 성공"

# ==============================================================================
# Phase 2: 거래내역 초기화 (우리 서버 Admin API)
# ==============================================================================

# 2-1. 계좌 거래내역 초기화
step "계좌 거래내역 초기화 (admin/data-init/account)"
INIT_ACCT_RES=$(curl -sk -X POST "$BASE/admin/data-init/account" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"accountNo\": \"$ACCOUNT_NO\"}")
INIT_ACCT_COUNT=$(jq_r "$INIT_ACCT_RES" '.data.transactionCount')
echo "  -> 계좌 거래내역 ${INIT_ACCT_COUNT}건 초기화"

# 2-2. 카드 거래내역 초기화
step "카드 거래내역 초기화 (admin/data-init/card)"
INIT_CARD_RES=$(curl -sk -X POST "$BASE/admin/data-init/card" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"cardNo\": \"$CARD_NO\"}")
INIT_CARD_COUNT=$(jq_r "$INIT_CARD_RES" '.data.transactionCount')
echo "  -> 카드 거래내역 ${INIT_CARD_COUNT}건 초기화"

# ==============================================================================
# Phase 2.5: 계정계 월급 입금 (시드 데이터 수준: 월 3,500,000원)
# ==============================================================================

step "계정계 월급 입금 확인 (3,500,000원)"
sleep 1
# 거래내역 조회로 이미 월급 입금이 있는지 확인
TODAY_STR=$(date +"%Y%m%d")
YEAR_AGO_STR=$(date -d "-365 days" +"%Y%m%d" 2>/dev/null || date -v-365d +"%Y%m%d" 2>/dev/null || echo "$TODAY_STR")
TX_HISTORY_RES=$(curl -sk -X POST "$FIN_BASE/edu/demandDeposit/inquireTransactionHistoryList" \
  -H "Content-Type: application/json" \
  -d "{\"Header\": $(fin_header inquireTransactionHistoryList "$USER_KEY"), \"accountNo\": \"$ACCOUNT_NO\", \"startDate\": \"$YEAR_AGO_STR\", \"endDate\": \"$TODAY_STR\", \"transactionType\": \"A\", \"orderByType\": \"DESC\"}")

SALARY_EXISTS=$(printf '%s' "$TX_HISTORY_RES" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    recs = data.get('REC', {}).get('list', data.get('REC', []))
    found = False
    if isinstance(recs, list):
        for r in recs:
            type_name = (r.get('transactionTypeName', '') or '').strip()
            summary = (r.get('transactionSummary', '') or '').strip()
            memo = (r.get('transactionMemo', '') or '').strip()
            text = f'{summary} {memo}'.casefold()
            if '입금' in type_name and any(keyword in text for keyword in ('salary', '급여', '월급')):
                found = True
                break
    print('YES' if found else 'NO')
except:
    print('NO')
" 2>/dev/null || echo "NO")

if [[ "$SALARY_EXISTS" == "YES" ]]; then
  echo "  -> 이미 월급 입금 내역이 존재합니다. 월급 입금 플로우를 건너뜁니다."
else
  sleep 1
  DEPOSIT_RES=$(curl -sk -X POST "$FIN_BASE/edu/demandDeposit/updateDemandDepositAccountDeposit" \
    -H "Content-Type: application/json" \
    -d "{\"Header\": $(fin_header updateDemandDepositAccountDeposit "$USER_KEY"), \"accountNo\": \"$ACCOUNT_NO\", \"transactionBalance\": 3500000, \"transactionSummary\": \"SSAFY 급여\"}")
  DEPOSIT_STATUS=$(jq_r "$DEPOSIT_RES" '.Header.responseCode' 2>/dev/null || echo "")
  DEPOSIT_TX_NO=$(jq_r "$DEPOSIT_RES" '.REC.transactionUniqueNo' 2>/dev/null || echo "")
  if [[ "$DEPOSIT_STATUS" != "H0000" || -z "$DEPOSIT_TX_NO" || "$DEPOSIT_TX_NO" == "null" ]]; then
    echo "  -> 입금 실패 (응답: $DEPOSIT_RES)"
  else
    sleep 1
    BALANCE_RES=$(curl -sk -X POST "$FIN_BASE/edu/demandDeposit/inquireDemandDepositAccountBalance" \
      -H "Content-Type: application/json" \
      -d "{\"Header\": $(fin_header inquireDemandDepositAccountBalance "$USER_KEY"), \"accountNo\": \"$ACCOUNT_NO\"}")
    DEPOSIT_BALANCE=$(jq_r "$BALANCE_RES" '.REC.accountBalance' 2>/dev/null || echo "")
    if [[ -z "$DEPOSIT_BALANCE" || "$DEPOSIT_BALANCE" == "null" ]]; then
      echo "  -> 입금 완료 (transactionUniqueNo=$DEPOSIT_TX_NO)"
    else
      echo "  -> 입금 완료, 잔액: ${DEPOSIT_BALANCE}원"
    fi
  fi
fi

# ==============================================================================
# Phase 3: 거래 분석
# ==============================================================================

step "거래 분석"
ANALYSIS_RES=$(curl -sk -X POST "$BASE/transactions/analysis" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"months": 6}')
ANALYSIS_MSG=$(jq_r "$ANALYSIS_RES" '.status')
echo "  -> 분석 상태: $ANALYSIS_MSG"

# ==============================================================================
# Phase 4: 예산 설정
# ==============================================================================

# 4-1. 수입 등록
step "월급 등록 (3,000,000원)"
INCOME_RES=$(curl -sk -X POST "$BASE/budgets/income" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"monthlyIncome": 3000000}')
INCOME_MONTH=$(jq_r "$INCOME_RES" '.data.baseMonth')
echo "  -> 기준월: $INCOME_MONTH"

# 4-2. 예산 배분
step "예산 배분"
ALLOCATE_RES=$(curl -sk -X POST "$BASE/budgets/allocate" \
  -H "$AUTH" -H "Content-Type: application/json")
ALLOC_SUCCESS=$(jq_r "$ALLOCATE_RES" '.data.success')
echo "  -> 배분 결과: $ALLOC_SUCCESS"

# ==============================================================================
# Phase 5: 목표 생성 & Backfill & 검증
# ==============================================================================

# 날짜 계산 (오늘 기준 3개월 전/후)
TODAY=$(date +%Y-%m-%d)
if date --version &>/dev/null 2>&1; then
  # GNU date
  START_A=$(date -d "$TODAY - 3 months" +%Y-%m-%d)
  END_A=$(date -d "$TODAY" +%Y-%m-%d)
  START_B=$(date -d "$TODAY - 2 months" +%Y-%m-%d)
  END_B=$(date -d "$TODAY + 1 month" +%Y-%m-%d)
  START_C=$(date -d "$TODAY - 1 month" +%Y-%m-%d)
  END_C=$(date -d "$TODAY + 2 months" +%Y-%m-%d)
else
  # macOS date
  START_A=$(date -v-3m +%Y-%m-%d)
  END_A=$(date +%Y-%m-%d)
  START_B=$(date -v-2m +%Y-%m-%d)
  END_B=$(date -v+1m +%Y-%m-%d)
  START_C=$(date -v-1m +%Y-%m-%d)
  END_C=$(date -v+2m +%Y-%m-%d)
fi

# 5-1. 목표 A
step "목표 A 생성: 맥북 프로 구매 (~100%)"
echo "  -> 기간: $START_A ~ $END_A"
PLAN_A_RES=$(curl -sk -X POST "$BASE/v1/plans" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"title\": \"맥북 프로 구매\", \"targetAmount\": 900000, \"startDate\": \"$START_A\", \"endDate\": \"$END_A\", \"priority\": 1}")
PLAN_A_ID=$(jq_r "$PLAN_A_RES" '.data.planId')
echo "  -> planId=$PLAN_A_ID"

# 5-2. 목표 B
step "목표 B 생성: 여행 자금 모으기 (~66%)"
echo "  -> 기간: $START_B ~ $END_B"
PLAN_B_RES=$(curl -sk -X POST "$BASE/v1/plans" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"title\": \"여행 자금 모으기\", \"targetAmount\": 600000, \"startDate\": \"$START_B\", \"endDate\": \"$END_B\", \"priority\": 2}")
PLAN_B_ID=$(jq_r "$PLAN_B_RES" '.data.planId')
echo "  -> planId=$PLAN_B_ID"

# 5-3. 목표 C
step "목표 C 생성: 자격증 교재 구매 (~33%)"
echo "  -> 기간: $START_C ~ $END_C"
PLAN_C_RES=$(curl -sk -X POST "$BASE/v1/plans" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"title\": \"자격증 교재 구매\", \"targetAmount\": 300000, \"startDate\": \"$START_C\", \"endDate\": \"$END_C\", \"priority\": 3}")
PLAN_C_ID=$(jq_r "$PLAN_C_RES" '.data.planId')
echo "  -> planId=$PLAN_C_ID"

# 5-4. Backfill
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

# 5-5. 홈 화면 확인
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

# 5-6. 목표 목록 확인
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
echo " Test Flow 완료!"
echo "=========================================="
