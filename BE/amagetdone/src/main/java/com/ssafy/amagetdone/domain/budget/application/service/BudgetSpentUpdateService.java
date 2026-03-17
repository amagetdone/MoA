package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionExcludedReason;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetSpentUpdateService {

	private final BudgetWeekRepository budgetWeekRepository;
	private final EssentialWeekAllocationRepository essentialWeekAllocationRepository;

	public void updateSpentAmounts(User user, List<TransactionAnalysisResult> analysisResults) {
		if (user == null || analysisResults == null || analysisResults.isEmpty()) {
			return;
		}

		Map<LocalDate, Optional<BudgetWeek>> budgetWeeksByDate = new HashMap<>();
		Map<BudgetWeek, List<EssentialWeekAllocation>> essentialAllocationsByBudgetWeek = new HashMap<>();

		for (TransactionAnalysisResult analysisResult : analysisResults) {
			if (analysisResult == null || analysisResult.getTransaction() == null
				|| analysisResult.getTransaction().getTransactionDateTime() == null) {
				continue;
			}

			long spentAmount = normalizeAmount(analysisResult.getTransaction().getTransactionBalance());
			if (spentAmount <= 0L) {
				continue;
			}

			LocalDate spentDate = analysisResult.getTransaction().getTransactionDateTime().toLocalDate();
			Optional<BudgetWeek> budgetWeek = budgetWeeksByDate.computeIfAbsent(
				spentDate,
				date -> budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(user, date, date)
			);
			if (budgetWeek.isEmpty()) {
				continue;
			}

			if (analysisResult.isAnalysisIncluded()) {
				updateEssentialSpentAmount(
					analysisResult,
					budgetWeek.get(),
					spentAmount,
					essentialAllocationsByBudgetWeek
				);
				continue;
			}

			if (analysisResult.getExcludedReason() == TransactionExcludedReason.NON_ESSENTIAL) {
				BudgetWeek targetBudgetWeek = budgetWeek.get();
				targetBudgetWeek.setActualSpentAmount(normalizeAmount(targetBudgetWeek.getActualSpentAmount()) + spentAmount);
			}
		}
	}

	private void updateEssentialSpentAmount(
		TransactionAnalysisResult analysisResult,
		BudgetWeek budgetWeek,
		long spentAmount,
		Map<BudgetWeek, List<EssentialWeekAllocation>> essentialAllocationsByBudgetWeek
	) {
		if (analysisResult.getCategory() == null || analysisResult.getExpenseScope() == null) {
			return;
		}

		List<EssentialWeekAllocation> allocations = essentialAllocationsByBudgetWeek.computeIfAbsent(
			budgetWeek,
			essentialWeekAllocationRepository::findByBudgetWeek
		);

		allocations.stream()
			.filter(allocation -> isMatchingAllocation(analysisResult, allocation))
			.findFirst()
			.ifPresent(allocation -> allocation.setActualSpentAmount(normalizeAmount(allocation.getActualSpentAmount()) + spentAmount));
	}

	private boolean isMatchingAllocation(TransactionAnalysisResult analysisResult, EssentialWeekAllocation allocation) {
		if (allocation.getCategory() == null || allocation.getExpenseScope() != analysisResult.getExpenseScope()) {
			return false;
		}

		Integer analysisCategoryId = analysisResult.getCategory().getId();
		Integer allocationCategoryId = allocation.getCategory().getId();
		if (analysisCategoryId != null || allocationCategoryId != null) {
			return Objects.equals(analysisCategoryId, allocationCategoryId);
		}

		return Objects.equals(analysisResult.getCategory().getName(), allocation.getCategory().getName());
	}

	private long normalizeAmount(Long amount) {
		return amount == null ? 0L : amount;
	}
}
