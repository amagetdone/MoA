package com.ssafy.amagetdone.domain.transaction.support.util;

import com.ssafy.amagetdone.domain.transaction.infra.client.dto.CardTransactionDto;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.DemandDepositTransactionDto;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;

public final class TransactionMerchantTextResolver {

	private TransactionMerchantTextResolver() {
	}

	public static String resolve(TransactionOverride transaction) {
		if (transaction instanceof CardTransactionOverride cardTransaction) {
			return cardTransaction.getMerchant().getName();
		}
		AccountTransactionOverride accountTransaction = (AccountTransactionOverride) transaction;
		return resolveAccountText(accountTransaction.getTransactionSummary(), accountTransaction.getTransactionMemo());
	}

	public static String resolve(DemandDepositTransactionDto.Item item) {
		return resolveAccountText(item.transactionSummary(), item.transactionMemo());
	}

	public static String resolve(CardTransactionDto.Item item) {
		return item.merchantName();
	}

	public static String resolveAccountText(String transactionSummary, String transactionMemo) {
		return firstText(transactionSummary, transactionMemo);
	}

	private static String firstText(String primary, String fallback) {
		if (hasText(primary)) {
			return primary;
		}
		return fallback;
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
