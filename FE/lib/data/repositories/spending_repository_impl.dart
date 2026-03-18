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
  Future<List<SpendingCategory>> fetchMonthlyCategories(DateTime monthStart) async {
    final yearMonth = _toYearMonth(monthStart);
    final dto = await _remoteDataSource.fetchMonthlyAnalysis(yearMonth);
    return dto.categoryExpenses
        .map((e) => SpendingCategory(
              name: e.categoryName,
              amount: e.amount,
              budget: 0,
              colorValue: 0xFFFF7043,
              iconKey: 'more_horiz',
            ))
        .toList();
  }

  @override
  Future<List<DailySpending>> fetchMonthlyDailySpending(DateTime monthStart) async {
    return _remoteDataSource.fetchMonthlyDailySpending(monthStart);
  }

  @override
  Future<List<SpendingCategory>> fetchWeeklyCategories(DateTime weekStart) async {
    final yearWeek = _toIsoYearWeek(weekStart);
    final dto = await _remoteDataSource.fetchWeeklyAnalysis(yearWeek);
    return dto.categoryExpenses
        .map((e) => SpendingCategory(
              name: e.categoryName,
              amount: e.amount,
              budget: 0,
              colorValue: 0xFFFF7043,
              iconKey: 'more_horiz',
            ))
        .toList();
  }

  String _toIsoYearWeek(DateTime weekMonday) {
    final thursday = weekMonday.add(const Duration(days: 3));
    final year = thursday.year;
    final jan4 = DateTime(year, 1, 4);
    final week1Monday = jan4.subtract(Duration(days: jan4.weekday - 1));
    final weekNum = weekMonday.difference(week1Monday).inDays ~/ 7 + 1;
    return '$year-W${weekNum.toString().padLeft(2, '0')}';
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
