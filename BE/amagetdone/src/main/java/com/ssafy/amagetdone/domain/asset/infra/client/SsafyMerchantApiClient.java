package com.ssafy.amagetdone.domain.asset.infra.client;

import java.util.List;

import com.ssafy.amagetdone.domain.asset.exception.AssetErrorCode;
import com.ssafy.amagetdone.domain.asset.exception.AssetException;
import com.ssafy.amagetdone.domain.asset.infra.client.dto.MerchantDto;
import com.ssafy.amagetdone.global.infra.ssafy.envelope.SsafyResponseEnvelope;
import com.ssafy.amagetdone.global.infra.ssafy.exception.SsafyApiException;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiRequestFactory;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiResponseValidator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class SsafyMerchantApiClient {

	private static final String API_NAME = "inquireMerchantList";

	private final RestClient restClient;
	private final AssetSsafyApiProperties properties;
	private final SsafyApiRequestFactory requestFactory;
	private final SsafyApiResponseValidator responseValidator;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<MerchantDto.Response>> responseType;

	@Value("${asset.ssafy.api-key}")
	private String apiKey;

	public SsafyMerchantApiClient(
		RestClient.Builder restClientBuilder,
		AssetSsafyApiProperties properties,
		SsafyApiRequestFactory requestFactory,
		SsafyApiResponseValidator responseValidator
	) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
		this.requestFactory = requestFactory;
		this.responseValidator = responseValidator;
		this.responseType = new ParameterizedTypeReference<>() {
		};
	}

	public MerchantListResult inquireMerchantList() {
		validateConfiguration();

		List<MerchantDto.Record> records;
		try {
			records = responseValidator.validate(
				restClient.post()
				.uri(properties.getBaseUrl() + "/creditCard/" + API_NAME)
				.contentType(MediaType.APPLICATION_JSON)
				.body(requestFactory.create(
					API_NAME,
					properties.getInstitutionCode(),
					properties.getFintechAppNo(),
					apiKey,
					null,
					new MerchantDto.Request()
				))
				.retrieve()
				.body(responseType)
			).payload().records();
		} catch (SsafyApiException exception) {
			throw new AssetException(resolveErrorCode(exception), exception);
		} catch (RestClientException exception) {
			throw new AssetException(AssetErrorCode.MERCHANT_API_CALL_FAILED, exception);
		}

		return new MerchantListResult(records.stream()
			.map(record -> new MerchantItem(
				record.categoryId(),
				record.categoryName(),
				record.merchantId(),
				record.merchantName()
			))
			.toList());
	}

	private void validateConfiguration() {
		if (isBlank(properties.getBaseUrl())
			|| isBlank(properties.getInstitutionCode())
			|| isBlank(properties.getFintechAppNo())
			|| isBlank(apiKey)) {
			throw new AssetException(AssetErrorCode.MERCHANT_API_CALL_FAILED);
		}
	}

	private AssetErrorCode resolveErrorCode(SsafyApiException exception) {
		if (exception.reason() == SsafyApiException.Reason.INVALID_CONFIGURATION
			|| exception.reason() == SsafyApiException.Reason.CALL_FAILED) {
			return AssetErrorCode.MERCHANT_API_CALL_FAILED;
		}
		return AssetErrorCode.INVALID_MERCHANT_API_RESPONSE;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
