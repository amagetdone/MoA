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
