package com.ssafy.amagetdone.domain.account.infra.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class CardTransactionOverrideBulkSaveRepositoryTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Test
	void saveAllInsertsParentAndChildTablesWithJdbcBatch() {
		CardTransactionOverrideBulkSaveRepository repository = new CardTransactionOverrideBulkSaveRepository(jdbcTemplate);

		repository.saveAll(List.of(sampleTransaction(101), sampleTransaction(102)));

		InOrder inOrder = inOrder(jdbcTemplate);
		inOrder.verify(jdbcTemplate).batchUpdate(eq(CardTransactionOverrideBulkSaveRepository.INSERT_TRANSACTION_OVERRIDE_SQL), any(List.class));
		inOrder.verify(jdbcTemplate).batchUpdate(eq(CardTransactionOverrideBulkSaveRepository.INSERT_CARD_TRANSACTION_OVERRIDE_SQL), any(List.class));
	}

	@Test
	void saveAllSkipsJdbcCallsWhenTransactionsAreEmpty() {
		CardTransactionOverrideBulkSaveRepository repository = new CardTransactionOverrideBulkSaveRepository(jdbcTemplate);

		repository.saveAll(List.of());

		org.mockito.Mockito.verify(jdbcTemplate, never()).batchUpdate(any(String.class), any(List.class));
	}

	private CardTransactionOverride sampleTransaction(int id) {
		User user = User.builder()
			.id(1)
			.build();
		Account account = Account.create(
			user,
			"001-1234-5678",
			"product-1",
			"Main account",
			"Demand deposit",
			"001",
			"Bank",
			"KRW"
		);
		Card card = Card.create(
			123456789L,
			account,
			"123",
			"5241111122223333",
			"Card",
			"001",
			"Issuer",
			LocalDateTime.of(2029, 3, 31, 0, 0),
			"desc",
			"3",
			0L,
			0L
		);
		return CardTransactionOverride.builder()
			.id(id)
			.category(Category.create(4, "etc"))
			.transactionDateTime(LocalDateTime.of(2026, 3, 25, 8, id % 60))
			.transactionBalance(12_345L)
			.transactionMemo("memo-" + id)
			.transactionUniqueNo(20260325000L + id)
			.merchant(Merchant.builder().id(77).name("merchant").build())
			.cardStatus(true)
			.billStatementsYn(true)
			.billStatementsStatus("정상")
			.card(card)
			.build();
	}
}
