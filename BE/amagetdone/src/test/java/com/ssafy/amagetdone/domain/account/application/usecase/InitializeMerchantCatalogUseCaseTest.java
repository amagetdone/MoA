package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import com.ssafy.amagetdone.domain.account.infra.client.SsafyMerchantQueryApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.MerchantQueryDto;
import com.ssafy.amagetdone.domain.account.model.vo.MerchantCatalogEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitializeMerchantCatalogUseCaseTest {

	@Mock
	private SsafyMerchantQueryApiClient ssafyMerchantQueryApiClient;

	@Mock
	private MerchantCatalogBulkSyncPort merchantCatalogBulkSyncPort;

	@InjectMocks
	private InitializeMerchantCatalogUseCase initializeMerchantCatalogUseCase;

	@Test
	void executeDelegatesFilteredDeduplicatedCatalogToBulkSyncPort() {
		given(ssafyMerchantQueryApiClient.inquireMerchantList()).willReturn(
			new MerchantQueryDto.Response(List.of(
				new MerchantQueryDto.MerchantItem("CG-traffic", "교통", 7L, "UT 택시"),
				new MerchantQueryDto.MerchantItem("CG-traffic", "교통", 7L, "UT 택시 리브랜딩"),
				new MerchantQueryDto.MerchantItem("CG-cafe", "카페", 8L, "스타벅스"),
				new MerchantQueryDto.MerchantItem("CG-invalid", "기타", null, "누락"),
				new MerchantQueryDto.MerchantItem("CG-blank", "기타", 9L, " ")
			))
		);

		initializeMerchantCatalogUseCase.execute();

		ArgumentCaptor<List<MerchantCatalogEntry>> captor = ArgumentCaptor.forClass(List.class);
		verify(merchantCatalogBulkSyncPort).synchronize(captor.capture());
		assertThat(captor.getValue())
			.containsExactly(
				new MerchantCatalogEntry(7, "UT 택시 리브랜딩"),
				new MerchantCatalogEntry(8, "스타벅스")
			);
	}
}
