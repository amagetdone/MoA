package com.ssafy.amagetdone.domain.account.infra.persistence;

import java.util.List;

import com.ssafy.amagetdone.domain.account.application.usecase.MerchantCatalogBulkSyncPort;
import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.model.vo.MerchantCatalogEntry;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MerchantBulkSyncRepository implements MerchantCatalogBulkSyncPort {

	static final String CREATE_STAGING_TABLE_SQL = """
		CREATE TEMP TABLE tmp_merchant_catalog_sync (
			id INTEGER PRIMARY KEY,
			name VARCHAR(100) NOT NULL
		) ON COMMIT DROP
		""";
	static final String INSERT_STAGING_SQL = "INSERT INTO tmp_merchant_catalog_sync (id, name) VALUES (?, ?)";
	static final String UPSERT_MERCHANT_SQL = """
		INSERT INTO merchant (id, name)
		SELECT id, name
		FROM tmp_merchant_catalog_sync
		ON CONFLICT (id) DO UPDATE
		SET name = EXCLUDED.name
		WHERE merchant.name IS DISTINCT FROM EXCLUDED.name
		""";
	static final String COUNT_REFERENCED_STALE_SQL = """
		SELECT COUNT(DISTINCT cto.merchant_id)
		FROM card_transaction_override cto
		JOIN merchant m ON m.id = cto.merchant_id
		LEFT JOIN tmp_merchant_catalog_sync tmp ON tmp.id = m.id
		WHERE tmp.id IS NULL
		""";
	static final String DELETE_STALE_SQL = """
		DELETE FROM merchant m
		WHERE NOT EXISTS (
			SELECT 1
			FROM tmp_merchant_catalog_sync tmp
			WHERE tmp.id = m.id
		)
		""";

	private final JdbcTemplate jdbcTemplate;

	public MerchantBulkSyncRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void synchronize(List<MerchantCatalogEntry> merchants) {
		jdbcTemplate.execute(CREATE_STAGING_TABLE_SQL);
		if (!merchants.isEmpty()) {
			jdbcTemplate.batchUpdate(
				INSERT_STAGING_SQL,
				merchants.stream()
					.map(merchant -> new Object[] {merchant.id(), merchant.name()})
					.toList()
			);
		}
		jdbcTemplate.update(UPSERT_MERCHANT_SQL);

		Integer referencedStaleCount = jdbcTemplate.queryForObject(COUNT_REFERENCED_STALE_SQL, Integer.class);
		if (referencedStaleCount != null && referencedStaleCount > 0) {
			throw new AccountException(AccountErrorCode.MERCHANT_CATALOG_OUT_OF_SYNC);
		}

		jdbcTemplate.update(DELETE_STALE_SQL);
	}
}
