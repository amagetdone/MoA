import 'package:more_app/domain/entities/spending_overview.dart';
import 'package:more_app/domain/entities/spending.dart';

abstract class SpendingRepository {
  Future<void> syncTransactions({int months = 3, bool relabel = false});
  Future<void> analyzeTransactions({int months = 3, bool relabel = false});
  Future<List<SpendingCategory>> fetchWeeklyCategories(DateTime weekStart);
  Future<List<DailySpending>> fetchWeeklyDailySpending(DateTime weekStart);
  Future<List<SpendingCategory>> fetchMonthlyCategories(DateTime monthStart);
  Future<List<DailySpending>> fetchMonthlyDailySpending(DateTime monthStart);
  Future<SpendingOverview> fetchSpendingOverview();
  Future<SpendingOverview> updateBudgetPlan({
    required int monthlyIncome,
    required int monthlySavingsGoal,
    required int monthlyFixedExpense,
    required List<SpendingCategory> categories,
  });
}
