package com.ssafy.amagetdone.domain.transaction.support.util;

import java.util.Set;

import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionExcludedReason;

import org.springframework.stereotype.Component;

@Component
public class TransactionExclusionPolicy {

	private static final Set<String> ESSENTIAL_LABELS = Set.of(
		"교육비",
		"교통비",
		"구독료",
		"세금/공과금",
		"식비",
		"의료/건강",
		"주거/통신"
	);

	public TransactionExcludedReason determinePreLabelExclusion(
		TransactionOverride transaction,
		Set<String> registeredAccountNos
	) {
		if (!(transaction instanceof AccountTransactionOverride accountTransaction)) {
			return null;
		}
		if (accountTransaction.getTransactionType() == 1) {
			return TransactionExcludedReason.INCOME;
		}
		String memo = accountTransaction.getTransactionMemo();
		if (memo != null && registeredAccountNos.contains(memo.trim())) {
			return TransactionExcludedReason.INTERNAL_TRANSFER;
		}
		return null;
	}

	public TransactionExcludedReason determinePostLabelExclusion(String finalLabel) {
		return isEssential(finalLabel) ? null : TransactionExcludedReason.NON_ESSENTIAL;
	}

	public boolean isEssential(String finalLabel) {
		return finalLabel != null && ESSENTIAL_LABELS.contains(finalLabel);
	}
}
