package com.ssafy.amagetdone.domain.transaction.model.repository;

import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionExcludedReason;

public interface TransactionAnalysisAmountView {

	Boolean getAnalysisIncluded();

	TransactionExcludedReason getExcludedReason();

	ExpenseScope getExpenseScope();

	Long getTotalAmount();

	Long getTransactionCount();
}
