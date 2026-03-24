import 'package:more_app/data/datasources/remote/budget_remote_datasource.dart';
import 'package:more_app/domain/entities/budget.dart';
import 'package:more_app/domain/repositories/budget_repository.dart';

class BudgetRepositoryImpl implements BudgetRepository {
  BudgetRepositoryImpl({required BudgetRemoteDataSource remoteDataSource})
      : _remoteDataSource = remoteDataSource;

  final BudgetRemoteDataSource _remoteDataSource;

  String _toIsoYearWeek(DateTime weekMonday) {
    final thursday = weekMonday.add(const Duration(days: 3));
    final year = thursday.year;
    final jan4 = DateTime(year, 1, 4);
    final week1Monday = jan4.subtract(Duration(days: jan4.weekday - 1));
    final weekNum = weekMonday.difference(week1Monday).inDays ~/ 7 + 1;
    return '$year-W${weekNum.toString().padLeft(2, '0')}';
  }

  DateTime _thisWeekMonday() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    return today.subtract(Duration(days: today.weekday - 1));
  }

  @override
  Future<BudgetSetup> fetchBudgetSetup() async {
    // 수입과 setup을 독립적으로 조회 - 하나 실패해도 나머지 표시
    int income = 0;
    BudgetWeekSetupDto setup = const BudgetWeekSetupDto();

    try {
      income = await _remoteDataSource.getIncome();
    } catch (_) {}

    // 저장된 월급이 없으면 거래 분석의 평균 수입으로 pre-fill
    if (income == 0) {
      try {
        income = await _remoteDataSource.getAverageIncomeFromAnalysis();
      } catch (_) {}
    }

    try {
      final yearWeek = _toIsoYearWeek(_thisWeekMonday());
      setup = await _remoteDataSource.getWeekSetup(yearWeek);
    } catch (_) {}

    final fixedEssentials = setup.essentials
        .where((e) => e.expenseScope == 'FIXED')
        .map((e) => BudgetEssential(
              categoryName: e.categoryName,
              plannedAmount: e.plannedAmount,
              actualSpentAmount: e.actualSpentAmount,
            ))
        .toList();
    final variableEssentials = setup.essentials
        .where((e) => e.expenseScope == 'VARIABLE')
        .map((e) => BudgetEssential(
              categoryName: e.categoryName,
              plannedAmount: e.plannedAmount,
              actualSpentAmount: e.actualSpentAmount,
            ))
        .toList();

    return BudgetSetup(
      monthlyIncome: income,
      savingsAmount: setup.plannedSavingAmount,
      fixedExpenseAmount: fixedEssentials.fold(0, (sum, e) => sum + e.plannedAmount),
      variableExpenseAmount: variableEssentials.fold(0, (sum, e) => sum + e.plannedAmount),
      freeAmount: setup.plannedFreeAmount,
      remainingFreeAmount: setup.remainingFreeAmount,
      fixedEssentials: fixedEssentials,
      variableEssentials: variableEssentials,
    );
  }

  @override
  Future<BudgetSetup> saveIncomeAndAllocate(int monthlyIncome) async {
    await _remoteDataSource.saveIncome(monthlyIncome);
    await _remoteDataSource.allocateBudget();
    return fetchBudgetSetup();
  }
}