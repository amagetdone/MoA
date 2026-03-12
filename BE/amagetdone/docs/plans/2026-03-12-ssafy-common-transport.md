# SSAFY Common Transport Refactor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** SSAFY 공통 요청/응답 transport를 `global.infra.ssafy`로 분리하고, `asset`/`transaction` 도메인 client가 공통 transport만 참조하도록 리팩터링한다.

**Architecture:** `global.infra.ssafy`는 Header, 제네릭 request/response, request factory, response validator만 가진다. 각 도메인 client는 endpoint, payload 타입, domain result 매핑, domain 예외 변환만 유지한다. JSON 평탄화는 envelope 계층에서만 처리하고 내부 코드는 `header + payload` 구조를 사용한다.

**Tech Stack:** Java 21, Spring Boot, RestClient, Jackson, JUnit 5, MockRestServiceServer, Gradle

---

### Task 1: Add Global SSAFY Transport Tests

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyApiRequestFactoryTest.java`
- Create: `src/test/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyApiResponseValidatorTest.java`
- Create: `src/test/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyEnvelopeSerializationTest.java`

**Step 1: Write the failing request factory test**

```java
@Test
void createWithoutUserKeyBuildsCommonHeader() {
	SsafyApiRequestFactory factory = new SsafyApiRequestFactory("00100", "001", "test-api-key");

	SsafyRequestEnvelope<TestPayload> request = factory.create(
		"inquireMerchantList",
		null,
		new TestPayload("A")
	);

	assertThat(request.header().apiName()).isEqualTo("inquireMerchantList");
	assertThat(request.header().userKey()).isNull();
	assertThat(request.payload().value()).isEqualTo("A");
}
```

**Step 2: Write the failing response validator test**

```java
@Test
void validateThrowsWhenResponseCodeIsNotSuccess() {
	SsafyApiResponseValidator validator = new SsafyApiResponseValidator();

	assertThatThrownBy(() -> validator.validate(responseWithCode("H1000")))
		.isInstanceOf(SsafyApiException.class);
}
```

**Step 3: Write the failing envelope serialization test**

```java
@Test
void requestEnvelopeSerializesHeaderAndPayloadAsFlatJson() throws Exception {
	String json = objectMapper.writeValueAsString(envelope);

	assertThat(json).contains("\"Header\"");
	assertThat(json).contains("\"cardNo\"");
}
```

**Step 4: Run tests to verify they fail**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiRequestFactoryTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiResponseValidatorTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyEnvelopeSerializationTest
```

Expected: FAIL because `global.infra.ssafy` classes do not exist yet.

**Step 5: Commit**

```bash
git add src/test/java/com/ssafy/amagetdone/global/infra/ssafy
git commit -m "test: add ssafy common transport tests"
```

### Task 2: Implement Global SSAFY Transport Layer

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyRequestHeader.java`
- Create: `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyResponseHeader.java`
- Create: `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyRequest.java`
- Create: `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyResponse.java`
- Create: `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyRequestEnvelope.java`
- Create: `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyResponseEnvelope.java`
- Create: `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyApiException.java`
- Create: `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyApiRequestFactory.java`
- Create: `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyApiResponseValidator.java`

**Step 1: Implement the minimal records and exception**

```java
public record SsafyRequest<T>(SsafyRequestHeader header, T payload) {}

public record SsafyResponse<T>(SsafyResponseHeader header, T payload) {}
```

```java
public class SsafyApiException extends RuntimeException {
	private final Reason reason;

