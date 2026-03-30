package com.ssafy.amagetdone.domain.account.infra.client;

import java.util.List;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.dto.MerchantQueryDto;
import com.ssafy.amagetdone.global.infra.ssafy.envelope.SsafyResponseEnvelope;
import com.ssafy.amagetdone.global.infra.ssafy.exception.SsafyApiException;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiRequestFactory;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiResponseValidator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class SsafyMerchantQueryApiClient {

	private static final String INQUIRE_MERCHANT_LIST_API = "inquireMerchantList";

	private final RestClient restClient;
	private final AccountSsafyApiProperties properties;
	private final SsafyApiRequestFactory requestFactory;
	private final SsafyApiResponseValidator responseValidator;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<List<MerchantQueryDto.MerchantItem>>> responseType;

	@Value("${account.ssafy.api-key}")
	private String apiKey;

	public SsafyMerchantQueryApiClient(
		RestClient.Builder restClientBuilder,
		AccountSsafyApiProperties properties,
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

	public MerchantQueryDto.Response inquireMerchantList() {
		validateConfiguration();

		try {
			List<MerchantQueryDto.MerchantItem> payload = responseValidator.validate(
				restClient.post()
					.uri(properties.getBaseUrl() + "/creditCard/" + INQUIRE_MERCHANT_LIST_API)
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestFactory.create(
						INQUIRE_MERCHANT_LIST_API,
						properties.getInstitutionCode(),
						properties.getFintechAppNo(),
						apiKey,
						null,
						new EmptyRequest()
					))
					.retrieve()
					.body(responseType)
			).payload();

			if (payload == null) {
				throw new AccountException(AccountErrorCode.INVALID_LINKED_CARD_QUERY_API_RESPONSE);
			}

			return new MerchantQueryDto.Response(payload);
		} catch (SsafyApiException exception) {
			if (exception.reason() == SsafyApiException.Reason.INVALID_CONFIGURATION
				|| exception.reason() == SsafyApiException.Reason.CALL_FAILED) {
				throw new AccountException(AccountErrorCode.LINKED_CARD_QUERY_API_CALL_FAILED, exception);
			}
			throw new AccountException(AccountErrorCode.INVALID_LINKED_CARD_QUERY_API_RESPONSE, exception);
		} catch (RestClientException exception) {
			throw new AccountException(AccountErrorCode.LINKED_CARD_QUERY_API_CALL_FAILED, exception);
		}
	}

	private void validateConfiguration() {
		if (isBlank(properties.getBaseUrl())
			|| isBlank(properties.getInstitutionCode())
			|| isBlank(properties.getFintechAppNo())
			|| isBlank(apiKey)) {
			throw new AccountException(AccountErrorCode.LINKED_CARD_QUERY_API_CALL_FAILED);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record EmptyRequest() {
	}
}
