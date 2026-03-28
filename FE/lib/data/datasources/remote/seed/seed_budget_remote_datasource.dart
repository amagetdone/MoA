import 'package:more_app/data/datasources/remote/budget_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/seed/seed_data_store.dart';

class SeedBudgetRemoteDataSource implements BudgetRemoteDataSource {
  SeedBudgetRemoteDataSource({required SeedDataStore dataStore})
      : _dataStore = dataStore;

  final SeedDataStore _dataStore;

  @override
  Future<int> getIncome() async {
    await Future.delayed(const Duration(milliseconds: 150));
    final snapshot = await _dataStore.ensureSessionSnapshot();
    return snapshot.spending.monthlyIncome;
  }

  @override
  Future<int> saveIncome(int monthlyIncome) async {
    await _dataStore.mutate(
      (snapshot) => snapshot.copyWith(
        spending: snapshot.spending.copyWith(monthlyIncome: monthlyIncome),
      ),
    );
    return monthlyIncome;
  }

  @override
  Future<bool> allocateBudget() async {
    await Future.delayed(const Duration(milliseconds: 200));
    return true;
  }

  @override
  Future<BudgetWeekSetupDto> getWeekSetup(String yearWeek) async {
    await Future.delayed(const Duration(milliseconds: 150));
    final snapshot = await _dataStore.ensureSessionSnapshot();
    final spending = snapshot.spending;

    final monthlyIncome = spending.monthlyIncome;
    final monthlySavings = spending.monthlySavingsGoal;

    final weeklySavings = (monthlySavings / 4).round();

    final categories = spending.categories;
    final totalCategoryAmount = categories.fold(0, (sum, c) => sum + c.amount);

    final fixedEssentials = categories
        .where((c) => ['교통', '구독', '통신비', '보험'].contains(c.name))
        .map((c) => BudgetEssentialDto(
              categoryName: c.name,
              expenseScope: 'FIXED',
              plannedAmount: c.amount,
              actualSpentAmount: (c.amount * 0.6).round(),
            ))
        .toList();

    final variableEssentials = categories
        .where((c) => !['교통', '구독', '통신비', '보험'].contains(c.name))
        .map((c) => BudgetEssentialDto(
              categoryName: c.name,
              expenseScope: 'VARIABLE',
              plannedAmount: c.amount,
              actualSpentAmount: (c.amount * 0.4).round(),
            ))
        .toList();

    final fixedTotal = fixedEssentials.fold(0, (sum, e) => sum + e.plannedAmount);
    final variableTotal = variableEssentials.fold(0, (sum, e) => sum + e.plannedAmount);
    final essentialTotal = fixedTotal + variableTotal;
    final plannedFree = (spending.weeklyBudget - weeklySavings - essentialTotal).clamp(0, spending.weeklyBudget).toInt();
    final actualSpentTotal = fixedEssentials.fold(0, (sum, e) => sum + e.actualSpentAmount) +
        variableEssentials.fold(0, (sum, e) => sum + e.actualSpentAmount);
    final remainingFree = (plannedFree - (totalCategoryAmount - actualSpentTotal)).clamp(0, plannedFree).toInt();

    return BudgetWeekSetupDto(
      plannedIncomeAmount: (monthlyIncome / 4).round(),
      plannedSavingAmount: weeklySavings,
      plannedEssentialAmount: essentialTotal,
      plannedFreeAmount: plannedFree,
      remainingFreeAmount: remainingFree,
      essentials: [...fixedEssentials, ...variableEssentials],
    );
  }

  @override
  Future<int> getAverageIncomeFromAnalysis() async {
    await Future.delayed(const Duration(milliseconds: 150));
    final snapshot = await _dataStore.ensureSessionSnapshot();
    return snapshot.spending.monthlyIncome;
  }
}