package com.ssafy.amagetdone.domain.transaction.infra.client;

import java.util.List;

import com.ssafy.amagetdone.domain.transaction.exception.TransactionErrorCode;
import com.ssafy.amagetdone.domain.transaction.exception.TransactionException;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.MerchantInferenceDto;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MerchantInferenceApiClient {

	private static final String BATCH_PATH = "/merchant/batch";

	private final RestClient restClient;
	private final MerchantInferenceApiProperties properties;

	public MerchantInferenceApiClient(
		RestClient.Builder restClientBuilder,
		MerchantInferenceApiProperties properties
	) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	public MerchantInferenceDto.Response labelMerchants(List<String> merchantTexts) {
		validateConfiguration();
		validateRequest(merchantTexts);

		MerchantInferenceDto.Response response;
		try {
			response = restClient.post()
				.uri(properties.getBaseUrl() + BATCH_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.body(new MerchantInferenceDto.Request(merchantTexts))
				.retrieve()
				.body(MerchantInferenceDto.Response.class);
		} catch (RestClientException exception) {
			throw new TransactionException(TransactionErrorCode.MERCHANT_INFERENCE_API_CALL_FAILED, exception);
		}

		if (response == null || response.results() == null) {
			throw new TransactionException(TransactionErrorCode.INVALID_MERCHANT_INFERENCE_API_RESPONSE);
		}

		return response;
	}

	private void validateConfiguration() {
		if (isBlank(properties.getBaseUrl())) {
			throw new TransactionException(TransactionErrorCode.MERCHANT_INFERENCE_API_CALL_FAILED);
		}
	}

	private void validateRequest(List<String> merchantTexts) {
		if (merchantTexts == null || merchantTexts.isEmpty()) {
			throw new TransactionException(TransactionErrorCode.MERCHANT_INFERENCE_API_CALL_FAILED);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
