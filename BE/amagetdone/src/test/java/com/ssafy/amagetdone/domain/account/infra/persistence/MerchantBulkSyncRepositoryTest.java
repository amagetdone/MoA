package com.ssafy.amagetdone.domain.account.infra.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import java.util.List;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.model.vo.MerchantCatalogEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class MerchantBulkSyncRepositoryTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Test
	void synchronizeLoadsStagingTableUpsertsAndDeletesStaleMerchants() {
		MerchantBulkSyncRepository repository = new MerchantBulkSyncRepository(jdbcTemplate);
		given(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class))).willReturn(0);

		repository.synchronize(List.of(
			new MerchantCatalogEntry(7, "UT 택시"),
			new MerchantCatalogEntry(8, "스타벅스")
		));

		InOrder inOrder = inOrder(jdbcTemplate);
		inOrder.verify(jdbcTemplate).execute(any(String.class));
		inOrder.verify(jdbcTemplate).batchUpdate(eq(MerchantBulkSyncRepository.INSERT_STAGING_SQL), any(List.class));
		inOrder.verify(jdbcTemplate).update(MerchantBulkSyncRepository.UPSERT_MERCHANT_SQL);
		inOrder.verify(jdbcTemplate).queryForObject(MerchantBulkSyncRepository.COUNT_REFERENCED_STALE_SQL, Integer.class);
		inOrder.verify(jdbcTemplate).update(MerchantBulkSyncRepository.DELETE_STALE_SQL);
	}

	@Test
	void synchronizeThrowsWhenReferencedStaleMerchantsRemain() {
		MerchantBulkSyncRepository repository = new MerchantBulkSyncRepository(jdbcTemplate);
		given(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class))).willReturn(1);

		assertThatThrownBy(() -> repository.synchronize(List.of(new MerchantCatalogEntry(7, "UT 택시"))))
			.isInstanceOf(AccountException.class)
			.extracting(throwable -> ((AccountException)throwable).getErrorCode())
			.isEqualTo(AccountErrorCode.MERCHANT_CATALOG_OUT_OF_SYNC);

		org.mockito.Mockito.verify(jdbcTemplate, never()).update(MerchantBulkSyncRepository.DELETE_STALE_SQL);
	}
}
