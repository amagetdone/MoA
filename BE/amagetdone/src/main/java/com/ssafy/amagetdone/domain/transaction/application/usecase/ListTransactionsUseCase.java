package com.ssafy.amagetdone.domain.transaction.application.usecase;

import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryService;

import org.springframework.stereotype.Component;

@Component
public class ListTransactionsUseCase {

	private final TransactionQueryService transactionQueryService;

	public ListTransactionsUseCase(TransactionQueryService transactionQueryService) {
		this.transactionQueryService = transactionQueryService;
	}

	public TransactionListDto.Response execute(Integer userId, TransactionListDto.Query query) {
		return transactionQueryService.listTransactions(userId, query);
	}
}
