### Embedded Content 1

 

[Skip to content](https://project.ssafy.com/docs/ssafy-finance/api-key#21-api-key)

# 앱 관리자(개발자) API KEY 발급

### 2.1 앱 관리자(개발자) API KEY 발급[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#21-api-key "Permanent link")

---

#### 2.1.1 앱 API KEY 발급[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#211-api-key "Permanent link")

##### 설명[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#_1 "Permanent link")

OPEN API를 사용하기 전 API KEY를 발급 받는 API 입니다.  
발급받은 API 키를 통해 관리자(개발자)는 앱을 생성합니다.

##### 요청 메시지 URL[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#url "Permanent link")

| **HTTP URL** | **HTTP Method** |
| --- | --- |
| `https://finopenapi.ssafy.io/ssafy/api/v1/edu/app/issuedApiKey` | POST |

##### 요청 메시지 명세[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#_2 "Permanent link")

| 변수명 | 설명 | TYPE | 길이 | 필수 | 비고 |
| --- | --- | --- | --- | --- | --- |
| managerId | 관리자 ID | String | 30 | Y | 이메일 형식 |

##### 요청 메시지 형태[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#_3 "Permanent link")

`[](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-0-1){     [](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-0-2)    "managerId": "ssafy@ssafy.co.kr" [](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-0-3)}`

##### 응답 메시지 명세[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#_4 "Permanent link")

| 변수명 | 설명 | TYPE | 길이 | 필수 | 비고 |
| --- | --- | --- | --- | --- | --- |
| managerId | 관리자 ID | String | 30 | Y | 이메일 형식 |
| apiKey | API 키 | String | 40 | Y | UUID 40 |
| creationDate | 생성일 | String | 8 | Y | 계정 생성일 |
| expirationDate | 만료일 | String | 8 | Y | 계정 만료일 |

##### 응답 메시지 형태[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#_5 "Permanent link")

`[](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-1-1){     [](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-1-2)    "managerId": "ssafy@ssafy.co.kr",    [](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-1-3)    "apiKey": "84711cec18cf4d52b2dbfbf03f20f17b",    [](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-1-4)    "creationDate": "20240415",    [](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-1-5)    "expirationDate": "20250415" [](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-1-6)}`

##### 에러코드 목록[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#_6 "Permanent link")

| **코드** | **설명** | **비고** |
| --- | --- | --- |
| E3000 | 이미 존재하는 관리자 ID입니다. |  |
| E3002 | 관리자 ID가 유효하지 않습니다. |  |
| A1080 | 등록되지 않은 관리자 이메일입니다. |  |
| Q1000 | 그 이외에 에러 메시지 |  |
| Q1001 | 요청 본문의 형식이 잘못되었습니다. JSON 형식 또는 데이터 타입을 확인해 주세요. |  |

---

#### 2.1.2 앱 API KEY 재발급[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#212-api-key "Permanent link")

##### 설명[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#_7 "Permanent link")

현재 사용 중인 API KEY를 확인하거나, 필요한 경우 새로운 API KEY를 생성할 수 있습니다.  
재발급 이후 기존 API KEY는 더 이상 사용되지 않습니다.

##### 요청 메시지 URL[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#url_1 "Permanent link")

| **HTTP URL** | **HTTP Method** |
| --- | --- |
| `https://finopenapi.ssafy.io/ssafy/api/v1/edu/app/reIssuedApiKey` | POST |

##### 요청 메시지 명세[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#_8 "Permanent link")

| 변수명 | 설명 | TYPE | 길이 | 필수 | 비고 |
| --- | --- | --- | --- | --- | --- |
| managerId | 관리자 ID | String | 30 | Y | 이메일 형식 |

##### 요청 메시지 형태[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#_9 "Permanent link")

`[](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-2-1){     [](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-2-2)    "managerId": "ssafy@ssafy.co.kr" [](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-2-3)}`

##### 응답 메시지 명세[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#_10 "Permanent link")

| 변수명 | 설명 | TYPE | 길이 | 필수 | 비고 |
| --- | --- | --- | --- | --- | --- |
| managerId | 관리자 ID | String | 30 | Y | 이메일 형식 |
| apiKey | API 키 | String | 40 | Y | UUID 40 |

##### 응답 메시지 형태[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#_11 "Permanent link")

`[](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-3-1){     [](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-3-2)    "managerId": "ssafy@ssafy.co.kr",    [](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-3-3)    "apiKey": "8644e48ee75740469ef8b5214499e5f7" [](https://project.ssafy.com/docs/ssafy-finance/api-key#__codelineno-3-4)}`

##### 에러코드 목록[¶](https://project.ssafy.com/docs/ssafy-finance/api-key#_12 "Permanent link")

| 코드 | 설명 | 비고 |
| --- | --- | --- |
| E3001 | 존재하지 않는 관리자 ID입니다. |  |
| E3002 | 관리자 ID가 유효하지 않습니다. |  |
| E3003 | 현재 사용중인 API KEY가 만료되어 재발급이 불가합니다. |  |
| Q1000 | 그 이외에 에러 메시지 |  |
| Q1001 | 요청 본문의 형식이 잘못되었습니다. JSON 형식 또는 데이터 타입을 확인해 주세요. |  |

---

Back to top

---
Source: [SSAFY_WebDRM[cH3]](https://project.ssafy.com/docs/ssafy-finance/api-key)