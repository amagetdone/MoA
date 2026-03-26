package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.infra.client.AccountSsafyApiProperties;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminDemandDepositApiClient;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionCategoryAssignmentService;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.DemandDepositTransactionDto;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ExecuteAdminAccountPaymentUseCaseTest {

	@Test
	void executePersistsWithdrawalOverrideAfterSsafyTransfer() {
		AccountRepository accountRepository = mock(AccountRepository.class);
		AccountTransactionOverrideRepository accountTransactionOverrideRepository = mock(AccountTransactionOverrideRepository.class);
		SsafyAdminDemandDepositApiClient ssafyAdminDemandDepositApiClient = mock(SsafyAdminDemandDepositApiClient.class);
		TransactionCategoryAssignmentService transactionCategoryAssignmentService = mock(TransactionCategoryAssignmentService.class);
		AccountSsafyApiProperties properties = new AccountSsafyApiProperties();
		properties.setApiKey("test-api-key");
		ExecuteAdminAccountPaymentUseCase useCase = new ExecuteAdminAccountPaymentUseCase(
			accountRepository,
			accountTransactionOverrideRepository,
			ssafyAdminDemandDepositApiClient,
			transactionCategoryAssignmentService,
			properties
		);

		User user = User.createPendingSocialUser();
		user.setId(7);
		user.setUserKey("user-key");
		user.setBirthDate(LocalDate.of(2000, 1, 1));
		Account account = Account.create(
			user,
			"0011234567890123",
			"product-1",
			"생활비 계좌",
			"수시입출금",
			"001",
			"테스트은행",
			"KRW"
		);
		account.markVerifiedLinked();
		AccountAdminDto.AccountPaymentRequest request = new AccountAdminDto.AccountPaymentRequest(
			"0011234567890123",
			"0010000000000001",
			10000L,
			"테스트 출금",
			"테스트 입금"
		);
		DemandDepositTransactionDto.Item item = new DemandDepositTransactionDto.Item(
			91L,
			"20260324",
			"092500",
			"2",
			"출금(이체)",
			"0010000000000001",
			10000L,
			450000L,
			"테스트 출금",
			"테스트 메모"
		);
		Category category = Category.create(4, "기타");

		when(accountRepository.findActiveLinkedAccountByUserIdAndAccountNo(7, "0011234567890123"))
			.thenReturn(Optional.of(account));
		when(ssafyAdminDemandDepositApiClient.createTransferTransaction(
			"test-api-key",
			"user-key",
			"0011234567890123",
			"0010000000000001",
			10000L,
			"테스트 출금",
			"테스트 입금"
		)).thenReturn(item);
		when(transactionCategoryAssignmentService.assignCategories(any()))
			.thenReturn(Map.of("테스트 출금", category));

		AccountAdminDto.PaymentResponse result = useCase.execute(7, request);

		ArgumentCaptor<AccountTransactionOverride> captor = ArgumentCaptor.forClass(AccountTransactionOverride.class);
		verify(accountTransactionOverrideRepository).save(captor.capture());
		assertThat(captor.getValue().getId()).isNull();
		assertThat(captor.getValue().getAccount()).isEqualTo(account);
		assertThat(captor.getValue().getTransactionUniqueNo()).isEqualTo(91L);
		assertThat(captor.getValue().getTransactionBalance()).isEqualTo(10000L);
		assertThat(result.targetType()).isEqualTo("account");
		assertThat(result.targetNo()).isEqualTo("0011234567890123");
		assertThat(result.transactionUniqueNo()).isEqualTo(91L);
		assertThat(result.status()).isEqualTo("COMPLETED");
	}
}
