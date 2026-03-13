package com.ssafy.amagetdone.domain.transaction.model.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionAnalysisJobStatus;

import org.junit.jupiter.api.Test;

class TransactionRepositoryQueryTest {

	@Test
	void accountTransactionRepositoryExposesLatestAndDedupLookups() throws Exception {
		assertMethod(
			AccountTransactionOverrideRepository.class,
			"findTopByAccount_IdOrderByTransactionDateTimeDesc",
			Optional.class,
			String.class
		);
		assertMethod(
			AccountTransactionOverrideRepository.class,
			"existsByAccount_IdAndTransactionUniqueNo",
			boolean.class,
			String.class,
			Long.class
		);
		assertMethod(
			AccountTransactionOverrideRepository.class,
			"findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc",
			List.class,
			Integer.class,
			LocalDateTime.class,
			LocalDateTime.class
		);
	}

	@Test
	void cardTransactionRepositoryExposesLatestAndDedupLookups() throws Exception {
		assertMethod(
			CardTransactionOverrideRepository.class,
			"findTopByCard_CardUniqueNoOrderByTransactionDateTimeDesc",
			Optional.class,
			Long.class
		);
		assertMethod(
			CardTransactionOverrideRepository.class,
			"existsByCard_CardUniqueNoAndTransactionUniqueNo",
			boolean.class,
			Long.class,
			Long.class
		);
		assertMethod(
			CardTransactionOverrideRepository.class,
			"findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc",
			List.class,
			Integer.class,
			LocalDateTime.class,
			LocalDateTime.class
		);
	}

	@Test
	void analysisRepositoriesExposeJobAndResultLookups() throws Exception {
		assertMethod(
			TransactionAnalysisJobRepository.class,
			"findFirstByUser_IdAndStatusOrderByRequestedAtDesc",
			Optional.class,
			Integer.class,
			TransactionAnalysisJobStatus.class
		);
		assertMethod(
			TransactionAnalysisJobRepository.class,
			"existsByUser_IdAndStatus",
			boolean.class,
			Integer.class,
			TransactionAnalysisJobStatus.class
		);
		assertMethod(
			TransactionAnalysisResultRepository.class,
			"findByTransaction_Id",
			Optional.class,
			Integer.class
		);
		assertMethod(
			TransactionAnalysisResultRepository.class,
			"findAllByTransaction_IdIn",
			List.class,
			Collection.class
		);
		assertMethod(
			TransactionAnalysisResultRepository.class,
			"summarizeByUserIdAndPeriod",
			List.class,
			Integer.class,
			LocalDateTime.class,
			LocalDateTime.class
		);
	}

	@Test
	void budgetSnapshotRepositoryExposesWeeklyLookupQueries() throws Exception {
		assertMethod(
			EssentialExpenseSnapshotWeekRepository.class,
			"findByUser_IdAndYearWeek",
			Optional.class,
			Integer.class,
			String.class
		);
		assertMethod(
			EssentialExpenseSnapshotWeekRepository.class,
			"findAllByUser_IdAndWeekStartDateBetweenOrderByWeekStartDate",
			List.class,
			Integer.class,
			LocalDate.class,
			LocalDate.class
		);
	}

	private Method assertMethod(
		Class<?> repositoryType,
		String methodName,
		Class<?> returnType,
		Class<?>... parameterTypes
	) throws Exception {
		Method method = repositoryType.getMethod(methodName, parameterTypes);
		assertNotNull(method);
		assertEquals(returnType, method.getReturnType());
		return method;
	}
}
