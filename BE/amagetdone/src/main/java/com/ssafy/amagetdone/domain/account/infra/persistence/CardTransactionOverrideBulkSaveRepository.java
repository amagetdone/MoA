package com.ssafy.amagetdone.domain.account.infra.persistence;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.amagetdone.domain.account.application.usecase.CardTransactionOverrideBulkSavePort;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CardTransactionOverrideBulkSaveRepository implements CardTransactionOverrideBulkSavePort {

	static final String INSERT_TRANSACTION_OVERRIDE_SQL = """
		INSERT INTO transaction_override (
			id,
			category_id,
			transaction_date_time,
			transaction_balance,
			transaction_memo,
			created_at,
			updated_at,
			is_deleted
		) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String INSERT_CARD_TRANSACTION_OVERRIDE_SQL = """
		INSERT INTO card_transaction_override (
			id,
			transaction_unique_no,
			merchant_id,
			card_status,
			bill_statements_yn,
			bill_statements_status,
			card_unique_no
		) VALUES (?, ?, ?, ?, ?, ?, ?)
		""";

	private final JdbcTemplate jdbcTemplate;

	public CardTransactionOverrideBulkSaveRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void saveAll(List<CardTransactionOverride> transactions) {
		if (transactions == null || transactions.isEmpty()) {
			return;
		}

		LocalDateTime now = LocalDateTime.now();
		Timestamp nowTimestamp = Timestamp.valueOf(now);

		jdbcTemplate.batchUpdate(
			INSERT_TRANSACTION_OVERRIDE_SQL,
			transactions.stream()
				.map(transaction -> new Object[] {
					transaction.getId(),
					transaction.getCategory().getId(),
					Timestamp.valueOf(transaction.getTransactionDateTime()),
					transaction.getTransactionBalance(),
					transaction.getTransactionMemo(),
					nowTimestamp,
					nowTimestamp,
					false
				})
				.toList()
		);

		jdbcTemplate.batchUpdate(
			INSERT_CARD_TRANSACTION_OVERRIDE_SQL,
			transactions.stream()
				.map(transaction -> new Object[] {
					transaction.getId(),
					transaction.getTransactionUniqueNo(),
					transaction.getMerchant().getId(),
					transaction.isCardStatus(),
					transaction.isBillStatementsYn(),
					transaction.getBillStatementsStatus(),
					transaction.getCard().getCardUniqueNo()
				})
				.toList()
		);
	}
}
