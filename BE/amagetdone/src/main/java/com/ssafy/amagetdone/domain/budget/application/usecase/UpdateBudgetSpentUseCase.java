package com.ssafy.amagetdone.domain.budget.application.usecase;

import java.util.List;

import com.ssafy.amagetdone.domain.budget.application.service.BudgetSpentUpdateService;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;

import org.springframework.stereotype.Component;

@Component
public class UpdateBudgetSpentUseCase {

	private final BudgetSpentUpdateService budgetSpentUpdateService;

	public UpdateBudgetSpentUseCase(BudgetSpentUpdateService budgetSpentUpdateService) {
		this.budgetSpentUpdateService = budgetSpentUpdateService;
	}

	public void execute(User user, List<TransactionAnalysisResult> analysisResults) {
		budgetSpentUpdateService.updateSpentAmounts(user, analysisResults);
	}
}
