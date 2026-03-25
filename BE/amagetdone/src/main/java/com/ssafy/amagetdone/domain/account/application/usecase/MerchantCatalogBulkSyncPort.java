package com.ssafy.amagetdone.domain.account.application.usecase;

import java.util.List;

import com.ssafy.amagetdone.domain.account.model.vo.MerchantCatalogEntry;

public interface MerchantCatalogBulkSyncPort {

	void synchronize(List<MerchantCatalogEntry> merchants);
}
