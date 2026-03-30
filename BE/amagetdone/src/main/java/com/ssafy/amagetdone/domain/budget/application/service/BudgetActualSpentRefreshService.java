package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisMetadataRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionExcludedReason;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetActualSpentRefreshService {

	private final TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;
	private final TransactionAnalysisResultRepository transactionAnalysisResultRepository;
	private final BudgetWeekRepository budgetWeekRepository;
	private final EssentialWeekAllocationRepository essentialWeekAllocationRepository;

	public void refresh(Integer userId, LocalDate today) {
		if (userId == null || today == null) {
			return;
		}

		List<BudgetWeek> openWeeks = budgetWeekRepository.findAllByUser_IdOrderByWeekStartDateAsc(userId).stream()
			.filter(budgetWeek -> budgetWeek.getStatus() != BudgetWeekStatus.CLOSED)
			.toList();
		refresh(userId, openWeeks, today.atStartOfDay(), today.atTime(LocalTime.MAX));
	}

	public void refresh(Integer userId, LocalDate fromDate, LocalDate toDate) {
		if (userId == null || fromDate == null || toDate == null || toDate.isBefore(fromDate)) {
			return;
		}

		List<BudgetWeek> targetWeeks = budgetWeekRepository.findAllByUser_IdOrderByWeekStartDateAsc(userId).stream()
			.filter(budgetWeek -> budgetWeek.getStatus() != BudgetWeekStatus.CLOSED)
			.filter(budgetWeek -> overlaps(budgetWeek, fromDate, toDate))
			.toList();
		refresh(userId, targetWeeks, fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX));
	}

	public void refresh(Integer userId, Collection<BudgetWeek> targetWeeks) {
		if (userId == null || targetWeeks == null || targetWeeks.isEmpty()) {
			return;
		}

		List<BudgetWeek> orderedWeeks = targetWeeks.stream()
			.filter(java.util.Objects::nonNull)
			.sorted(Comparator.comparing(BudgetWeek::getWeekStartDate))
			.toList();
		if (orderedWeeks.isEmpty()) {
			return;
		}

		refresh(
			userId,
			orderedWeeks,
			orderedWeeks.get(0).getWeekStartDate().atStartOfDay(),
			orderedWeeks.get(orderedWeeks.size() - 1).getWeekEndDate().atTime(LocalTime.MAX)
		);
	}

	private void refresh(
		Integer userId,
		List<BudgetWeek> targetWeeks,
		LocalDateTime fromDateTime,
		LocalDateTime toDateTime
	) {
		if (targetWeeks == null || targetWeeks.isEmpty()) {
			return;
		}

		Optional<TransactionAnalysisMetadata> latestMetadata = transactionAnalysisMetadataRepository
			.findTopByUser_IdOrderByAnalyzedAtDesc(userId);
		if (latestMetadata.isEmpty()) {
			resetTargetWeeks(targetWeeks);
			return;
		}

		resetTargetWeeks(targetWeeks);

		List<TransactionAnalysisResult> latestResults = transactionAnalysisResultRepository
			.findAllByUserIdAndAnalyzedAtAndTransactionDateTimeBetweenOrderByTransactionDateTimeAsc(
				userId,
				latestMetadata.get().getAnalyzedAt(),
				fromDateTime,
				toDateTime
			);
		if (latestResults.isEmpty()) {
			return;
		}

		java.util.Map<BudgetWeek, List<EssentialWeekAllocation>> allocationsByWeek = loadAllocationsByWeek(targetWeeks);
		for (TransactionAnalysisResult result : latestResults) {
			TransactionOverride transaction = result.getTransaction();
			if (transaction == null || transaction.getTransactionDateTime() == null) {
				continue;
			}

			long spentAmount = zero(transaction.getTransactionBalance());
			if (spentAmount <= 0L) {
				continue;
			}

			BudgetWeek targetWeek = findContainingWeek(targetWeeks, transaction.getTransactionDateTime().toLocalDate());
			if (targetWeek == null) {
				continue;
			}

			if (result.isAnalysisIncluded() && result.getCategory() != null && result.getExpenseScope() != null) {
				refreshEssentialSpentAmount(
					result,
					allocationsByWeek.getOrDefault(targetWeek, List.of()),
					spentAmount
				);
				continue;
			}

			if (result.getExcludedReason() == TransactionExcludedReason.NON_ESSENTIAL) {
				targetWeek.setActualSpentAmount(zero(targetWeek.getActualSpentAmount()) + spentAmount);
			}
		}
	}

	private void resetTargetWeeks(List<BudgetWeek> targetWeeks) {
		for (BudgetWeek budgetWeek : targetWeeks) {
			budgetWeek.setActualSpentAmount(0L);
			List<EssentialWeekAllocation> allocations = essentialWeekAllocationRepository.findByBudgetWeek(budgetWeek);
			for (EssentialWeekAllocation allocation : allocations) {
				allocation.setActualSpentAmount(0L);
			}
		}
	}

	private java.util.Map<BudgetWeek, List<EssentialWeekAllocation>> loadAllocationsByWeek(List<BudgetWeek> targetWeeks) {
		java.util.Map<BudgetWeek, List<EssentialWeekAllocation>> allocationsByWeek = new java.util.LinkedHashMap<>();
		for (BudgetWeek budgetWeek : targetWeeks) {
			allocationsByWeek.put(budgetWeek, essentialWeekAllocationRepository.findByBudgetWeek(budgetWeek));
		}
		return allocationsByWeek;
	}

	private void refreshEssentialSpentAmount(
		TransactionAnalysisResult result,
		List<EssentialWeekAllocation> allocations,
		long spentAmount
	) {
		allocations.stream()
			.filter(allocation -> matches(result, allocation))
			.findFirst()
			.ifPresent(allocation -> allocation.setActualSpentAmount(zero(allocation.getActualSpentAmount()) + spentAmount));
	}

	private boolean matches(TransactionAnalysisResult result, EssentialWeekAllocation allocation) {
		if (allocation.getCategory() == null || allocation.getExpenseScope() != result.getExpenseScope()) {
			return false;
		}

		Integer resultCategoryId = result.getCategory().getId();
		Integer allocationCategoryId = allocation.getCategory().getId();
		if (resultCategoryId != null || allocationCategoryId != null) {
			return java.util.Objects.equals(resultCategoryId, allocationCategoryId);
		}

		return java.util.Objects.equals(result.getCategory().getName(), allocation.getCategory().getName());
	}

	private BudgetWeek findContainingWeek(List<BudgetWeek> targetWeeks, LocalDate date) {
		for (BudgetWeek targetWeek : targetWeeks) {
			if (!date.isBefore(targetWeek.getWeekStartDate()) && !date.isAfter(targetWeek.getWeekEndDate())) {
				return targetWeek;
			}
		}
		return null;
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}

	private boolean overlaps(BudgetWeek budgetWeek, LocalDate fromDate, LocalDate toDate) {
		return !budgetWeek.getWeekEndDate().isBefore(fromDate) && !budgetWeek.getWeekStartDate().isAfter(toDate);
	}
}
