import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/spending_remote_datasource.dart';
import 'package:more_app/data/models/app_snapshot_dto.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/domain/entities/spending_overview.dart';
import 'package:more_app/domain/repositories/spending_repository.dart';

class SpendingRepositoryImpl implements SpendingRepository {
  SpendingRepositoryImpl({
    required SpendingRemoteDataSource remoteDataSource,
    required AppLocalDataSource localDataSource,
  }) : _remoteDataSource = remoteDataSource,
       _localDataSource = localDataSource;

  final SpendingRemoteDataSource _remoteDataSource;
  final AppLocalDataSource _localDataSource;

  @override
  Future<void> syncTransactions({int months = 1, bool relabel = false}) async {
    await _remoteDataSource.syncTransactions(months: months, relabel: relabel);
  }

  @override
  Future<void> analyzeTransactions({int months = 1, bool relabel = false}) async {
    await _remoteDataSource.analyzeTransactions(months: months, relabel: relabel);
  }

  @override
  Future<List<DailySpending>> fetchWeeklyDailySpending(DateTime weekStart) async {
    return _remoteDataSource.fetchWeeklyDailySpending(weekStart);
  }

  String _toYearMonth(DateTime monthStart) {
    return '${monthStart.year}-${monthStart.month.toString().padLeft(2, '0')}';
  }

  @override
  Future<MonthlyAnalysisResult> fetchMonthlyAnalysis(DateTime monthStart) async {
    final yearMonth = _toYearMonth(monthStart);
    try {
      final dto = await _remoteDataSource.fetchMonthlyAnalysis(yearMonth);
      final categories = dto.categoryExpenses
          .map((e) => SpendingCategory(
                name: e.categoryName,
                amount: e.amount,
                budget: 0,
                colorValue: 0xFFFF7043,
                iconKey: 'more_horiz',
                ratio: e.ratio,
              ))
          .toList();
      return MonthlyAnalysisResult(
        categories: categories,
        expenseAmount: dto.expenseAmount,
      );
    } catch (e) {
      return const MonthlyAnalysisResult();
    }
  }

  @override
  Future<List<DailySpending>> fetchMonthlyDailySpending(DateTime monthStart) async {
    return _remoteDataSource.fetchMonthlyDailySpending(monthStart);
  }

  @override
  Future<WeeklyAnalysisResult> fetchWeeklyAnalysis(DateTime weekStart) async {
    final yearWeek = _toMonthWeek(weekStart);
    try {
      final dto = await _remoteDataSource.fetchWeeklyAnalysis(yearWeek);
      if (dto.expenseAmount > 0 && dto.categoryExpenses.isNotEmpty) {
        final categories = dto.categoryExpenses
            .map((e) => SpendingCategory(
                  name: e.categoryName,
                  amount: e.amount,
                  budget: 0,
                  colorValue: 0xFFFF7043,
                  iconKey: 'more_horiz',
                  ratio: e.ratio,
                ))
            .toList();
        return WeeklyAnalysisResult(
          categories: categories,
          expenseAmount: dto.expenseAmount,
        );
      }
      // 분석 결과 없거나 actualSpentAmount가 모두 0이면 거래 내역에서 직접 카테고리 집계
      final summary = await _remoteDataSource.fetchWeeklyTransactionSummary(weekStart);
      final categories = summary.categoryAmounts.entries
          .map((e) => SpendingCategory(
                name: e.key,
                amount: e.value,
                budget: 0,
                colorValue: 0xFFFF7043,
                iconKey: 'more_horiz',
              ))
          .toList()
        ..sort((a, b) => b.amount.compareTo(a.amount));
      final expenseAmount = summary.categoryAmounts.values.fold(0, (sum, v) => sum + v);
      return WeeklyAnalysisResult(categories: categories, expenseAmount: expenseAmount);
    } catch (e) {
      return const WeeklyAnalysisResult();
    }
  }