	public enum Reason {
		INVALID_CONFIGURATION,
		CALL_FAILED,
		INVALID_HEADER,
		FAILED_RESPONSE_CODE,
		INVALID_PAYLOAD
	}
}
```

**Step 2: Implement request factory**

```java
public <T> SsafyRequestEnvelope<T> create(String apiName, String userKey, T payload) {
	LocalDateTime now = LocalDateTime.now();
	SsafyRequestHeader header = new SsafyRequestHeader(
		apiName,
		now.format(DATE_FORMAT),
		now.format(TIME_FORMAT),
		institutionCode,
		fintechAppNo,
		apiName,
		createUniqueNo(now),
		apiKey,
		userKey
	);
	return SsafyRequestEnvelope.from(new SsafyRequest<>(header, payload));
}
```

**Step 3: Implement response validator**

```java
public <T> SsafyResponse<T> validate(SsafyResponseEnvelope<T> envelope) {
	if (envelope == null || envelope.header() == null) {
		throw new SsafyApiException(Reason.INVALID_HEADER);
	}
	if (!"H0000".equals(envelope.header().responseCode())) {
		throw new SsafyApiException(Reason.FAILED_RESPONSE_CODE);
	}
	if (envelope.payload() == null) {
		throw new SsafyApiException(Reason.INVALID_PAYLOAD);
	}
	return envelope.toResponse();
}
```

**Step 4: Run global transport tests**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiRequestFactoryTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiResponseValidatorTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyEnvelopeSerializationTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/global/infra/ssafy src/test/java/com/ssafy/amagetdone/global/infra/ssafy
git commit -m "feat: add ssafy common transport layer"
```

### Task 3: Refactor Asset Merchant Client To Use Global Transport

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantApiClient.java`
- Modify or rename: `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantRequest.java`
- Modify or rename: `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantResponse.java`
- Modify: `src/test/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantApiClientTest.java`
- Delete: `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/AssetSsafyRequestHeader.java`
- Delete: `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/AssetSsafyResponseHeader.java`

**Step 1: Update merchant client test to assert global Header structure is still sent**

```java
server.expect(requestTo(baseUrl + "/creditCard/inquireMerchantList"))
	.andExpect(jsonPath("$.Header.apiName").value("inquireMerchantList"))
	.andExpect(jsonPath("$.Header.userKey").doesNotExist())
	.andRespond(withSuccess(successJson, MediaType.APPLICATION_JSON));
```

**Step 2: Run merchant test to verify it fails**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.domain.asset.infra.client.SsafyMerchantApiClientTest
```

Expected: FAIL because the client still uses domain-local Header types.

**Step 3: Implement minimal client refactor**

```java
SsafyRequestEnvelope<SsafyMerchantRequestPayload> request = requestFactory.create(API_NAME, null, new SsafyMerchantRequestPayload());
SsafyResponse<SsafyMerchantResponsePayload> response = responseValidator.validate(
	restClient.post()
		.uri(properties.getBaseUrl() + "/creditCard/" + API_NAME)
		.contentType(MediaType.APPLICATION_JSON)
		.body(request)
		.retrieve()
		.body(merchantResponseType)
);
```

**Step 4: Run merchant test to verify it passes**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.domain.asset.infra.client.SsafyMerchantApiClientTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/asset/infra/client src/test/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantApiClientTest.java
git commit -m "refactor: move merchant client to ssafy common transport"
```

### Task 4: Refactor Card Transaction Client To Use Global Transport

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionApiClient.java`
- Modify or rename: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionRequest.java`
- Modify or rename: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionResponse.java`
- Modify: `src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionApiClientTest.java`
- Delete: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/TransactionSsafyRequestHeader.java`
- Delete: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/TransactionSsafyResponseHeader.java`

**Step 1: Update card transaction test for global transport usage**

```java
server.expect(requestTo(baseUrl + "/creditCard/inquireCreditCardTransactionList"))
	.andExpect(jsonPath("$.Header.userKey").value("user-key"))
	.andExpect(jsonPath("$.cardNo").value("1234567890123456"))
	.andRespond(withSuccess(successJson, MediaType.APPLICATION_JSON));
```

