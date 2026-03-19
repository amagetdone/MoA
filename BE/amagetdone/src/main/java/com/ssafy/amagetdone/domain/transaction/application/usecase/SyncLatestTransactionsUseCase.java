package com.ssafy.amagetdone.domain.transaction.application.usecase;

import com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncService;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncService.SyncResult;

import org.springframework.stereotype.Component;

@Component
public class SyncLatestTransactionsUseCase {

	private final TransactionSyncService transactionSyncService;

	public SyncLatestTransactionsUseCase(TransactionSyncService transactionSyncService) {
		this.transactionSyncService = transactionSyncService;
	}

	public SyncResult execute(Integer userId, Integer months, boolean forceFullSync) {
		int requestedMonths = months == null ? 6 : months;
		return transactionSyncService.syncLatest(userId, requestedMonths, forceFullSync);
	}
}
