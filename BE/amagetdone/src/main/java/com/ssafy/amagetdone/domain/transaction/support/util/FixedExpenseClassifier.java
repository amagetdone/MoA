package com.ssafy.amagetdone.domain.transaction.support.util;

import static com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope.FIXED;
import static com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope.VARIABLE;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;

import org.springframework.stereotype.Component;

@Component
public class FixedExpenseClassifier {

	private static final double AMOUNT_VARIATION_THRESHOLD = 0.1d;
	private static final int DAY_OF_MONTH_VARIATION_THRESHOLD = 3;

	public Map<Integer, ExpenseScope> classify(List<Candidate> candidates) {
		Map<GroupKey, List<Candidate>> groupedCandidates = candidates.stream()
			.collect(Collectors.groupingBy(candidate -> new GroupKey(candidate.finalLabel(), candidate.normalizedMerchantText())));
		Set<GroupKey> recurringFixedKeys = groupedCandidates.entrySet().stream()
			.filter(entry -> isRecurringFixed(entry.getValue()))
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());

		return candidates.stream()
			.collect(Collectors.toMap(
				Candidate::transactionId,
				candidate -> isAlwaysFixed(candidate.finalLabel()) || recurringFixedKeys.contains(
					new GroupKey(candidate.finalLabel(), candidate.normalizedMerchantText())
				)
					? FIXED
					: VARIABLE
			));
	}

	private boolean isAlwaysFixed(String finalLabel) {
		return "구독료".equals(finalLabel);
	}

	private boolean isRecurringFixed(List<Candidate> candidates) {
		if (candidates.size() < 3) {
			return false;
		}
		Set<YearMonth> months = candidates.stream()
			.map(candidate -> YearMonth.from(candidate.occurredDate()))
			.collect(Collectors.toSet());
		if (months.size() < 3) {
			return false;
		}

		double averageAmount = candidates.stream().mapToLong(Candidate::amount).average().orElse(0d);
		if (averageAmount <= 0d) {
			return false;
		}
		double amountVariance = candidates.stream()
			.mapToDouble(candidate -> Math.pow(candidate.amount() - averageAmount, 2))
			.average()
			.orElse(0d);
		double amountCoefficientOfVariation = Math.sqrt(amountVariance) / averageAmount;
		int minDay = candidates.stream().mapToInt(candidate -> candidate.occurredDate().getDayOfMonth()).min().orElse(0);
		int maxDay = candidates.stream().mapToInt(candidate -> candidate.occurredDate().getDayOfMonth()).max().orElse(0);

		return amountCoefficientOfVariation <= AMOUNT_VARIATION_THRESHOLD
			&& (maxDay - minDay) <= DAY_OF_MONTH_VARIATION_THRESHOLD;
	}

	public record Candidate(
		Integer transactionId,
		String finalLabel,
		String normalizedMerchantText,
		LocalDate occurredDate,
		Long amount
	) {
	}

	private record GroupKey(
		String finalLabel,
		String normalizedMerchantText
	) {
	}
}