**Step 2: Run card transaction test to verify it fails**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClientTest
```

Expected: FAIL until the client uses the global request factory and response validator.

**Step 3: Implement minimal client refactor**

```java
SsafyRequestEnvelope<SsafyCardTransactionRequestPayload> request = requestFactory.create(
	API_NAME,
	input.userKey(),
	new SsafyCardTransactionRequestPayload(input.cardNo(), input.cvc(), input.startDate(), input.endDate())
);
```

Catch `SsafyApiException` and map it to `TransactionException`.

**Step 4: Run card transaction test to verify it passes**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClientTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionApiClientTest.java
git commit -m "refactor: move card transaction client to ssafy common transport"
```

### Task 5: Refactor Demand Deposit Transaction Client To Use Global Transport

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionApiClient.java`
- Modify or rename: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionRequest.java`
- Modify or rename: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionResponse.java`
- Modify: `src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionApiClientTest.java`

**Step 1: Update demand deposit test for global transport usage**

```java
server.expect(requestTo(baseUrl + "/demandDeposit/inquireTransactionHistoryList"))
	.andExpect(jsonPath("$.Header.userKey").value("user-key"))
	.andExpect(jsonPath("$.accountNo").value("123-456-789"))
	.andRespond(withSuccess(successJson, MediaType.APPLICATION_JSON));
```

**Step 2: Run demand deposit test to verify it fails**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClientTest
```

Expected: FAIL until the client uses the global transport layer.

**Step 3: Implement minimal client refactor**

```java
SsafyRequestEnvelope<SsafyDemandDepositTransactionRequestPayload> request = requestFactory.create(
	API_NAME,
	input.userKey(),
	new SsafyDemandDepositTransactionRequestPayload(
		input.accountNo(),
		input.startDate(),
		input.endDate(),
		input.transactionType(),
		input.orderByType()
	)
);
```

**Step 4: Run demand deposit test to verify it passes**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClientTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionApiClientTest.java
git commit -m "refactor: move demand deposit client to ssafy common transport"
```

### Task 6: Clean Up Obsolete Types And Run Regression Suite

**Files:**
- Delete: `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/AssetSsafyRequestHeader.java`
- Delete: `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/AssetSsafyResponseHeader.java`
- Delete: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/TransactionSsafyRequestHeader.java`
- Delete: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/TransactionSsafyResponseHeader.java`
- Modify: `src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyInfraClientStructureTest.java`

**Step 1: Update structure test to enforce new package boundary**

```java
assertThatClass("com.ssafy.amagetdone.global.infra.ssafy.SsafyRequestHeader").exists();
assertThatClass("com.ssafy.amagetdone.domain.asset.infra.client.AssetSsafyRequestHeader").doesNotExist();
```

**Step 2: Run structure test and all affected client tests**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiRequestFactoryTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiResponseValidatorTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyEnvelopeSerializationTest --tests com.ssafy.amagetdone.domain.asset.infra.client.SsafyMerchantApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyInfraClientStructureTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyApiPropertiesConfigurationTest --tests com.ssafy.amagetdone.domain.asset.AssetEntityMappingTest --tests com.ssafy.amagetdone.domain.transaction.TransactionEntityMappingTest --tests com.ssafy.amagetdone.domain.transaction.FkRelationshipMappingTest
```

Expected: PASS

**Step 3: Run a clean regression build if incremental compile looks stale**

Run:

```bash
./gradlew.bat clean test --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiRequestFactoryTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiResponseValidatorTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyEnvelopeSerializationTest --tests com.ssafy.amagetdone.domain.asset.infra.client.SsafyMerchantApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyInfraClientStructureTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyApiPropertiesConfigurationTest
```

Expected: PASS

**Step 4: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/global/infra/ssafy src/main/java/com/ssafy/amagetdone/domain/asset/infra/client src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client src/test/java/com/ssafy/amagetdone/global/infra/ssafy src/test/java/com/ssafy/amagetdone/domain/asset/infra/client src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client
git commit -m "refactor: consolidate ssafy transport models"
```
