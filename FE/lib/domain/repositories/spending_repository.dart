import 'package:more_app/domain/entities/spending_overview.dart';
import 'package:more_app/domain/entities/spending.dart';

abstract class SpendingRepository {
  Future<SpendingOverview> fetchSpendingOverview();
  Future<SpendingOverview> updateBudgetPlan({
    required int monthlyIncome,
    required int monthlySavingsGoal,
    required int monthlyFixedExpense,
    required List<SpendingCategory> categories,
  });
}
