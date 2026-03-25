package com.ssafy.amagetdone.domain.account.application.usecase;

import java.util.List;

import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;

public interface CardTransactionOverrideBulkSavePort {

	void saveAll(List<CardTransactionOverride> transactions);
}
