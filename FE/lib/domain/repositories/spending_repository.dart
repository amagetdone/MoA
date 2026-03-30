import 'package:more_app/domain/entities/spending_overview.dart';
import 'package:more_app/domain/entities/spending.dart';

abstract class SpendingRepository {
  Future<void> syncTransactions({int months = 1, bool relabel = false});
  Future<void> analyzeTransactions({int months = 1, bool relabel = false});
  Future<WeeklyAnalysisResult> fetchWeeklyAnalysis(DateTime weekStart);
  Future<List<DailySpending>> fetchWeeklyDailySpending(DateTime weekStart);
  Future<MonthlyAnalysisResult> fetchMonthlyAnalysis(DateTime monthStart);
  Future<List<DailySpending>> fetchMonthlyDailySpending(DateTime monthStart);
  Future<SpendingOverview> fetchSpendingOverview();
  Future<SpendingPatternResult> fetchPreviousMonthPattern(DateTime currentMonth);
  Future<SpendingOverview> updateBudgetPlan({
    required int monthlyIncome,
    required int monthlySavingsGoal,
    required int monthlyFixedExpense,
    required List<SpendingCategory> categories,
  });
}
