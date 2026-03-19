// Responsibility: Verifies account service orchestration for linked-account operations.
package com.ssafy.amagetdone.domain.account.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.application.usecase.ConfirmAccountOwnershipUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.FindUserAccountUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ListAccountsUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ListDemandDepositProductsUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ReconcileLinkedAccountsUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.RegisterDemandDepositProductUseCase;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	@Mock
	private RegisterDemandDepositProductUseCase registerDemandDepositProductUseCase;

	@Mock
	private ListDemandDepositProductsUseCase listDemandDepositProductsUseCase;

	@Mock
	private ConfirmAccountOwnershipUseCase confirmAccountOwnershipUseCase;

	@Mock
	private ListAccountsUseCase listAccountsUseCase;

	@Mock
	private ReconcileLinkedAccountsUseCase reconcileLinkedAccountsUseCase;

	@Mock
	private FindUserAccountUseCase findUserAccountUseCase;

	@Mock
	private AccountRepository accountRepository;

	@InjectMocks
	private AccountService accountService;

	@Test
	void confirmAccountDelegatesToUseCase() {
		AccountDto.ConfirmAccountRequest request = new AccountDto.ConfirmAccountRequest("0011234567890123");
		AccountDto.ConfirmAccountResponse response = new AccountDto.ConfirmAccountResponse(true);

		when(confirmAccountOwnershipUseCase.execute(7, request)).thenReturn(response);

		AccountDto.ConfirmAccountResponse result = accountService.confirmAccount(7, request);

		assertThat(result).isEqualTo(response);
		verify(confirmAccountOwnershipUseCase).execute(7, request);
	}

	@Test
	void getAccountsReconcilesLinkedAccountsBeforeMappingResponse() {
		Account account = mock(Account.class);
		AccountDto.AccountSummaryResponse response = new AccountDto.AccountSummaryResponse(
			"0011234567890123",
			"001",
			"한국은행",
			"데모 수시입출금",
			null,
			true
		);
		when(reconcileLinkedAccountsUseCase.execute(7)).thenReturn(List.of(account));
		when(listAccountsUseCase.toResponses(List.of(account))).thenReturn(List.of(response));

		List<AccountDto.AccountSummaryResponse> result = accountService.getAccounts(7);

		assertThat(result).containsExactly(response);
		verify(reconcileLinkedAccountsUseCase).execute(7);
		verify(listAccountsUseCase).toResponses(List.of(account));
	}

	@Test
	void registerProductDelegatesToUseCase() {
		AccountAdminDto.ProductCreateRequest request = new AccountAdminDto.ProductCreateRequest("001", "데모 상품", "설명");
		AccountAdminDto.ProductResponse response = new AccountAdminDto.ProductResponse(
			"001-1-abc",
			"001",
			"한국은행",
			"1",
			"수시입출금",
			"데모 상품",
			"설명",
			"DOMESTIC"
		);
		when(registerDemandDepositProductUseCase.execute(request)).thenReturn(response);

		assertThat(accountService.registerProduct(request)).isEqualTo(response);
		verify(registerDemandDepositProductUseCase).execute(request);
	}

	@Test
	void deleteAccountSoftDeletesActiveLinkedAccount() {
		User user = User.createPendingSocialUser();
		Account account = Account.create(
			user,
			"0011234567890123",
			null,
			"생활비 계좌",
			"수시입출금",
			"001",
			"한국은행",
			"KRW"
		);
		account.markVerifiedLinked();

		when(findUserAccountUseCase.execute(7, "0011234567890123")).thenReturn(account);
		when(accountRepository.save(account)).thenReturn(account);

		accountService.deleteAccount(7, "0011234567890123");

		assertThat(account.isVerified()).isFalse();
		assertThat(account.isActive()).isFalse();
		assertThat(account.isDeleted()).isTrue();
		verify(findUserAccountUseCase).execute(7, "0011234567890123");
		verify(accountRepository).save(account);
	}
}
