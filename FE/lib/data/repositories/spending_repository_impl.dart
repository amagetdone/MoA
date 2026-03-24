import 'package:flutter/foundation.dart';
import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/spending_remote_datasource.dart'
    show SpendingRemoteDataSource, MonthlyTransactionSummary;
import 'package:more_app/data/models/app_snapshot_dto.dart';
import 'package:more_app/data/models/weekly_analysis_dto.dart';
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
  Future<void> syncTransactions({int months = 3, bool relabel = false}) async {
    await _remoteDataSource.syncTransactions(months: months, relabel: relabel);
  }

  @override
  Future<void> analyzeTransactions({int months = 3, bool relabel = false}) async {
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
              ))
          .toList();
      return MonthlyAnalysisResult(
        categories: categories,
        expenseAmount: dto.expenseAmount,
      );
    } catch (e, st) {
      debugPrint('[Spending] fetchMonthlyAnalysis ERROR: $e\n$st');
      return const MonthlyAnalysisResult();
    }
  }

  @override
  Future<List<DailySpending>> fetchMonthlyDailySpending(DateTime monthStart) async {
    return _remoteDataSource.fetchMonthlyDailySpending(monthStart);
  }

  @override
  Future<WeeklyAnalysisResult> fetchWeeklyAnalysis(DateTime weekStart) async {
    final yearWeek = _toIsoYearWeek(weekStart);
    try {
      final dto = await _remoteDataSource.fetchWeeklyAnalysis(yearWeek);
      final categories = dto.categoryExpenses
          .map((e) => SpendingCategory(
                name: e.categoryName,
                amount: e.amount,
                budget: 0,
                colorValue: 0xFFFF7043,
                iconKey: 'more_horiz',
              ))
          .toList();
      return WeeklyAnalysisResult(
        categories: categories,
        expenseAmount: dto.expenseAmount,
      );
    } catch (e, st) {
      debugPrint('[Spending] fetchWeeklyAnalysis ERROR: $e\n$st');
      return const WeeklyAnalysisResult();
    }
  }

  String _toIsoYearWeek(DateTime weekMonday) {
    final month = weekMonday.month.toString().padLeft(2, '0');
    final weekOfMonth = ((weekMonday.day - 1) ~/ 7) + 1;
    return '${weekMonday.year}-$month-$weekOfMonth';
  }

  @override
  Future<SpendingOverview> fetchSpendingOverview() async {
    try {
      final snapshot = await _remoteDataSource.fetchSpendingSnapshotDto();
      return _toOverview(snapshot);
    } catch (_) {
      final snapshot = await _localDataSource.getSpendingSnapshotDto();
      return _toOverview(snapshot);
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
  Future<SpendingPatternResult> fetchThreeMonthPattern(DateTime currentMonth) async {
    try {
      final months = [
        DateTime(currentMonth.year, currentMonth.month - 3, 1),
        DateTime(currentMonth.year, currentMonth.month - 2, 1),
        DateTime(currentMonth.year, currentMonth.month - 1, 1),
      ];

      // 실거래 기반: 수입 합계 + 카테고리별 지출
      final summaries = await Future.wait(months.map((m) async {
        try {
          return await _remoteDataSource.fetchMonthlyTransactionSummary(m);
        } catch (_) {
          return const MonthlyTransactionSummary();
        }
      }));

      // 필수지출 분류 (고정비/변동비)
      final analysisDtos = await Future.wait(months.map((m) async {
        try {
          return await _remoteDataSource.fetchMonthlyAnalysis(_toYearMonth(m));
        } catch (_) {
          return MonthlyAnalysisDto(yearMonth: _toYearMonth(m));
        }
      }));

      final totalIncome = summaries.fold(0, (sum, s) => sum + s.incomeTotal);
      final totalExpense = analysisDtos.fold(0, (sum, d) => sum + d.expenseAmount);
      final fixedAmount = analysisDtos.fold(0, (sum, d) => sum + d.fixedEssentialAmount);
      final variableAmount = analysisDtos.fold(0, (sum, d) => sum + d.variableEssentialAmount);
      for (final dto in analysisDtos) {
        debugPrint('[Pattern] ${dto.yearMonth} expense=${dto.expenseAmount} fixed=${dto.fixedEssentialAmount} variable=${dto.variableEssentialAmount} categories=${dto.categoryExpenses.length}');
      }
      final freeAmount = (totalExpense - fixedAmount - variableAmount).clamp(0, totalExpense);
      final savingsAmount = (totalIncome - totalExpense).clamp(0, totalIncome);

      // 카테고리: 분석 스냅샷 기반 (구독비, 식비 등 properly labeled)
      final categoryMap = <String, int>{};
      for (final dto in analysisDtos) {
        for (final cat in dto.categoryExpenses) {
          categoryMap[cat.categoryName] = (categoryMap[cat.categoryName] ?? 0) + cat.amount;
        }
      }

      final categories = categoryMap.entries
          .map((e) => SpendingCategory(
                name: e.key,
                amount: e.value,
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
    } catch (e, st) {
      debugPrint('[Spending] fetchThreeMonthPattern ERROR: $e\n$st');
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
