package com.ssafy.amagetdone.domain.transaction.infra.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SsafyInfraClientStructureTest {

	@Test
	void merchantClientDoesNotContainEmbeddedRecordDeclarations() throws IOException {
		String source = read("src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantApiClient.java");

		assertFalse(source.contains("record MerchantListResult("));
		assertFalse(source.contains("record MerchantItem("));
		assertFalse(source.contains("SsafyMerchantRequest"));
		assertFalse(source.contains("SsafyMerchantRecord"));
		assertFalse(source.contains("new SsafyApiRequestFactory("));
		assertFalse(source.contains("new SsafyApiResponseValidator("));

		assertTrue(exists("src/main/java/com/ssafy/amagetdone/global/infra/ssafy/model/SsafyRequestHeader.java"));
		assertTrue(exists("src/main/java/com/ssafy/amagetdone/global/infra/ssafy/model/SsafyResponseHeader.java"));
		assertTrue(exists("src/main/java/com/ssafy/amagetdone/global/infra/ssafy/model/SsafyRequest.java"));
		assertTrue(exists("src/main/java/com/ssafy/amagetdone/global/infra/ssafy/model/SsafyResponse.java"));
		assertTrue(exists("src/main/java/com/ssafy/amagetdone/global/infra/ssafy/envelope/SsafyRequestEnvelope.java"));
		assertTrue(exists("src/main/java/com/ssafy/amagetdone/global/infra/ssafy/envelope/SsafyResponseEnvelope.java"));
		assertTrue(exists("src/main/java/com/ssafy/amagetdone/global/infra/ssafy/support/SsafyApiRequestFactory.java"));
		assertTrue(exists("src/main/java/com/ssafy/amagetdone/global/infra/ssafy/support/SsafyApiResponseValidator.java"));
		assertTrue(exists("src/main/java/com/ssafy/amagetdone/global/infra/ssafy/exception/SsafyApiException.java"));
		assertTrue(exists("src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/MerchantListResult.java"));
		assertTrue(exists("src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/MerchantItem.java"));
		assertTrue(exists("src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/dto/MerchantDto.java"));
		assertFalse(exists("src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantRequest.java"));
		assertFalse(exists("src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantRecord.java"));
	}

	@Test
	void transactionClientsDoNotContainEmbeddedRecordDeclarations() throws IOException {
		String cardClient = read(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionApiClient.java");
		String demandDepositClient = read(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionApiClient.java");

		assertFalse(cardClient.contains("CardTransactionListRequest"));
		assertFalse(cardClient.contains("CardTransactionListResult"));
		assertFalse(cardClient.contains("CardTransactionItem"));
		assertFalse(cardClient.contains("SsafyCardTransactionRequest"));
		assertFalse(cardClient.contains("SsafyCardTransactionRecord"));
		assertFalse(cardClient.contains("SsafyCardTransactionItemRecord"));
		assertFalse(cardClient.contains("new SsafyApiRequestFactory("));
		assertFalse(cardClient.contains("new SsafyApiResponseValidator("));

		assertFalse(demandDepositClient.contains("DemandDepositTransactionListRequest"));
		assertFalse(demandDepositClient.contains("DemandDepositTransactionListResult"));
		assertFalse(demandDepositClient.contains("DemandDepositTransactionItem"));
		assertFalse(demandDepositClient.contains("SsafyDemandDepositTransactionRequest"));
		assertFalse(demandDepositClient.contains("SsafyDemandDepositTransactionRecord"));
		assertFalse(demandDepositClient.contains("SsafyDemandDepositTransactionItemRecord"));
		assertFalse(demandDepositClient.contains("new SsafyApiRequestFactory("));
		assertFalse(demandDepositClient.contains("new SsafyApiResponseValidator("));

		assertTrue(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/dto/CardTransactionDto.java"));
		assertFalse(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/CardTransactionListRequest.java"));
		assertFalse(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/CardTransactionListResult.java"));
		assertFalse(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/CardTransactionItem.java"));
		assertFalse(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionRequest.java"));
		assertFalse(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionRecord.java"));
		assertFalse(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionItemRecord.java"));
		assertTrue(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/dto/DemandDepositTransactionDto.java"));
		assertFalse(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/DemandDepositTransactionListRequest.java"));
		assertFalse(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/DemandDepositTransactionListResult.java"));
		assertFalse(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/DemandDepositTransactionItem.java"));
		assertFalse(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionRequest.java"));
		assertFalse(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionRecord.java"));
		assertFalse(exists(
			"src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionItemRecord.java"));
	}

	private String read(String path) throws IOException {
		return Files.readString(Path.of(path));
	}

	private boolean exists(String path) {
		return Files.exists(Path.of(path));
	}
}