  String _toMonthWeek(DateTime weekMonday) {
    final year = weekMonday.year;
    final month = weekMonday.month;
    final firstDayOfMonth = DateTime(year, month, 1);
    final daysToFirstMonday = (8 - firstDayOfMonth.weekday) % 7;
    final firstMonday = firstDayOfMonth.add(Duration(days: daysToFirstMonday));
    final weekNum = weekMonday.difference(firstMonday).inDays ~/ 7 + 1;
    return '$year-${month.toString().padLeft(2, '0')}-$weekNum';
  }

  @override
  Future<SpendingOverview> fetchSpendingOverview() async {
    try {
      final snapshot = await _remoteDataSource.fetchSpendingSnapshotDto();
      return _toOverview(snapshot);
    } catch (_) {
      return const SpendingOverview();
    }
  }

  @override
  Future<SpendingOverview> updateBudgetPlan({
    required int monthlyIncome,
    required int monthlySavingsGoal,
    required int monthlyFixedExpense,
    required List<SpendingCategory> categories,
  }) async {
    final current = await _fetchCurrentSpending();
    final available = monthlyIncome - monthlySavingsGoal - monthlyFixedExpense;
    final weeklyBudget = available <= 0 ? 0 : (available / 4.345).round();

    final updated = SpendingSnapshotDto(
      categories: categories,
      dailySpendings: current.dailySpendings,
      monthlySpendings: current.monthlySpendings,
      weeklyBudget: weeklyBudget,
      monthlyIncome: monthlyIncome,
      monthlySavingsGoal: monthlySavingsGoal,
      monthlyFixedExpense: monthlyFixedExpense,
    );

    try {
      final saved = await _remoteDataSource.updateSpendingSnapshotDto(updated);
      return _toOverview(saved);
    } catch (_) {
      final saved = await _localDataSource.updateSpendingSnapshotDto(updated);
      return _toOverview(saved);
    }
  }

  @override
  Future<SpendingPatternResult> fetchPreviousMonthPattern(DateTime currentMonth) async {
    try {
      final prevMonth = DateTime(currentMonth.year, currentMonth.month - 1, 1);
      final yearMonth = _toYearMonth(prevMonth);

      final dto = await _remoteDataSource.fetchMonthlyAnalysis(yearMonth);
      final summary = await _remoteDataSource.fetchMonthlyTransactionSummary(prevMonth);

      final totalIncome = summary.incomeTotal;
      final totalExpense = dto.expenseAmount;
      final fixedAmount = dto.fixedEssentialAmount;
      final variableAmount = dto.variableEssentialAmount;
      final freeAmount = (totalExpense - fixedAmount - variableAmount).clamp(0, totalExpense);
      final savingsAmount = totalIncome - totalExpense;

      final categories = dto.categoryExpenses
          .map((e) => SpendingCategory(
                name: e.categoryName,
                amount: e.amount,
                budget: 0,
                colorValue: 0xFFFF7043,
                iconKey: 'more_horiz',
              ))
          .toList()
        ..sort((a, b) => b.amount.compareTo(a.amount));

      final topCategoryName = categories.isNotEmpty ? categories.first.name : '';

      return SpendingPatternResult(
        totalIncome: totalIncome,
        totalExpense: totalExpense,
        fixedAmount: fixedAmount,
        variableAmount: variableAmount,
        freeAmount: freeAmount,
        savingsAmount: savingsAmount,
        categories: categories,
        topCategoryName: topCategoryName,
      );
    } catch (e) {
      return const SpendingPatternResult();
    }
  }

  Future<SpendingSnapshotDto> _fetchCurrentSpending() async {
    try {
      return await _remoteDataSource.fetchSpendingSnapshotDto();
    } catch (_) {
      return _localDataSource.getSpendingSnapshotDto();
    }
  }

  SpendingOverview _toOverview(SpendingSnapshotDto snapshot) {
    return SpendingOverview(
      categories: snapshot.categories,
      dailySpendings: snapshot.dailySpendings,
      monthlySpendings: snapshot.monthlySpendings,
      weeklyBudget: snapshot.weeklyBudget,
      monthlyIncome: snapshot.monthlyIncome,
      monthlySavingsGoal: snapshot.monthlySavingsGoal,
      monthlyFixedExpense: snapshot.monthlyFixedExpense,
    );
  }
}
