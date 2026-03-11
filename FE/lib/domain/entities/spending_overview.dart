import 'package:more_app/domain/entities/spending.dart';

class SpendingOverview {
  final List<SpendingCategory> categories;
  final List<DailySpending> dailySpendings;
  final List<DailySpending> monthlySpendings;
  final int weeklyBudget;
  final int monthlyIncome;
  final int monthlySavingsGoal;
  final int monthlyFixedExpense;

  const SpendingOverview({
    this.categories = const [],
    this.dailySpendings = const [],
    this.monthlySpendings = const [],
    this.weeklyBudget = 0,
    this.monthlyIncome = 0,
    this.monthlySavingsGoal = 0,
    this.monthlyFixedExpense = 0,
  });
}
