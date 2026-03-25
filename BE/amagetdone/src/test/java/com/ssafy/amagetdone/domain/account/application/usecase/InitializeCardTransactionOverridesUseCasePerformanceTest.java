package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ssafy.amagetdone.domain.account.infra.persistence.CardTransactionOverrideBulkSaveRepository;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.account.model.repository.MerchantRepository;
import com.ssafy.amagetdone.domain.account.support.util.CardRealTransactionScenario;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.member.model.vo.UserStatus;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = {
	"spring.jpa.properties.hibernate.generate_statistics=true"
})
@Import(CardTransactionOverrideBulkSaveRepository.class)
class InitializeCardTransactionOverridesUseCasePerformanceTest {

	private static final int CATEGORY_ID = 904001;
	private static final int MERCHANT_ID = 904001;
	private static final int TRANSACTION_COUNT = new CardRealTransactionScenario().completedMonthSeeds().size();

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private CardRepository cardRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private MerchantRepository merchantRepository;

	@Autowired
	private CardTransactionOverrideRepository cardTransactionOverrideRepository;

	@Autowired
	private CardTransactionOverrideBulkSaveRepository cardTransactionOverrideBulkSaveRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void measureJpaSaveAllVersusJdbcBatchInsert() {
		Category category = categoryRepository.save(Category.create(CATEGORY_ID, "benchmark-category"));
		Merchant merchant = merchantRepository.save(Merchant.builder().id(MERCHANT_ID).name("benchmark-merchant").build());

		Card jpaCard = persistCard("jpa");
		entityManager.flush();
		warmUp(category, merchant, jpaCard);

		BenchmarkResult jpaResult = measureJpaSaveAll(category, merchant, jpaCard, 1_000_000);
		Card jdbcCard = persistCard("jdbc");
		entityManager.flush();
		BenchmarkResult jdbcResult = measureJdbcBatchInsert(category, merchant, jdbcCard, 2_000_000);

		assertThat(jpaResult.insertedCount()).isEqualTo(TRANSACTION_COUNT);
		assertThat(jdbcResult.insertedCount()).isEqualTo(TRANSACTION_COUNT);
		assertThat(jpaResult.elapsedNanos()).isGreaterThan(0L);
		assertThat(jdbcResult.elapsedNanos()).isGreaterThan(0L);

		System.out.printf(
			"CARD_INIT_BENCHMARK baseline_jpa=%.3fs improved_jdbc=%.3fs jpa_prepared_statements=%d rows=%d%n",
			jpaResult.elapsedSeconds(),
			jdbcResult.elapsedSeconds(),
			jpaResult.preparedStatementCount(),
			TRANSACTION_COUNT
		);
	}

	private void warmUp(Category category, Merchant merchant, Card card) {
		cardTransactionOverrideRepository.saveAll(buildTransactions(category, merchant, card, 100_000, 5));
		entityManager.flush();
		jdbcTemplate.update("delete from card_transaction_override where card_unique_no = ?", card.getCardUniqueNo());
		jdbcTemplate.update("""
			delete from transaction_override
			where id between ? and ?
			""", 100_000, 100_004);
		entityManager.clear();
	}

	private BenchmarkResult measureJpaSaveAll(Category category, Merchant merchant, Card card, int idOffset) {
		Statistics statistics = sessionFactory().getStatistics();
		statistics.clear();

		long startedAt = System.nanoTime();
		cardTransactionOverrideRepository.saveAll(buildTransactions(category, merchant, card, idOffset, TRANSACTION_COUNT));
		entityManager.flush();
		long elapsedNanos = System.nanoTime() - startedAt;

		Integer insertedCount = jdbcTemplate.queryForObject(
			"select count(*) from card_transaction_override where card_unique_no = ?",
			Integer.class,
			card.getCardUniqueNo()
		);
		entityManager.clear();

		return new BenchmarkResult(
			elapsedNanos,
			insertedCount == null ? 0 : insertedCount,
			statistics.getPrepareStatementCount()
		);
	}

	private BenchmarkResult measureJdbcBatchInsert(Category category, Merchant merchant, Card card, int idOffset) {
		long startedAt = System.nanoTime();
		cardTransactionOverrideBulkSaveRepository.saveAll(buildTransactions(category, merchant, card, idOffset, TRANSACTION_COUNT));
		long elapsedNanos = System.nanoTime() - startedAt;

		Integer insertedCount = jdbcTemplate.queryForObject(
			"select count(*) from card_transaction_override where card_unique_no = ?",
			Integer.class,
			card.getCardUniqueNo()
		);

		return new BenchmarkResult(
			elapsedNanos,
			insertedCount == null ? 0 : insertedCount,
			-1L
		);
	}

	private Card persistCard(String prefix) {
		User user = userRepository.save(User.builder()
			.email(prefix + "@example.com")
			.nickname(prefix + "-user")
			.birthDate(LocalDate.of(1999, 1, 1))
			.hasFixedIncome(true)
			.status(UserStatus.ACTIVE)
			.userKey(prefix + "-user-key")
			.build());

		Account account = accountRepository.save(Account.create(
			user,
			prefix + "-001-1234567890",
			"product-" + prefix,
			prefix + "-account",
			"Demand deposit",
			"001",
			"Bank",
			"KRW"
		));
		account.markVerifiedLinked();

		return cardRepository.save(Card.create(
			prefix.equals("jpa") ? 904001001L : 904001002L,
			account,
			"123",
			"524111112222" + (prefix.equals("jpa") ? "1001" : "1002"),
			prefix + "-card",
			"001",
			"Issuer",
			LocalDateTime.of(2030, 12, 31, 0, 0),
			"benchmark",
			"3",
			0L,
			0L
		));
	}

	private List<CardTransactionOverride> buildTransactions(
		Category category,
		Merchant merchant,
		Card card,
		int idOffset,
		int count
	) {
		List<CardTransactionOverride> transactions = new ArrayList<>(count);
		LocalDateTime baseDateTime = LocalDateTime.of(2026, 3, 25, 8, 0);
		for (int index = 0; index < count; index++) {
			transactions.add(CardTransactionOverride.builder()
				.id(idOffset + index)
				.category(category)
				.transactionDateTime(baseDateTime.plusMinutes(index))
				.transactionBalance(10_000L + index)
				.transactionMemo("benchmark-" + index)
				.transactionUniqueNo(9_000_000_000L + idOffset + index)
				.merchant(merchant)
				.cardStatus(true)
				.billStatementsYn(true)
				.billStatementsStatus("정상")
				.card(card)
				.build());
		}
		return transactions;
	}

	private SessionFactory sessionFactory() {
		return entityManagerFactory.unwrap(SessionFactory.class);
	}

	private record BenchmarkResult(
		long elapsedNanos,
		int insertedCount,
		long preparedStatementCount
	) {
		private double elapsedSeconds() {
			return elapsedNanos / 1_000_000_000.0;
		}
	}
}
