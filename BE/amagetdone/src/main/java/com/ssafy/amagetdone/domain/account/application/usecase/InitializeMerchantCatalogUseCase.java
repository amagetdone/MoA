package com.ssafy.amagetdone.domain.account.application.usecase;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ssafy.amagetdone.domain.account.infra.client.SsafyMerchantQueryApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.MerchantQueryDto;
import com.ssafy.amagetdone.domain.account.model.vo.MerchantCatalogEntry;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitializeMerchantCatalogUseCase {

	private final SsafyMerchantQueryApiClient ssafyMerchantQueryApiClient;
	private final MerchantCatalogBulkSyncPort merchantCatalogBulkSyncPort;

	public InitializeMerchantCatalogUseCase(
		SsafyMerchantQueryApiClient ssafyMerchantQueryApiClient,
		MerchantCatalogBulkSyncPort merchantCatalogBulkSyncPort
	) {
		this.ssafyMerchantQueryApiClient = ssafyMerchantQueryApiClient;
		this.merchantCatalogBulkSyncPort = merchantCatalogBulkSyncPort;
	}

	@Transactional
	public void execute() {
		MerchantQueryDto.Response response = ssafyMerchantQueryApiClient.inquireMerchantList();
		merchantCatalogBulkSyncPort.synchronize(toCatalogEntries(response.merchants()));
	}

	private List<MerchantCatalogEntry> toCatalogEntries(List<MerchantQueryDto.MerchantItem> merchants) {
		Map<Integer, String> remoteMerchantNames = new LinkedHashMap<>();
		for (MerchantQueryDto.MerchantItem merchantItem : merchants) {
			if (merchantItem.merchantId() == null || merchantItem.merchantName() == null || merchantItem.merchantName().isBlank()) {
				continue;
			}
			remoteMerchantNames.put(Math.toIntExact(merchantItem.merchantId()), merchantItem.merchantName());
		}
		return remoteMerchantNames.entrySet().stream()
			.map(entry -> new MerchantCatalogEntry(entry.getKey(), entry.getValue()))
			.toList();
	}
}
