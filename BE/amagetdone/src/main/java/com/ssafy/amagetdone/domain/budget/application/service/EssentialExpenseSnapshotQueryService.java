package com.ssafy.amagetdone.domain.budget.application.service;

import java.util.List;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDto;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeekCategory;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekCategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EssentialExpenseSnapshotQueryService {

	private final EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;
	private final EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;

	public EssentialExpenseSnapshotQueryService(
		EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository,
		EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository
	) {
		this.essentialExpenseSnapshotWeekRepository = essentialExpenseSnapshotWeekRepository;
		this.essentialExpenseSnapshotWeekCategoryRepository = essentialExpenseSnapshotWeekCategoryRepository;
	}

	public WeeklyEssentialExpenseDto.Response getWeeklyEssentialExpenses(Integer userId, String yearWeek) {
		EssentialExpenseSnapshotWeek snapshotWeek = essentialExpenseSnapshotWeekRepository.findByUser_IdAndYearWeek(userId, yearWeek)
			.orElseThrow(() -> new IllegalArgumentException("Weekly essential expense snapshot not found"));
		List<EssentialExpenseSnapshotWeekCategory> categories = essentialExpenseSnapshotWeekCategoryRepository.findAll().stream()
			.filter(item -> item.getSnapshotWeek().getId().equals(snapshotWeek.getId()))
			.toList();

		return new WeeklyEssentialExpenseDto.Response(
			snapshotWeek.getYearWeek(),
			snapshotWeek.getWeekStartDate(),
			snapshotWeek.getWeekEndDate(),
			snapshotWeek.getBaseMonths(),
			snapshotWeek.getEssentialAmount(),
			snapshotWeek.getFixedEssentialAmount(),
			snapshotWeek.getVariableEssentialAmount(),
			toCategoryExpenses(categories, ExpenseScope.FIXED),
			toCategoryExpenses(categories, ExpenseScope.VARIABLE),
			snapshotWeek.getAnalyzedAt()
		);
	}

	private List<WeeklyEssentialExpenseDto.CategoryExpense> toCategoryExpenses(
		List<EssentialExpenseSnapshotWeekCategory> categories,
		ExpenseScope expenseScope
	) {
		return categories.stream()
			.filter(item -> item.getExpenseScope() == expenseScope)
			.map(item -> new WeeklyEssentialExpenseDto.CategoryExpense(
				item.getCategory().getId(),
				item.getCategory().getName(),
				item.getAmount()
			))
			.toList();
	}
}
