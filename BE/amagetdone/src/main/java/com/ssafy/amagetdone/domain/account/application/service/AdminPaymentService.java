package com.ssafy.amagetdone.domain.account.application.service;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.application.usecase.CancelAdminCardPaymentUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ExecuteAdminAccountPaymentUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ExecuteAdminCardPaymentUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminPaymentService {

	private final ExecuteAdminAccountPaymentUseCase executeAdminAccountPaymentUseCase;
	private final ExecuteAdminCardPaymentUseCase executeAdminCardPaymentUseCase;
	private final CancelAdminCardPaymentUseCase cancelAdminCardPaymentUseCase;

	public AccountAdminDto.PaymentResponse executeAccountPayment(Integer userId, AccountAdminDto.AccountPaymentRequest request) {
		return executeAdminAccountPaymentUseCase.execute(userId, request);
	}

	public AccountAdminDto.PaymentResponse executeCardPayment(Integer userId, AccountAdminDto.CardPaymentRequest request) {
		return executeAdminCardPaymentUseCase.execute(userId, request);
	}

	public AccountAdminDto.PaymentResponse cancelCardPayment(Integer userId, AccountAdminDto.CardPaymentCancelRequest request) {
		return cancelAdminCardPaymentUseCase.execute(userId, request);
	}
}
