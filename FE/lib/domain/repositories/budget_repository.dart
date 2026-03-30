import 'package:more_app/domain/entities/budget.dart';

abstract class BudgetRepository {
  Future<BudgetSetup> fetchBudgetSetup();
  Future<BudgetSetup> saveIncomeAndAllocate(int monthlyIncome);
}