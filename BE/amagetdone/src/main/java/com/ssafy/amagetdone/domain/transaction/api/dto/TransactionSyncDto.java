package com.ssafy.amagetdone.domain.transaction.api.dto;

import com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncService.SyncResult;

public final class TransactionSyncDto {

	private TransactionSyncDto() {
	}

	public record Request(
		Integer months,
		Boolean forceFullSync
	) {
	}

	public record Response(
		int insertedAccountCount,
		int insertedCardCount
	) {
		public static Response from(SyncResult syncResult) {
			return new Response(syncResult.insertedAccountCount(), syncResult.insertedCardCount());
		}
	}
}
